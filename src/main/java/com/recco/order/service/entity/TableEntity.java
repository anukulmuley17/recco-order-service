package com.recco.order.service.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "tables") // maps to the table `dev_db.tables`
public class TableEntity {

    @Id
    private String tableId;

    // Add other fields if needed, like status, etc.

    // Getters and Setters
    public String getTableId() {
        return tableId;
    }

    public void setTableId(String tableId) {
        this.tableId = tableId;
    }
}
