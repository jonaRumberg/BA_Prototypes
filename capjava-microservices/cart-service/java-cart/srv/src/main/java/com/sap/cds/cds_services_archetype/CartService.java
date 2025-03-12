package com.sap.cds.cds_services_archetype;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.MultiValueMap;

import com.sap.cds.services.ServiceException;
import com.sap.cds.services.cds.ApplicationService;
import com.sap.cds.services.handler.EventHandler;
import com.sap.cds.services.handler.annotations.Before;
import com.sap.cds.services.handler.annotations.On;
import com.sap.cds.services.handler.annotations.ServiceName;
import com.sap.cds.services.persistence.PersistenceService;
import com.sap.cloud.sdk.cloudplatform.connectivity.HttpDestination;
import com.sap.cloud.sdk.services.rest.apiclient.ApiClient;
import com.sap.cds.Result;
import com.sap.cds.ql.Insert;
import com.sap.cds.ql.Select;
import com.sap.cds.ql.Update;
import com.sap.cds.ql.Delete;
import com.sap.cds.ql.cqn.CqnSelect;
import com.sap.cds.ql.cqn.CqnInsert;
import com.sap.cds.ql.cqn.CqnDelete;
import com.sap.cds.ql.cqn.CqnUpdate;

import cds.gen.Cart_;

@Component
@ServiceName("Cart")
public class CartService implements EventHandler {

    @Autowired
    private PersistenceService db;

    @On(event = "order", entity = "Cart.cart")
    public Map<String, Object> onOrderCart(Map<String, Object> id) {
        try {
            // Get cart with items
            CqnSelect cartQuery = Select.from(Cart_.CDS_NAME)
                    .columns("*")
                    .columns(b -> b.expand("items", i -> i.columns("*")))
                    .where(p -> p.get("ID").eq(id.get("ID")));
            
            Result cartResult = db.run(cartQuery);
            
            if (cartResult.rowCount() == 0) {
                throw new ServiceException(ServiceException.MessageKeys.NO_DATA_FOUND, "Cart not found");
            }
            
            Map<String, Object> cart = cartResult.single().as(Map.class);
            List<Map<String, Object>> cartItems = (List<Map<String, Object>>) cart.get("items");
            
            // Call checkout service using SAP Cloud SDK ApiClient
            Map<String, Object> checkoutPayload = new HashMap<>();
            checkoutPayload.put("products", cartItems);
            
            Map<String, Object> discount = new HashMap<>();
            discount.put("type", cart.get("DISCOUNT_TYPE"));
            discount.put("percentage", cart.get("DISCOUNT"));
            checkoutPayload.put("discount", discount);
            
            // Using HttpDestination with Cloud SDK ApiClient
            HttpDestination checkoutDestination = HttpDestination.builder("https://python-checkout.cfapps.us10-001.hana.ondemand.com").build();
            ApiClient checkoutClient = new ApiClient(checkoutDestination);
            
            // Configure request
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            List<MediaType> acceptedMediaTypes = Collections.singletonList(MediaType.APPLICATION_JSON);
            
            // Invoke API
            ParameterizedTypeReference<Map<String, Object>> typeRef = new ParameterizedTypeReference<Map<String, Object>>() {};
            Map<String, Object> order = checkoutClient.invokeAPI(
                "/api/checkout", 
                HttpMethod.POST, 
                null, // queryParams
                checkoutPayload, // body
                headers, 
                null, // formParams
                acceptedMediaTypes, 
                MediaType.APPLICATION_JSON, 
                null, // authNames
                typeRef
            );
            
            // Get next ORDER_ID
            CqnSelect maxOrderIdQuery = Select.one("ORDERS").columns(c -> c.func("max", "ORDER_ID").as("maxOrderId"));
            Result maxOrderIdResult = db.run(maxOrderIdQuery);
            Integer maxOrderId = maxOrderIdResult.first().isPresent() ? 
                                 maxOrderIdResult.first().get().get("maxOrderId", Integer.class) : 0;
            Integer nextOrderId = maxOrderId + 1;
            
            // Insert new order
            Map<String, Object> orderEntry = new HashMap<>(cart);
            orderEntry.put("ORDER_ID", nextOrderId);
            orderEntry.put("STATUS", "Pending");
            orderEntry.put("TOTAL", order.get("total"));
            orderEntry.put("SUBTOTAL", order.get("subtotal"));
            
            CqnInsert orderInsert = Insert.into("ORDERS").entry(orderEntry);
            db.run(orderInsert);
            
            // Insert order items
            List<Map<String, Object>> orderItems = new ArrayList<>();
            for (Map<String, Object> item : cartItems) {
                Map<String, Object> orderItem = new HashMap<>();
                Map<String, Object> orderIdMap = new HashMap<>();
                orderIdMap.put("ORDER_ID", nextOrderId);
                orderItem.put("ORDER_ID", orderIdMap);
                orderItem.put("PRODUCT_ID", item.get("PRODUCT_ID"));
                orderItem.put("PRICE", item.get("PRICE"));
                orderItems.add(orderItem);
            }
            
            CqnInsert orderItemsInsert = Insert.into("ORDER_ITEMS").entries(orderItems);
            db.run(orderItemsInsert);
            
            // Delete cart items
            CqnDelete deleteCartItems = Delete.from("CART_ITEMS").where(id);
            db.run(deleteCartItems);
            
            // Update cart
            CqnUpdate updateCart = Update.entity("CART")
                    .set("DISCOUNT_TYPE", null)
                    .set("DISCOUNT", null)
                    .where(p -> p.get("ID").eq(id.get("ID")));
            db.run(updateCart);
            
            // Retrieve inserted order
            CqnSelect insertedOrderQuery = Select.from("ORDERS")
                    .columns("*")
                    .columns(b -> b.expand("items", i -> i.columns("*")))
                    .where(p -> p.get("ORDER_ID").eq(nextOrderId));
            
            Result insertedOrderResult = db.run(insertedOrderQuery);
            return insertedOrderResult.single().as(Map.class);
            
        } catch (Exception e) {
            throw new ServiceException(e.getMessage());
        }
    }
    
