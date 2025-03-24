package com.example.checkout.model;

import java.util.List;

public class CheckoutResponse {
    private List<Product> products;
    private Discount discount;
    private double subtotal;
    private double total;

    public CheckoutResponse(List<Product> products, Discount discount, double subtotal, double total) {
        this.products = products;
        this.discount = discount;
        this.subtotal = subtotal;
        this.total = total;
    }

    public List<Product> getProducts() { return products; }
    public Discount getDiscount() { return discount; }
    public double getSubtotal() { return subtotal; }
    public double getTotal() { return total; }
}
