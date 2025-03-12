#pragma once
#include "crow/crow_all.h"

void setupRoutes(crow::SimpleApp& app) {
    CROW_ROUTE(app, "/")([]() {
        return "Hello, Microservice";
    });


    CROW_ROUTE(app, "/json")([]() {
        crow::json::wvalue res;
        res["message"] = "Hello, JSON!";
        return res;
    });
}
