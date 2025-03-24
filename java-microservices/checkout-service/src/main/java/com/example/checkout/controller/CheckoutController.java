package com.example.checkout.controller;

import com.example.checkout.model.CheckoutRequest;
import com.example.checkout.model.CheckoutResponse;
import com.example.checkout.model.Product;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
public class CheckoutController {

    @PostMapping("/checkout")
    public CheckoutResponse checkout(@RequestBody CheckoutRequest request) {
        List<Product> products = request.getProducts();
        double subtotal = products.stream().mapToDouble(Product::getPrice).sum();
        double total = subtotal;

        if (request.getDiscount() != null && "percentage".equals(request.getDiscount().getType())) {
            total -= subtotal * request.getDiscount().getPercentage();
        }

        return new CheckoutResponse(products, request.getDiscount(), subtotal, total);
    }
}
