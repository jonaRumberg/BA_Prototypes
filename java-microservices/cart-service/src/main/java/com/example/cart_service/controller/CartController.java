package com.example.cart_service.controller;

import com.example.cart_service.service.CartService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class CartController {
    private final CartService cartService;

    public CartController(CartService cartService) {
        this.cartService = cartService;
    }

    @GetMapping("/cart/{user}")
    public ResponseEntity<?> getCart(@PathVariable String user) {
        return ResponseEntity.ok(cartService.getCart(user));
    }

    @PostMapping("/cart/{user}/items")
    public ResponseEntity<?> addItemToCart(@PathVariable String user, @RequestBody Map<String, Object> requestBody) {
        // Assume requestBody contains at least an "id" field.
        // First, fetch product details from PRODUCT_SERVICE:
        try {
            // For example, using RestTemplate:
            Map<String, Object> product = cartService.getRestTemplate()
                    .getForObject("https://cpp-product.cfapps.us10-001.hana.ondemand.com/api/products/{id}", 
                                  Map.class, requestBody.get("id"));
            return ResponseEntity.ok(cartService.addItemToCart(user, product));
        } catch (Exception e) {
            return ResponseEntity.status(404).body("Product not found");
        }
    }

    @DeleteMapping("/cart/{user}/items/{product}")
    public ResponseEntity<?> deleteItemFromCart(@PathVariable String user, @PathVariable String product) {
        try {
            return ResponseEntity.ok(cartService.deleteItemFromCart(user, product));
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Internal server error");
        }
    }

    @PostMapping("/cart/{user}/discount")
    public ResponseEntity<?> addDiscount(@PathVariable String user, @RequestBody Map<String, Object> discountData) {
        String type = discountData.get("type").toString();
        double percentage = Double.parseDouble(discountData.get("percentage").toString());
        return ResponseEntity.ok(cartService.addDiscountToCart(user, type, percentage));
    }

    @PutMapping("/order/{orderId}")
    public ResponseEntity<?> updateOrder(@PathVariable String orderId, @RequestBody Map<String, Object> updatedOrderData) {
        try {
            // Implement editOrder in your service similar to addOrder.
            Map<String, Object> updatedOrder = cartService.editOrder(orderId, updatedOrderData);
            return ResponseEntity.ok(updatedOrder);
        } catch (RuntimeException e) {
            if ("Order not found".equals(e.getMessage())) {
                return ResponseEntity.status(404).body(e.getMessage());
            }
            return ResponseEntity.status(500).body("Internal server error");
        }
    }

    @GetMapping("/order")
    public ResponseEntity<?> getOrders() {
        return ResponseEntity.ok(cartService.getOrders());
    }

    @GetMapping("/order/{orderId}")
    public ResponseEntity<?> getOrder(@PathVariable String orderId) {
        Object order = cartService.getOrder(orderId);
        if (order == null) {
            return ResponseEntity.status(404).body("Order not found");
        }
        return ResponseEntity.ok(order);
    }

    @PostMapping("/cart/{user}/order")
    public ResponseEntity<?> createOrder(@PathVariable String user) {
        // First, retrieve the cart.
        var cart = cartService.getCart(user);
        if (cart.getItems() == null || cart.getItems().isEmpty()) {
            return ResponseEntity.badRequest().body("Cart is empty. Cannot create order.");
        }
        try {
            // Call the checkout service
            Map<String, Object> order = cartService.getRestTemplate().postForObject(
                "https://cpp-checkout.cfapps.us10-001.hana.ondemand.com/api/checkout",
                Map.of("products", cart.getItems(), "discount", Map.of("type", cart.getDiscountType(), "percentage", cart.getDiscount())),
                Map.class
            );

            order.put("user", user);

            
            // Add order to SAP HANA and clear the cart.
            cartService.addOrder(order); // asynchronous in Node but here executed synchronously
            cartService.clearCart(user);
            return ResponseEntity.ok(order);
        } catch (Exception e) {
            // Log the exception
            System.err.println("Error during order creation: " + e.getMessage());
            e.printStackTrace();

            return ResponseEntity.status(500).body("Internal server error");
        }
    }
}
