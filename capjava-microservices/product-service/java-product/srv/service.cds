using { Northwind_Service as nwd } from './external/Northwind_Service';

service Product @(path: '/api') {
    // entity products as projection on Products {
    //     ProductID as id,
    //     ProductName as name,
    //     UnitPrice as price
    // };

    entity Products as projection on nwd.Products;
}