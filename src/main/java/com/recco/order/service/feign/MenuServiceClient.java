package com.recco.order.service.feign;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

//@FeignClient(name = "menu-service", url = "http://localhost:8082/api/menu")
@FeignClient(name = "menu-service", url = "http://localhost:8082/api")
public interface MenuServiceClient {
	 @GetMapping("/menu/price")
	    Double getItemPrice(@RequestParam("name") String name,@RequestParam("qrToken") String qrToken);
	 @GetMapping("/admin/menu/price")
	    Double getItemPriceAdmin(@RequestParam("name") String name,@RequestParam("tableId") String tableId);
	 
}
