package com.sap.cds.cds_services_archetype;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.util.EntityUtils;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import com.sap.cds.services.ServiceException;
import com.sap.cds.services.cds.CdsCreateEventContext;
import com.sap.cds.services.handler.EventHandler;
import com.sap.cds.services.handler.annotations.On;
import com.sap.cds.services.handler.annotations.ServiceName;
import com.sap.cds.services.persistence.PersistenceService;
import com.sap.cds.services.request.ParameterInfo;
import com.sap.cloud.sdk.cloudplatform.connectivity.HttpClientAccessor;
import com.sap.cds.Result;
import com.sap.cds.Row;
import com.sap.cds.ql.Insert;
import com.sap.cds.ql.Select;
import com.sap.cds.ql.Update;
import com.sap.cds.ql.CQL;
import com.sap.cds.ql.Delete;
import com.sap.cds.ql.cqn.CqnSelect;
import com.sap.cds.ql.cqn.CqnDelete;
import com.sap.cds.ql.cqn.CqnInsert;
import com.sap.cds.ql.cqn.CqnUpdate;
import com.sap.cds.services.handler.annotations.Before;

import static cds.gen.cart.CartModel_.CART;
import static cds.gen.cart.CartModel_.ORDER;
import cds.gen.cart.Cart;
import cds.gen.cart.CartDiscountContext;
import cds.gen.cart.CartOrderContext;
import cds.gen.cart.Order;

@Component
@ServiceName("Cart")
public class CartService implements EventHandler {

    @Autowired
    private PersistenceService db;

    @Autowired
    ParameterInfo parameterInfo;

    @On(event = "order", entity = "Cart.cart")
    public void onOrderCart(CartOrderContext context) {

        try {
            CqnSelect cqn = context.getCqn();
            Result data = db.run(cqn);

            Integer cartId = (Integer) data.single().get("USER_ID");

            // Get cart with items
            CqnSelect cartQuery = Select.from(CART)
                    .columns(b -> b._all(), b -> b.items().expand(i -> i._all()))
                    .where(p -> p.USER_ID().eq(cartId));

            Result cartResult = db.run(cartQuery);

            if (cartResult.rowCount() == 0) {
                throw new ServiceException("NO_DATA_FOUND", "Cart not found");
            }

            Map<String, Object> cart = cartResult.single().as(Map.class);
            List<Map<String, Object>> cartItems = (List<Map<String, Object>>) cart.get("items");

            // Call checkout service using SAP Cloud SDK ApiClient
            JSONObject jsonPayload = new JSONObject();
            JSONArray productsArray = new JSONArray();
            JSONObject discountObject = new JSONObject();

            // List<NameValuePair> checkoutPayload = new ArrayList<>();
            for (Map<String, Object> item : cartItems) {
                JSONObject jsonItem = new JSONObject();

                for (Map.Entry<String, Object> entry : item.entrySet()) {

                    if (entry.getKey().equals("PRICE")) {
                        jsonItem.put(entry.getKey(), new BigDecimal(entry.getValue().toString()).toPlainString());
                    } else {
                        jsonItem.put(entry.getKey(), entry.getValue());
                    }
                }

                productsArray.put(jsonItem);
            }

            discountObject.put("type", cart.get("DISCOUNT_TYPE").toString());
            discountObject.put("percentage", cart.get("DISCOUNT").toString());

            jsonPayload.put("discount", discountObject);
            jsonPayload.put("products", productsArray);

            HttpClient client = HttpClientAccessor.getHttpClient();
            HttpPost request = new HttpPost("https://python-checkout.cfapps.us10-001.hana.ondemand.com/api/checkout");

            // Set the entity
            StringEntity jsonEntity = new StringEntity(jsonPayload.toString(), "UTF-8");
            jsonEntity.setContentType("application/json");
            request.setEntity(jsonEntity);
            request.setHeader("Content-Type", "application/json");
            request.setHeader("Accept", "application/json");

            HttpResponse resp = client.execute(request);
            HttpEntity entity = resp.getEntity();
            String orderString = EntityUtils.toString(entity, "UTF-8");
            JSONObject order = new JSONObject(orderString);

            // Get next ORDER_ID
            CqnSelect maxOrderIdQuery = Select.from(ORDER).columns(c -> CQL.max(c.ORDER_ID()).as("maxOrderId"));
            Result maxOrderIdResult = db.run(maxOrderIdQuery);
            Row maxOrderIdRow = maxOrderIdResult.single();
            Integer maxOrderId = Integer.parseInt(maxOrderIdRow.get("maxOrderId").toString());
            Integer nextOrderId = maxOrderId + 1;

            // Insert new order
            Map<String, Object> orderEntry = new HashMap<>();
            JSONObject orderDiscount = order.getJSONObject("discount");
            orderEntry.put("ORDER_ID", nextOrderId);
            orderEntry.put("DISCOUNT_TYPE", orderDiscount.getString("type"));
            orderEntry.put("DISCOUNT", orderDiscount.get("percentage"));
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
            CqnDelete deleteCartItems = Delete.from("CART_ITEMS").where(p -> p.get("USER_ID_USER_ID").eq(cartId));
            db.run(deleteCartItems);

            // Update cart
            Cart cartEntity = Cart.create();
            cartEntity.setUserId(cartId);
            cartEntity.setDiscountType(null);
            cartEntity.setDiscount(null);

            CqnUpdate updateCart = Update.entity(CART).data(cartEntity);
            db.run(updateCart);

            // Retrieve inserted order
            CqnSelect insertedOrderQuery = Select.from(ORDER)
                    .columns(b -> b._all(), b -> b.items().expand(i -> i._all()))
                    .where(p -> p.get("ORDER_ID").eq(nextOrderId));

            Result insertedOrderResult = db.run(insertedOrderQuery);
            context.setResult(insertedOrderResult.single().as(Order.class));
            context.setCompleted();

        } catch (Exception e) {
            throw new ServiceException(e.getMessage());
        }
    }

    @On(event = "discount", entity = "Cart.cart")
    public void onCartDiscount(CartDiscountContext context) {
        CqnSelect cqn = context.getCqn();
        Result data = db.run(cqn);

        Integer cartId = (Integer) data.single().get("USER_ID");

        Cart cart = Cart.create();
        cart.setDiscountType(context.getType());
        cart.setDiscount(context.getPercentage());
        cart.setUserId(cartId);

        CqnUpdate updateCart = Update.entity(CART).data(cart);
        db.run(updateCart);

        context.setResult(cart);
        context.setCompleted();
    }

    @Before(event = "CREATE", entity = "Cart.cart_items")
    public void beforeCreateCartItems(CdsCreateEventContext context) {
        try {
            Map<String, Object> data = context.getCqn().entries().get(0);
            Integer productId = (Integer) data.get("PRODUCT_ID");

            HttpClient client = HttpClientAccessor.getHttpClient();
            HttpGet request = new HttpGet("https://python-product.cfapps.us10-001.hana.ondemand.com" + "/api/products/" + productId);

            HttpResponse response = client.execute(request);
            HttpEntity entity = response.getEntity();
            String productString = EntityUtils.toString(entity, "UTF-8");
            JSONObject product = new JSONObject(productString);

            data.put("PRODUCT_NAME", product.getString("name"));
            data.put("PRICE", product.getBigDecimal("price"));
        } catch (Exception e) {
            throw new ServiceException(e.getMessage());
        }
    }

}