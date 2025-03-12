type order {
    products: array of product;
    discount: discount;
    subtotal: Decimal;
    total: Decimal;
}

type product {
    PRICE: Decimal;
}

type discount {
    type: String;
    percentage: Decimal;
}

service checkout @(path: '/api') {
    action checkout(products: array of product, discount: discount) returns order;
}