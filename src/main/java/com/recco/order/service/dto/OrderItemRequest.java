package com.recco.order.service.dto;

public class OrderItemRequest {
    private String itemName;
    private Integer quantity;
    private Double price;

	public String getItemName() {
		return itemName;
	}
	public void setItemName(String itemName) {
		this.itemName = itemName;
	}
	public Integer getQuantity() {
		return quantity;
	}
	public void setQuantity(Integer quantity) {
		this.quantity = quantity;
	}
	public Double getPrice() {
		return price;
	}
	public void setPrice(Double price) {
		this.price = price;
	}
//	@Override
//	public String toString() {
//		return "OrderItemRequest [itemName=" + itemName + ", quantity=" + quantity + ", price=" + price + "]";
//	}

    
}
