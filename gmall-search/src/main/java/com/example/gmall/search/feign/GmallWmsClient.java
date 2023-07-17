package com.example.gmall.search.feign;

import com.atguigu.gmall.wms.api.GmallWmsApi;
import org.springframework.cloud.openfeign.FeignClient;

@FeignClient("wms-server")
public interface GmallWmsClient extends GmallWmsApi {
}
