#include "crow/crow_all.h"
#include "routes.hpp"
#include <cstdlib>
#include <iostream>

int main() {
    crow::SimpleApp app;

    setupRoutes(app); // Define routes in a separate file

    // Get Port from env -> This is needed because cloud foundry does not allow all ports
    const char* portEnv = std::getenv("PORT");
    int port = portEnv ? std::stoi(portEnv) : 3000;

    CROW_LOG_INFO << "Server is running on http://localhost:" << port;
    app.port(port).multithreaded().run();

    return 0;
}
