#include "httplib.h"
#include "json.hpp"
#include <cstdlib>  // For std::getenv
#include <iostream>
#include <string>   // For std::stoi

using namespace httplib;
using json = nlohmann::json;

int main(void) {

    Server srv;

    srv.Post("/api/checkout", [](const httplib::Request &req, httplib::Response &res) {

            const std::basic_string<char> body = req.body;

            json request_json = json::parse(body);
            json products = request_json["products"];
            json discount = request_json["discount"];

            float subtotal = 0;

            //sum up product prices
            for (json::iterator it = products.begin(); it != products.end(); ++it) {
                json product = *it;


                if(product["PRICE"].is_string()){
                    product["PRICE"] = std::stof(product["PRICE"].get<std::string>());
                }

                if(product["PRICE"].is_number()){
                    subtotal += product["PRICE"].get<float>();
                }
            }

            float total = subtotal;

            //apply discount if available
            if( discount.contains("type") && discount.contains("percentage") 
             && discount["type"].is_string() && discount["percentage"].is_number()){

                const std::string type = discount["type"].get<std::string>();
                const float percent = discount["percentage"];

                if(type == "percentage"){
                    total -= subtotal * percent;
                }
            }

            //assemble result json
            json result_json = request_json;
            result_json["total"] = total;
            result_json["subtotal"] = subtotal;

            res.set_content(result_json.dump(), "application/json");
            });

    //start the server
    const char* port_env = std::getenv("PORT");
    int port = port_env ? std::stoi(port_env) : 8080; // Default to 8080 if PORT is not set

    std::cout << "Server is running on port " << port << std::endl;
    srv.listen("0.0.0.0", port);

    return 0;
}
