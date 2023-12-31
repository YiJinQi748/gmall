package com.example.gmall.search.pojo;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.DateFormat;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

import java.util.Date;
import java.util.List;

@Data
@Document(indexName = "goods",shards = 3,replicas = 2)
public class Goods {
    // sku基本信息
    @Id
    private Long skuId;
    @Field(type = FieldType.Text, analyzer = "ik_max_word")
    private String title;
    @Field(type = FieldType.Keyword, index = false)
    private String subtitle;
    @Field(type = FieldType.Keyword, index = false)
    private String defaultImage;
    @Field(type = FieldType.Double)
    private Double price;

    @Field(type = FieldType.Long)
    private Long sales = 0L; // 销量
    @Field(type = FieldType.Date, format = DateFormat.basic_date)
    private Date createTime; // 新品排序字段，spu的创建时间
    @Field(type = FieldType.Boolean)
    private Boolean store = false; // 是否有货：true-有货

    // 品牌聚合所需字段
    @Field(type = FieldType.Long)
    private Long brandId;
    @Field(type = FieldType.Keyword)
    private String brandName;
    @Field(type = FieldType.Keyword)
    private String logo;

    // 分类聚合所需字段
    @Field(type = FieldType.Long)
    private Long categoryId;
    @Field(type = FieldType.Keyword)
    private String categoryName;

    // 规格参数聚合所需的字段
    @Field(type = FieldType.Nested)
    private List<SearchAttrValue> searchAttrs;
}
