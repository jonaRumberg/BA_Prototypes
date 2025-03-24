package com.example.product_service.service;

import com.example.product_service.model.Product;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class ProductService {
    private static final String NORTHWIND_BASE_URL = "https://services.odata.org/V2/Northwind/Northwind.svc/Products";
    private final WebClient webClient;

    public ProductService(WebClient.Builder webClientBuilder) {
        this.webClient = webClientBuilder.baseUrl(NORTHWIND_BASE_URL).build();
    }

    public List<Product> getAllProducts() {
        Map<String, Object> response = webClient.get()
                .uri("?$format=json")
                .retrieve()
                .bodyToMono(Map.class)
                .block();

        List<Map<String, Object>> results = (List<Map<String, Object>>) ((Map<String, Object>) response.get("d")).get("results");

        return results.stream()
                .map(data -> new Product(
                        (Integer) data.get("ProductID"),
                        (String) data.get("ProductName"),
                        (Double.parseDouble(data.get("UnitPrice").toString()))
                ))
                .collect(Collectors.toList());
    }

    public Product getProductById(int id) {
        Map<String, Object> response = webClient.get()
                .uri("(" + Integer.toString(id) + ")?$format=json")
                .retrieve()
                .bodyToMono(Map.class)
                .block();

        if (response == null || !response.containsKey("d")) {
            return null;
        }

        Map<String, Object> productData = (Map<String, Object>) response.get("d");
        return new Product(
                (Integer) productData.get("ProductID"),
                (String) productData.get("ProductName"),
                (Double.parseDouble(productData.get("UnitPrice").toString()))
        );
    }
}
