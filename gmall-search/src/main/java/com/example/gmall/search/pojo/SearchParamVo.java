package com.example.gmall.search.pojo;

import lombok.Data;

import java.util.List;

/**
 * http://search.gmall.com/search?keyword=手机&brandId=1,2,3&categoryId=225&props=4:8G-12G&props=5:128G-256G&sort=1
 *  &priceFrom=1000&priceTo=2000&pageNum=3&store=true
 */
@Data
public class SearchParamVo {

    // 搜索关键字
    private String keyword;

    // 品牌id过滤条件
    private List<Long> brandId;

    // 分类id过滤条件
    private List<Long> categoryId;

    // 规格参数过滤条件：["4:8G-12G", "5:128G-256G"]
    private List<String> props;

    // 排序条件：0-得分降序 1-价格降序 2-价格升序 3-销量降序 4-新品降序
    private Integer sort = 0;

    // 价格区间
    private Double priceFrom;
    private Double priceTo;

    // 分页参数
    private Integer pageNum = 1;
    private final Integer pageSize = 20;

    // 仅显示有货
    private Boolean store;
}
