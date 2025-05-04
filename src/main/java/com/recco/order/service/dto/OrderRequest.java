package com.recco.order.service.dto;

import java.util.List;

public class OrderRequest {
    private String tableId;

    private List<OrderItemRequest> items;
    
    public OrderRequest() {} 
    
	public String getTableId() {
		return tableId;
	}
	public void setTableId(String tableId) {
		this.tableId = tableId;
	}

	public List<OrderItemRequest> getItems() {
		return items;
	}
	public void setItems(List<OrderItemRequest> items) {
		this.items = items;
	}

//	@Override
//	public String toString() {
//		return "OrderRequest [tableId=" + tableId + ", items=" + items + "]";
//	}
        
}



