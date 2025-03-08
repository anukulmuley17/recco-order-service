package com.recco.order.service.feign;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = "menu-service", url = "http://localhost:8082/api/menu")
public interface MenuServiceClient {
	 @GetMapping("/price")
	    Double getItemPrice(@RequestParam("name") String name);
}
