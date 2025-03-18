package com.sap.cds.cds_services_archetype;

import java.math.BigDecimal;
import java.util.Collection;

import org.springframework.stereotype.Component;

import com.sap.cds.services.handler.EventHandler;
import com.sap.cds.services.handler.annotations.On;
import com.sap.cds.services.handler.annotations.ServiceName;

import cds.gen.Discount;
import cds.gen.Order;
import cds.gen.Product;
import cds.gen.checkout.CheckoutContext;

@Component
@ServiceName("checkout")
public class CheckoutService implements EventHandler {

    @On(event = "checkout")
    public void onCheckout(CheckoutContext context) {

        Discount discount = context.getDiscount();
        Collection<Product> products = context.getProducts();

        // Initialize subtotal and total
        double subtotal = products.stream()
                .mapToDouble(product -> product.getPrice().doubleValue())
                .sum();
        double total = subtotal;

        // Apply discount if available
        if ("percentage".equalsIgnoreCase(discount.getType())) {
            total -= subtotal * discount.getPercentage().doubleValue();
        }

        // Create and populate the Order object
        Order order = Order.create();
        order.setProducts(products);
        order.setDiscount(discount);
        order.setSubtotal(BigDecimal.valueOf(subtotal));
        order.setTotal(BigDecimal.valueOf(total));

        context.setResult(order);
        context.setCompleted();

    }
}
