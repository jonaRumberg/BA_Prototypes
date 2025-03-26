package com.sap.cds.cds_service_archetype;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import com.sap.cds.Result;
import com.sap.cds.services.cds.CdsReadEventContext;
import com.sap.cds.services.cds.CqnService;
import com.sap.cds.services.handler.EventHandler;
import com.sap.cds.services.handler.annotations.On;
import com.sap.cds.services.handler.annotations.ServiceName;

@Component
@ServiceName("ProductService")
public class ProductService implements EventHandler {
    
    @Autowired
    @Qualifier("Northwind_Service")
    CqnService productsService;

    @On(event = "READ", entity = "ProductService.Products")
    public Result readNprod(CdsReadEventContext context) {
        System.out.println("Reading nprod");

        System.out.println(context.getCqn());
        
        Result res;
        try {
            res = productsService.run(context.getCqn());
        } catch (Exception e) {
            System.err.println("Error while reading nprod: " + e.getMessage());
            throw e;
        }

        return res;
    }
}