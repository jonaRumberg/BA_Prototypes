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

@path: '/api'
service checkout {
    action checkout(products: array of product, discount: discount) returns order;
}