    @On(event = "discount", entity = "Cart.cart")
    public Map<String, Object> onCartDiscount(Map<String, Object> id, Map<String, Object> data) {
        String type = (String) data.get("type");
        Double percentage = (Double) data.get("percentage");
        
        CqnUpdate updateCart = Update.entity("CART")
                .set("DISCOUNT_TYPE", type)
                .set("DISCOUNT", percentage)
                .where(p -> p.get("ID").eq(id.get("ID")));
        
        db.run(updateCart);
        
        Map<String, Object> result = new HashMap<>(id);
        result.put("DISCOUNT_TYPE", type);
        result.put("DISCOUNT", percentage);
        
        return result;
    }
    
    @Before(event = ApplicationService.EVENT_CREATE, entity = "Cart.cart_items")
    public void beforeCreateCartItem(Map<String, Object> data) {
        try {
            String productId = (String) data.get("PRODUCT_ID");
            
            // Using Cloud SDK for REST call with proper ApiClient
            HttpDestination productDestination = HttpDestination.builder("https://python-product.cfapps.us10-001.hana.ondemand.com").build();
            ApiClient productClient = new ApiClient(productDestination);
            
            // Configure request
            HttpHeaders headers = new HttpHeaders();
            List<MediaType> acceptedMediaTypes = Collections.singletonList(MediaType.APPLICATION_JSON);
            
            // Invoke API
            ParameterizedTypeReference<Map<String, Object>> typeRef = new ParameterizedTypeReference<Map<String, Object>>() {};
            Map<String, Object> product = productClient.invokeAPI(
                "/api/products/" + productId, 
                HttpMethod.GET, 
                null, // queryParams
                null, // body
                headers, 
                null, // formParams
                acceptedMediaTypes, 
                null, // contentType (not needed for GET)
                null, // authNames
                typeRef
            );
            
            data.put("PRODUCT_NAME", product.get("name"));
            data.put("PRICE", product.get("price"));
            
        } catch (Exception e) {
            throw new ServiceException(e.getMessage());
        }
    }
}