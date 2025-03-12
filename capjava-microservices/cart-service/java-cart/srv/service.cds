using { CART,ORDERS, CART_ITEMS } from '../db/schema.cds';

@path: '/api'
service Cart {
    entity cart as projection on CART{
        *
    } actions {
        action order() returns order;
        action discount(type: String, percentage: Decimal) returns order;
    };
    entity cart_items as projection on CART_ITEMS;
    entity order as projection on ORDERS;
}
