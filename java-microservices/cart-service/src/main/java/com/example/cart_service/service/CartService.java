package com.example.cart_service.service;

import com.example.cart_service.dao.HanaDao;
import com.example.cart_service.model.Cart;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Service
public class CartService {
    private final HanaDao hanaDao;
    private final RestTemplate restTemplate;
    
    // External service URLs (could also be externalized in application.properties)
    private final String PRODUCT_SERVICE = "https://cpp-product.cfapps.us10-001.hana.ondemand.com";
    private final String CHECKOUT_SERVICE = "https://cpp-checkout.cfapps.us10-001.hana.ondemand.com";

    public CartService(HanaDao hanaDao) {
        this.hanaDao = hanaDao;
        this.restTemplate = new RestTemplate();
    }

    private Cart emptyCart(String user) {
        return new Cart(user, List.of(), null, null);
    }

    public Cart getCart(String user) {
        // Get the discount information
        List<Map<String, Object>> discountRes = hanaDao.exec("SELECT * FROM cart WHERE USER_ID = " + user);
        if (discountRes.isEmpty()) {
            hanaDao.exec("INSERT INTO cart VALUES (" + user + ", NULL, NULL)");
            return emptyCart(user);
        }
        Map<String, Object> discount = discountRes.get(0);
        List<Map<String, Object>> itemsRes = hanaDao.exec("SELECT * FROM cart_items WHERE USER_ID = " + user);
        Cart cart = new Cart();
        cart.setUserId(user);
        cart.setItems(itemsRes);
        // Assume discount columns are named DISCOUNT and DISCOUNT_TYPE
        cart.setDiscount(discount.get("DISCOUNT") != null ? ((BigDecimal) discount.get("DISCOUNT")).doubleValue() : 0.0);
        cart.setDiscountType(discount.get("DISCOUNT_TYPE") != null ? (String) discount.get("DISCOUNT_TYPE") : "");
        return cart;
    }

    public Cart clearCart(String user) {
        hanaDao.exec("UPDATE cart SET DISCOUNT = NULL, DISCOUNT_TYPE = NULL WHERE USER_ID = " + user);
        hanaDao.exec("DELETE FROM cart_items WHERE USER_ID = " + user);
        return getCart(user);
    }

    public Cart addItemToCart(String user, Map<String, Object> item) {
        Cart cart = getCart(user);
        // Check if the item already exists (assuming item has PRODUCT_ID)
        boolean exists = cart.getItems().stream().anyMatch(
                cartItem -> cartItem.get("PRODUCT_ID").toString().equals(item.get("id").toString()));
        if (exists) {
            return cart;
        }
        // Note: Make sure to properly sanitize inputs in production.
        String sql = "INSERT INTO cart_items VALUES(" + user + ", " +
                item.get("id") + ", " +
                item.get("price") + ", '" +
                item.get("name").toString().replace("'", "") + "')";
        hanaDao.exec(sql);
        return getCart(user);
    }

    public Cart deleteItemFromCart(String user, String product) {
        hanaDao.exec("DELETE FROM cart_items WHERE PRODUCT_ID = " + product);
        return getCart(user);
    }

    public Cart addDiscountToCart(String user, String discountType, double discountPercentage) {
        List<Map<String, Object>> discountRes = hanaDao.exec("SELECT * FROM cart WHERE USER_ID = " + user);
        if (discountRes.isEmpty()) {
            hanaDao.exec("INSERT INTO cart VALUES (" + user + ", NULL, NULL)");
            return emptyCart(user);
        }
        String sql = "UPDATE cart SET DISCOUNT = " + discountPercentage +
                ", DISCOUNT_TYPE = '" + discountType + "' WHERE USER_ID = " + user;
        hanaDao.exec(sql);
        return getCart(user);
    }

    // Implement methods for orders: addOrder, getOrders, getOrder, editOrder.
    // As an example, here’s a simplified version for addOrder:

