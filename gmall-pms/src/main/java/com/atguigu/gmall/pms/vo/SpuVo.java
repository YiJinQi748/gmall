package com.atguigu.gmall.pms.vo;

import lombok.Data;

import java.util.List;

@Data
public class SpuVo {
    //海报
    private List<String> spuImages;
    //spu的基本属性及值
    private List<SpuAttrValueVo> baseAttrs;
    //sku
    private List<SkuVo> skus;


}
