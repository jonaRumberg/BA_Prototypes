package com.example.cart_service.model;

import java.util.List;
import java.util.Map;

public class Cart {
    private String userId;
    private List<Map<String, Object>> items; // or create a dedicated CartItem class
    private Double discount;
    private String discountType;

    public Cart() {
    }

    public Cart(String userId, List<Map<String, Object>> items, Double discount, String discountType) {
        this.userId = userId;
        this.items = items;
        this.discount = discount;
        this.discountType = discountType;
    }

    // getters and setters
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public List<Map<String, Object>> getItems() { return items; }
    public void setItems(List<Map<String, Object>> items) { this.items = items; }

    public Double getDiscount() { return discount; }
    public void setDiscount(Double discount) { this.discount = discount; }

    public String getDiscountType() { return discountType; }
    public void setDiscountType(String discountType) { this.discountType = discountType; }
}
