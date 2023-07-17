package com.example.gmall.search.controller;


import com.example.gmall.search.pojo.SearchParamVo;
import com.example.gmall.search.pojo.SearchResponseVo;
import com.example.gmall.search.service.SearchService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;


@Controller
public class SearchController {

    @Autowired
    private SearchService searchService;

    @GetMapping("search")
    public String search(SearchParamVo searchParamVo, Model model){
        SearchResponseVo responseVo = this.searchService.search(searchParamVo);
        model.addAttribute("response", responseVo);
        model.addAttribute("searchParam", searchParamVo);
        return "search";
    }

}
