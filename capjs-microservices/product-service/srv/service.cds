using { Northwind_Service.Products as Products } from './external/Northwind_Service';

service Product @(path: '/api') {
    entity products as projection on Products {
        ProductID as id,
        ProductName as name,
        UnitPrice as price
    };
}