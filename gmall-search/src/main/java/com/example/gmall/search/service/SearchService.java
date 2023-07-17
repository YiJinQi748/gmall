package com.example.gmall.search.service;

import com.alibaba.fastjson.JSON;
import com.atguigu.gmall.pms.entity.BrandEntity;
import com.atguigu.gmall.pms.entity.CategoryEntity;
import com.example.gmall.search.pojo.Goods;
import com.example.gmall.search.pojo.SearchParamVo;
import com.example.gmall.search.pojo.SearchResponseAttrVo;
import com.example.gmall.search.pojo.SearchResponseVo;
import org.apache.commons.lang3.StringUtils;
import org.apache.lucene.search.join.ScoreMode;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.common.text.Text;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.Operator;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.index.query.RangeQueryBuilder;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHits;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.Aggregations;
import org.elasticsearch.search.aggregations.bucket.MultiBucketsAggregation;
import org.elasticsearch.search.aggregations.bucket.nested.ParsedNested;
import org.elasticsearch.search.aggregations.bucket.terms.ParsedLongTerms;
import org.elasticsearch.search.aggregations.bucket.terms.ParsedStringTerms;
import org.elasticsearch.search.aggregations.bucket.terms.Terms;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightBuilder;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightField;
import org.elasticsearch.search.sort.SortOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class SearchService {

    @Autowired
    private RestHighLevelClient restHighLevelClient;

    public SearchResponseVo search(SearchParamVo searchParamVo) {
        try {
            SearchRequest searchRequest = new SearchRequest(new String[]{"goods"}, this.buildDsl(searchParamVo));
            SearchResponse response = this.restHighLevelClient.search(searchRequest, RequestOptions.DEFAULT);
            //解析结果集
            SearchResponseVo responseVo = this.parseResult(response);
            // 分页参数只能通过搜索条件获取
            responseVo.setPageNum(searchParamVo.getPageNum());
            responseVo.setPageSize(searchParamVo.getPageSize());
            return responseVo;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

    }

    /**
     * 从response中解析出数据设置给ResponseVo对象即可
     * @param response
     * @return
     */
    private SearchResponseVo parseResult(SearchResponse response){
        SearchResponseVo responseVo = new SearchResponseVo();
        // 解析搜索结果集获取分页相关信息
        SearchHits hits = response.getHits();
        responseVo.setTotal(hits.getTotalHits().value);
        // 获取搜索结果集中的当前页的数据
        SearchHit[] hitsHits = hits.getHits();
        // 把hitsHits数组转化成Goods集合
        responseVo.setGoodsList(Arrays.stream(hitsHits).map(hitsHit -> {
            // 获取HitsHit中的_source json字符串
            String json = hitsHit.getSourceAsString();
            // 把json字符串反序列化为goods对象
            Goods goods = JSON.parseObject(json, Goods.class);
            // 获取高亮结果集
            Map<String, HighlightField> highlightFields = hitsHit.getHighlightFields();
            if (!CollectionUtils.isEmpty(highlightFields)){
                HighlightField highlightField = highlightFields.get("title");
                if (highlightField != null) {
                    Text[] fragments = highlightField.fragments();
                    if (fragments != null && fragments.length > 0) {
                        Text fragment = fragments[0];
                        if (fragment != null) {
                            goods.setTitle(fragment.string());
                        }
                    }
                }
            }
            return goods;
        }).collect(Collectors.toList()));
        // 解析聚合结果集获取过滤条件列表
        Aggregations aggregations = response.getAggregations();
        // 获取品牌的聚合结果集
        ParsedLongTerms brandIdAgg = aggregations.get("brandIdAgg");
        List<? extends Terms.Bucket> brandIdAggBuckets = brandIdAgg.getBuckets();
        if (!CollectionUtils.isEmpty(brandIdAggBuckets)){
            // 把品牌id桶集合转化成品牌entity集合
            responseVo.setBrands(brandIdAggBuckets.stream().map(bucket -> {
                BrandEntity brandEntity = new BrandEntity();
                // 当前桶中的key就是品牌id
                brandEntity.setId(bucket.getKeyAsNumber().longValue());
                // 获取子聚合
                Aggregations subAggs = bucket.getAggregations();
                // 获取品牌名称的子聚合
                ParsedStringTerms brandNameAgg = subAggs.get("brandNameAgg");
                List<? extends Terms.Bucket> brandNameAggBuckets = brandNameAgg.getBuckets();
                // 获取品牌名称子聚合桶中的第一个元素的key，就是品牌名称
                if (!CollectionUtils.isEmpty(brandNameAggBuckets)){
                    brandEntity.setName(brandNameAggBuckets.get(0).getKeyAsString());
                }
                // 获取品牌logo的子聚合
                ParsedStringTerms logoAgg = subAggs.get("logoAgg");
                List<? extends Terms.Bucket> logoAggBuckets = logoAgg.getBuckets();
                if (!CollectionUtils.isEmpty(logoAggBuckets)){
                    brandEntity.setLogo(logoAggBuckets.get(0).getKeyAsString());
                }
                return brandEntity;
            }).collect(Collectors.toList()));
        }
        // 获取分类的聚合结果集
        ParsedLongTerms categoryIdAgg = aggregations.get("categoryIdAgg");
        List<? extends Terms.Bucket> categoryIdAggBuckets = categoryIdAgg.getBuckets();
        if (!CollectionUtils.isEmpty(categoryIdAggBuckets)){
            responseVo.setCategories(categoryIdAggBuckets.stream().map(bucket -> {
                CategoryEntity categoryEntity = new CategoryEntity();
                categoryEntity.setId(bucket.getKeyAsNumber().longValue());
                // 获取分类名称的子聚合
                ParsedStringTerms categoryNameAgg = bucket.getAggregations().get("categoryNameAgg");
                List<? extends Terms.Bucket> categoryNameAggBuckets = categoryNameAgg.getBuckets();
                if (!CollectionUtils.isEmpty(categoryNameAggBuckets)){
                    categoryEntity.setName(categoryNameAggBuckets.get(0).getKeyAsString());
                }
                return categoryEntity;
            }).collect(Collectors.toList()));
        }
        // 获取规格参数的嵌套聚合结果集
        ParsedNested attrAgg = aggregations.get("attrAgg");
        // 获取规格参数id子聚合
        ParsedLongTerms attrIdAgg = attrAgg.getAggregations().get("attrIdAgg");
        List<? extends Terms.Bucket> buckets = attrIdAgg.getBuckets();
        if (!CollectionUtils.isEmpty(buckets)){
            List<SearchResponseAttrVo> filters = buckets.stream().map(bucket -> {
                SearchResponseAttrVo attrVo = new SearchResponseAttrVo();
                attrVo.setAttrId(bucket.getKeyAsNumber().longValue());
                // 获取所有子聚合
                Aggregations subAggs = bucket.getAggregations();
                // 获取规格参数名称的子聚合
                ParsedStringTerms attrNameAgg = subAggs.get("attrNameAgg");
                List<? extends Terms.Bucket> attrNameAggBuckets = attrNameAgg.getBuckets();
                if (!CollectionUtils.isEmpty(attrNameAggBuckets)){
                    attrVo.setAttrName(attrNameAggBuckets.get(0).getKeyAsString());
                }
                // 获取规格参数值的子聚合
                ParsedStringTerms attrValueAgg = subAggs.get("attrValueAgg");
                List<? extends Terms.Bucket> attrValueAggBuckets = attrValueAgg.getBuckets();
                if (!CollectionUtils.isEmpty(attrValueAggBuckets)){
                    attrVo.setAttrValues(attrValueAggBuckets.stream().map(MultiBucketsAggregation.Bucket::getKeyAsString).collect(Collectors.toList()));
                }
                return attrVo;
            }).collect(Collectors.toList());
            responseVo.setFilters(filters);
        }

        return responseVo;
    }


    /**
     * 构建DSL语句
     * @param paramVo
     * @return
     */
    private SearchSourceBuilder buildDsl(SearchParamVo paramVo){
        SearchSourceBuilder sourceBuilder = new SearchSourceBuilder();
        // 获取搜索关键字
        String keyword = paramVo.getKeyword();
        if (StringUtils.isBlank(keyword)){
            // TODO：打广告
            throw new RuntimeException("请输入查询条件！");
        }
        // 1.构建查询过滤条件
        BoolQueryBuilder boolQueryBuilder = QueryBuilders.boolQuery();
        sourceBuilder.query(boolQueryBuilder);
        // 1.1. 构建匹配查询
        boolQueryBuilder.must(QueryBuilders.matchQuery("title", keyword).operator(Operator.AND));
        // 1.2. 构建过滤条件
        // 1.2.1. 构建品牌的词条过滤
        List<Long> brandId = paramVo.getBrandId();
        if (!CollectionUtils.isEmpty(brandId)){
            boolQueryBuilder.filter(QueryBuilders.termsQuery("brandId", brandId));
        }
        // 1.2.2. 构建分类的词条过滤
        List<Long> categoryId = paramVo.getCategoryId();
        if (!CollectionUtils.isEmpty(categoryId)){
            boolQueryBuilder.filter(QueryBuilders.termsQuery("categoryId", categoryId));
        }
        // 1.2.3. 构建价格区间的过滤
        Double priceFrom = paramVo.getPriceFrom();
        Double priceTo = paramVo.getPriceTo();
        // 任何一个价格不为null，都要有范围过滤
        if (priceFrom != null || priceTo != null) {
            RangeQueryBuilder rangeQuery = QueryBuilders.rangeQuery("price");
            // 如果起始价格不为null，则要添加gte
            if (priceFrom != null) {
                rangeQuery.gte(priceFrom);
            }
            // 如果截止价格不为null，则要添加lte
            if (priceTo != null) {
                rangeQuery.lte(priceTo);
            }
            boolQueryBuilder.filter(rangeQuery);
        }
        // 1.2.4. 仅显示有货的词条过滤
        Boolean store = paramVo.getStore();
        if (store != null) {
            boolQueryBuilder.filter(QueryBuilders.termQuery("store", store));
        }
        // 1.2.5. 构建规格参数的嵌套过滤
        List<String> props = paramVo.getProps(); // ["4:8G-12G", "5:128G-256G"]
        if (!CollectionUtils.isEmpty(props)) {
            // 遍历props，每一个prop就要对应一个嵌套过滤
            props.forEach(prop -> { // 4:8G-12G
                // 解析字符串，获取规格参数id和values(8G-12G)
                String[] attr = StringUtils.split(prop, ":");
                // 先使用：进行分割，如果分割后的结果不为null，并且长度为2，并且第一位为数字，则认为是合法的规格参数
                if (attr != null && attr.length == 2 && StringUtils.isNumeric(attr[0])){
                    // 嵌套过滤中需要一个小bool查询
                    BoolQueryBuilder boolQuery = QueryBuilders.boolQuery();
                    // 规格参数id的过滤
                    boolQuery.must(QueryBuilders.termQuery("searchAttrs.attrId", attr[0]));
                    // 规格参数值的过滤
                    boolQuery.must(QueryBuilders.termsQuery("searchAttrs.attrValue", StringUtils.split(attr[1], "-")));
                    // 给外层的bool查询添加嵌套过滤。
                    boolQueryBuilder.filter(QueryBuilders.nestedQuery("searchAttrs", boolQuery, ScoreMode.None));
                }
            });
        }
        // 2.构建排序条件 0-得分降序 1-价格降序 2-价格升序 3-销量降序 4-新品降序
        Integer sort = paramVo.getSort();
        switch (sort) {
            case 1: sourceBuilder.sort("price", SortOrder.DESC); break;
            case 2: sourceBuilder.sort("price", SortOrder.ASC); break;
            case 3: sourceBuilder.sort("sales", SortOrder.DESC); break;
            case 4: sourceBuilder.sort("createTime", SortOrder.DESC); break;
            default:
                sourceBuilder.sort("_score", SortOrder.DESC); break;
        }
        // 3.分页
        Integer pageNum = paramVo.getPageNum();
        Integer pageSize = paramVo.getPageSize();
        sourceBuilder.from((pageNum - 1) * pageSize);
        sourceBuilder.size(pageSize);
        // 4.构建高亮
        sourceBuilder.highlighter(new HighlightBuilder()
                .field("title")
                .preTags("<font style='color:red;'>")
                .postTags("</font>"));
        // 5.构建聚合
        // 5.1. 品牌聚合
        sourceBuilder.aggregation(AggregationBuilders.terms("brandIdAgg").field("brandId")
                .subAggregation(AggregationBuilders.terms("brandNameAgg").field("brandName"))
                .subAggregation(AggregationBuilders.terms("logoAgg").field("logo")));
        // 5.2. 分类聚合
        sourceBuilder.aggregation(AggregationBuilders.terms("categoryIdAgg").field("categoryId")
                .subAggregation(AggregationBuilders.terms("categoryNameAgg").field("categoryName")));
        // 5.3. 规格参数聚合
        sourceBuilder.aggregation(AggregationBuilders.nested("attrAgg", "searchAttrs")
                .subAggregation(AggregationBuilders.terms("attrIdAgg").field("searchAttrs.attrId")
                        .subAggregation(AggregationBuilders.terms("attrNameAgg").field("searchAttrs.attrName"))
                        .subAggregation(AggregationBuilders.terms("attrValueAgg").field("searchAttrs.attrValue"))));
        // 6. 结果集过滤
        sourceBuilder.fetchSource(new String[]{"skuId", "title", "subtitle", "defaultImage", "price"}, null);
        System.out.println(sourceBuilder);
        return sourceBuilder;
    }

}
