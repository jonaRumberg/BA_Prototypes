#define CPPHTTPLIB_OPENSSL_SUPPORT
#include "httplib.h"

#include "json.hpp"
#include <cstdlib>  // For std::getenv
#include <iostream>
#include <string>   // For std::stoi

using namespace httplib;
using json = nlohmann::json;

int main(void) {

    Server srv;

    srv.Get("/api/products", [](const httplib::Request &req, httplib::Response &res) {
        
        std::cout << "Endpoint '/api/products' called" << std::endl;

        Client cli("https://services.odata.org");

        std::string url;

        if(req.has_param("q")){
            std::string query = req.get_param_value("q");
            url = "/V2/Northwind/Northwind.svc/Products?$filter=substringof('" + query + "', ProductName)&%24format=json";
        } else {
            url = "/V2/Northwind/Northwind.svc/Products?%24format=json";
        }

        // Make a GET request to the API
        auto api_res = cli.Get(url.c_str());

        // Check if the request was successful
        if (api_res && api_res->status == 200) {
            // Parse the JSON response
            json api_json = json::parse(api_res->body);

            // Transform the JSON data
            json products = json::array();
            for (const auto& item : api_json["d"]["results"]) {
                products.push_back({
                        {"id", item["ProductID"]},
                        {"name", item["ProductName"]},
                        {"price", item["UnitPrice"]}
                        });
            }

            // Set the response body with the transformed JSON
            res.set_content(products.dump(), "application/json");
        } else {
            // Set an error message if the request failed
            res.set_content("Failed to fetch data", "text/plain");
        }
    });


    // New endpoint for fetching a product by ID
    srv.Get(R"(/api/products/(\d+))", [](const httplib::Request &req, httplib::Response &res) {
        std::cout << "Endpoint '/api/products/:id' called with ID: " << req.matches[1] << std::endl;

        std::string id = req.matches[1];

        Client cli("https://services.odata.org");
        std::string url = "/V2/Northwind/Northwind.svc/Products(" + id + ")?%24format=json";

        // Make a GET request to the API
        auto api_res = cli.Get(url.c_str());

        // Check if the request was successful
        if (api_res && api_res->status == 200) {
            // Parse the JSON response
            json api_json = json::parse(api_res->body);

            // Extract product data
            const auto& productData = api_json["d"];
            if (productData.is_null()) {
                res.status = 404;
                res.set_content("{\"error\": \"Product not found\"}", "application/json");
                return;
            }

            // Create the product JSON object
            json product = {
                {"id", productData["ProductID"]},
                {"name", productData["ProductName"]},
                {"price", productData["UnitPrice"]}
            };

            // Set the response body with the product JSON
            res.set_content(product.dump(), "application/json");
        } else {
            // Set an error message if the request failed
            res.status = 404;
            res.set_content("Product not found", "text/plain");
        }
    });


    //start the server
    const char* port_env = std::getenv("PORT");
    int port = port_env ? std::stoi(port_env) : 8080; // Default to 8080 if PORT is not set

    std::cout << "Server is running on port " << port << std::endl;
    srv.listen("0.0.0.0", port);

    return 0;
}
