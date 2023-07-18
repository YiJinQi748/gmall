package com.example.gmall.search.listener;

import com.atguigu.gmall.common.bean.ResponseVo;
import com.atguigu.gmall.pms.entity.*;
import com.atguigu.gmall.wms.entity.WareSkuEntity;
import com.example.gmall.search.feign.GmallPmsClient;
import com.example.gmall.search.feign.GmallWmsClient;
import com.example.gmall.search.pojo.Goods;
import com.example.gmall.search.pojo.SearchAttrValue;
import com.rabbitmq.client.Channel;
import org.springframework.amqp.core.ExchangeTypes;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.Exchange;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.QueueBinding;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.elasticsearch.core.ElasticsearchRestTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class GoodsListener {

    @Autowired
    private GmallPmsClient gmallPmsClient;

    @Autowired
    private GmallWmsClient gmallWmsClient;

    @Autowired
    private ElasticsearchRestTemplate restTemplate;

    @RabbitListener(bindings = @QueueBinding(
            value = @Queue("SEARCH.INSERT.QUEUE"),
            exchange = @Exchange(value = "PMS.SPU.EXCHANGE", type = ExchangeTypes.TOPIC, ignoreDeclarationExceptions = "true"),
            key = {"item.insert"}
    ))
    public void syncData(Long spuId, Channel channel, Message message) throws IOException {
        if (spuId == null) {
            channel.basicAck(message.getMessageProperties().getDeliveryTag(), false);
            return;
        }
        //根据spuID获取spu
        ResponseVo<SpuEntity> spuEntityResponseVo = this.gmallPmsClient.querySpuById(spuId);
        SpuEntity spu = spuEntityResponseVo.getData();
        if (spu==null){
            channel.basicAck(message.getMessageProperties().getDeliveryTag(), false);
            return;
        }
        // 根据spuId查询spu下skus
        ResponseVo<List<SkuEntity>> skuResponseVo = this.gmallPmsClient.querySkuBySpuId(spu.getId());
        List<SkuEntity> skus = skuResponseVo.getData();
        //如果当前spu下的sku为空,则遍历下个spu
        if (CollectionUtils.isEmpty(skus)) {
            return;//增强版foreach return类似于continue
        }
        // 同一个spu的sku品牌肯定是一样的：根据品牌id查询品牌
        ResponseVo<BrandEntity> brandEntityResponseVo = this.gmallPmsClient.queryBrandById(spu.getBrandId());
        BrandEntity brandEntity = brandEntityResponseVo.getData();
        // 同一个spu的sku分类肯定是一样的：根据分类id查询分类
        ResponseVo<CategoryEntity> categoryEntityResponseVo = this.gmallPmsClient.queryCategoryById(spu.getCategoryId());
        CategoryEntity categoryEntity = categoryEntityResponseVo.getData();
        //同一个spu基本类型的检索属性和值,肯定是一样的,根据分类ID和spuID检索
        ResponseVo<List<SpuAttrValueEntity>> baseAttrValueResponseVo = this.gmallPmsClient.querySearchSpuAttrValueByCidAndSpuId(spu.getCategoryId(), spu.getId());
        List<SpuAttrValueEntity> baseAttrEntities = baseAttrValueResponseVo.getData();
        this.restTemplate.save(skus.stream().map(sku -> {
            Goods goods = new Goods();
            // 设置sku的基本信息
            goods.setSkuId(sku.getId());
            goods.setTitle(sku.getTitle());
            goods.setSubtitle(sku.getSubtitle());
            goods.setDefaultImage(sku.getDefaultImage());
            goods.setPrice(sku.getPrice().doubleValue());
            // 设置spu的创建时间
            goods.setCreateTime(spu.getCreateTime());
            // 根据skuId查询库存，获取到销量及库存信息
            ResponseVo<List<WareSkuEntity>> wareResponseVo = this.gmallWmsClient.queryStockBySkuId(sku.getId());
            List<WareSkuEntity> wareSkuEntities = wareResponseVo.getData();
            if (!CollectionUtils.isEmpty(wareSkuEntities)) {
                //销量
                goods.setSales(wareSkuEntities.stream().map(WareSkuEntity::getSales).reduce((a, b) -> a + b).get());
                //是否有货
                goods.setStore(wareSkuEntities.stream().anyMatch(wareSkuEntity -> wareSkuEntity.getStock() - wareSkuEntity.getStockLocked() > 0));
            }
            // 设置品牌参数
            if (brandEntity != null) {
                goods.setBrandId(brandEntity.getId());
                goods.setBrandName(brandEntity.getName());
                goods.setLogo(brandEntity.getLogo());
            }
            // 设置分类参数
            if (categoryEntity != null) {
                goods.setCategoryId(categoryEntity.getId());
                goods.setCategoryName(categoryEntity.getName());
            }
            // 设置检索类型的规格参数和值
            List<SearchAttrValue> searchAttrValues = new ArrayList<>();
            // pms_sku_attr_value：当前sku的检索类型的销售属性和值
            ResponseVo<List<SkuAttrValueEntity>> saleAttrResponseVo = this.gmallPmsClient.querySearchSkuAttrValueByCidAndSkuId(sku.getCategoryId(), sku.getId());
            List<SkuAttrValueEntity> skuAttrValueEntities = saleAttrResponseVo.getData();
            if (!CollectionUtils.isEmpty(skuAttrValueEntities)) {
                searchAttrValues.addAll(skuAttrValueEntities.stream().map(skuAttrValueEntity -> {
                    SearchAttrValue searchAttrValue = new SearchAttrValue();
                    BeanUtils.copyProperties(skuAttrValueEntity, searchAttrValue);
                    return searchAttrValue;
                }).collect(Collectors.toList()));
            }
            // pms_spu_attr_value：当前spu的检索类型的基本属性和值
            if (!CollectionUtils.isEmpty(baseAttrEntities)) {
                searchAttrValues.addAll(baseAttrEntities.stream().map(baseAttrEntity -> {
                    SearchAttrValue searchAttrValue = new SearchAttrValue();
                    BeanUtils.copyProperties(baseAttrEntity, searchAttrValue);
                    return searchAttrValue;
                }).collect(Collectors.toList()));
            }
            goods.setSearchAttrs(searchAttrValues);
            return goods;
        }).collect(Collectors.toList()));

        //确认消息
        channel.basicAck(message.getMessageProperties().getDeliveryTag(),false);

    }
}
