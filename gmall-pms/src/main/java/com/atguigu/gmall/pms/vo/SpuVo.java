package com.atguigu.gmall.pms.vo;

import com.atguigu.gmall.pms.entity.SpuEntity;
import lombok.Data;

import java.util.List;

@Data
public class SpuVo extends SpuEntity {
    //海报
    private List<String> spuImages;
    //spu的基本属性及值
    private List<SpuAttrValueVo> baseAttrs;
    //sku
    private List<SkuVo> skus;


}
