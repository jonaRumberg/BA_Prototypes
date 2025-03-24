package com.example.product_service.controller;

import com.example.product_service.model.Product;
import com.example.product_service.service.ProductService;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/products")
public class ProductController {
    private final ProductService productService;

    public ProductController(ProductService productService) {
        this.productService = productService;
    }

    @GetMapping
    public List<Product> getProducts(@RequestParam(required = false) String q) {
        List<Product> products = productService.getAllProducts();
        
        if (q != null && !q.isEmpty()) {
            return products.stream()
                    .filter(product -> product.getName().toUpperCase().contains(q.toUpperCase()))
                    .collect(Collectors.toList());
        }
        return products;
    }

    @GetMapping("/{id}")
    public Product getProductById(@PathVariable int id) {
        Product product = productService.getProductById(id);
        if (product == null) {
            throw new RuntimeException("Product not found");
        }
        return product;
    }
}
