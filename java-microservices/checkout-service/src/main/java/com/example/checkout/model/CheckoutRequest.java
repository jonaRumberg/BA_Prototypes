package com.example.checkout.model;

import java.util.List;

public class CheckoutRequest {
    private List<Product> products;
    private Discount discount;

    public List<Product> getProducts() { return products; }
    public void setProducts(List<Product> products) { this.products = products; }

    public Discount getDiscount() { return discount; }
    public void setDiscount(Discount discount) { this.discount = discount; }
}
