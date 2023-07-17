package com.example.gmall.search.pojo;

import com.atguigu.gmall.pms.entity.BrandEntity;
import com.atguigu.gmall.pms.entity.CategoryEntity;
import lombok.Data;

import java.util.List;

@Data
public class SearchResponseVo {

    // 品牌过滤列表
    private List<BrandEntity> brands;

    // 分类过滤列表
    private List<CategoryEntity> categories;

    // 规格参数的过滤列表
    private List<SearchResponseAttrVo> filters;

    // 分页信息
    private Long total; // 总记录数
    private Integer pageNum; // 页码
    private Integer pageSize; // 每页记录数
    private List<Goods> goodsList; // 当前页的数据
}
