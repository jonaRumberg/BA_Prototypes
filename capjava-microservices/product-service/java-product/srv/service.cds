using { Northwind_Service as nwd } from './external/Northwind_Service';

service ProductService @(path: '/api') {
    entity Products as projection on nwd.Products {
        ProductID as id,
        ProductName as name,
        UnitPrice as price
    };
}