    public Map<String, Object> addOrder(Map<String, Object> order) {
        //set base values
        order.put("status", "created");

        String insertOrderSql = "INSERT INTO orders(USER_ID, DISCOUNT_TYPE, DISCOUNT, SUBTOTAL, TOTAL, STATUS) VALUES (" +
                order.get("user") + ", '" +
                ((Map<String, Object>) order.get("discount")).get("type") + "', " +
                (((Map<String, Object>) order.get("discount")).get("percentage") != null ? ((Map<String, Object>) order.get("discount")).get("percentage") : 0) + ", " +
                order.get("subtotal") + ", " +
                order.get("total") + ", '" +
                order.get("status") + "')";
        hanaDao.exec(insertOrderSql);

        // Retrieve the newly created order (assuming ORDER_ID is auto-generated and CREATED_AT is used for ordering)
        List<Map<String, Object>> orderQuery = hanaDao.exec("SELECT * FROM orders WHERE USER_ID = " + order.get("user") + " ORDER BY CREATED_AT DESC LIMIT 1");

        // Construct order_items INSERT statement. This example assumes multiple products.
        List<Map<String, Object>> products = (List<Map<String, Object>>) order.get("products");
        for (Map<String, Object> product : products) {
            StringBuilder query = new StringBuilder("INSERT INTO order_items VALUES (");
            query.append(orderQuery.get(0).get("ORDER_ID")).append(", ")
                 .append(product.get("PRODUCT_ID")).append(", ")
                 .append(product.get("PRICE")).append("); \n");
            hanaDao.exec(query.toString());
        }
    
        return order;
    }

    public Map<String, Object> editOrder(String orderId, Map<String, Object> updatedOrderData) {
        // Check if the order exists
        List<Map<String, Object>> orderRes = hanaDao.exec("SELECT * FROM orders WHERE ORDER_ID = " + orderId);
        if (orderRes.isEmpty()) {
            throw new RuntimeException("Order not found");
        }

        // Merge existing order data with updated data
        Map<String, Object> existingOrder = orderRes.get(0);
        existingOrder.putAll(updatedOrderData);

        // Update the order in the database
        String updateSql = "UPDATE orders SET " +
                "DISCOUNT_TYPE = '" + existingOrder.get("DISCOUNT_TYPE") + "', " +
                "DISCOUNT = " + existingOrder.get("DISCOUNT") + ", " +
                "SUBTOTAL = " + existingOrder.get("SUBTOTAL") + ", " +
                "TOTAL = " + existingOrder.get("TOTAL") + ", " +
                "STATUS = '" + existingOrder.get("STATUS") + "' " +
                "WHERE ORDER_ID = " + orderId;
        hanaDao.exec(updateSql);

        return existingOrder;
    }

    public Map<String, Object> getOrder(String orderId) {
        // Retrieve the order by ID
        List<Map<String, Object>> orders = hanaDao.exec("SELECT * FROM orders WHERE ORDER_ID = " + orderId);
        if (orders.isEmpty()) {
            return null; // Return null if the order is not found
        }
        // Retrieve the items associated with the order
        List<Map<String, Object>> orderItems = hanaDao.exec("SELECT * FROM order_items WHERE ORDER_ID = " + orderId);
        Map<String, Object> order = orders.get(0);
        order.put("items", orderItems);
        return order;
    }

    public Object getOrders() {
        // Retrieve all orders, ordered by creation date
        return hanaDao.exec("SELECT * FROM orders ORDER BY CREATED_AT DESC");
    }

    public RestTemplate getRestTemplate() {
        return this.restTemplate;
    }


    // Similarly, add getOrders(), getOrder(String orderId), editOrder(String orderId, Map<String,Object> updatedData)
    // You can use hanaDao.exec() with the corresponding SQL statements.

    // For external calls to the PRODUCT_SERVICE and CHECKOUT_SERVICE, you can use restTemplate.getForObject(...)
    // or restTemplate.postForObject(...) as needed.
}
