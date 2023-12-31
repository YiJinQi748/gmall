package com.atguigu.gmall.pms.vo;

import com.atguigu.gmall.pms.entity.SkuAttrValueEntity;
import com.atguigu.gmall.pms.entity.SkuEntity;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class SkuVo extends SkuEntity {

    //积分优惠

    private BigDecimal growBounds;

    private BigDecimal buyBounds;

    private List<Integer> work;



    //打折优惠

    private Integer fullCount;

    private BigDecimal discount;

    private Integer ladderAddOther;


    //满减

    private BigDecimal fullPrice;

    private BigDecimal reducePrice;

    private Integer FULLaddOther;

    //sku图片列表
    private List<String> images;

    //销售属性
    private List<SkuAttrValueEntity> saleAttrs;
}
