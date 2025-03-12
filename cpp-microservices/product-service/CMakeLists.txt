cmake_minimum_required(VERSION 3.10)
project(cpp_product_service)

set(CMAKE_CXX_STANDARD 17)

# Set to use static libraries - cloud foundry needs this
set(OpenSSL_USE_STATIC_LIBS TRUE)

find_package(OpenSSL REQUIRED)
include_directories(include)

# Add debug symbols
set(CMAKE_BUILD_TYPE Debug)

add_executable(microservice src/main.cpp)
target_link_libraries(microservice OpenSSL::SSL OpenSSL::Crypto)
