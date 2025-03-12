entity CART{
    key USER_ID    : Integer;
    DISCOUNT       : Decimal;
    DISCOUNT_TYPE  : String;
    items          : Association to many CART_ITEMS on items.USER_ID = $self;
}

entity CART_ITEMS{
    key PRODUCT_ID : Integer;
    key USER_ID    : Association to one CART;
    PRICE          : Decimal;
    PRODUCT_NAME   : String;
}

entity ORDERS{
    key ORDER_ID   : Integer;
    CREATED_AT     : Timestamp @cds.on.insert: $now;
    DISCOUNT       : Decimal null;
    DISCOUNT_TYPE  : String null;
    STATUS         : String;
    TOTAL          : Decimal;
    SUBTOTAL       : Decimal;
    items          : Association to many ORDER_ITEMS on items.ORDER_ID = $self;
}

entity ORDER_ITEMS {
    key ORDER_ID   : Association to one ORDERS;
    key PRODUCT_ID : Integer;
    PRICE          : Decimal;
}
