package com.atguigu.gmall.pms.api;

import com.atguigu.gmall.common.bean.PageParamVo;
import com.atguigu.gmall.common.bean.ResponseVo;
import com.atguigu.gmall.pms.entity.*;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import java.util.List;

public interface GmallPmsApi {

    @GetMapping("pms/spu/{id}")
    ResponseVo<SpuEntity> querySpuById(@PathVariable("id") Long id);

    @PostMapping("pms/spu/json")
    ResponseVo<List<SpuEntity>> querySpuByPageJson(@RequestBody PageParamVo paramVo);

    @GetMapping("pms/sku/spu/{spuId}")
    ResponseVo<List<SkuEntity>> querySkuBySpuId(@PathVariable("spuId") Long sid);

    @GetMapping("pms/brand/{id}")
    ResponseVo<BrandEntity> queryBrandById(@PathVariable("id") Long id);

    @GetMapping("pms/category/{id}")
    ResponseVo<CategoryEntity> queryCategoryById(@PathVariable("id") Long id);

    @GetMapping("pms/category/parent/{parentId}")
    ResponseVo<List<CategoryEntity>> queryCategoryByPid(@PathVariable("parentId") Long pid);

    @GetMapping("pms/category/lv23/{pid}")
    ResponseVo<List<CategoryEntity>> queryLV23CategoriesByPid(@PathVariable("pid")Long pid);

    @GetMapping("pms/skuattrvalue/search/{cid}/{skuId}")
    ResponseVo<List<SkuAttrValueEntity>> querySearchSkuAttrValueByCidAndSkuId(
            @PathVariable("cid")Long cid,
            @PathVariable("skuId")Long skuId);

    @GetMapping("pms/spuattrvalue/search/{cid}/{spuId}")
    ResponseVo<List<SpuAttrValueEntity>> querySearchSpuAttrValueByCidAndSpuId(
            @PathVariable("cid")Long cid,
            @PathVariable("spuId")Long spuId
    );

}
