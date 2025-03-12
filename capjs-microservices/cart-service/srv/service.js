class CartService extends cds.ApplicationService {
  init() {
    this.on("order", "Cart.cart", async ({ params: [id] }) => {
      const cart = await SELECT.one
        .from("CART")
        .columns((a) => {
          a`.*`,
            a.items((b) => {
              b`.*`;
            });
        })
        .where();

      //call the checkout service
      const checkoutService = await cds.connect.to(
        "rest:https://python-checkout.cfapps.us10-001.hana.ondemand.com"
      );

      const order = await checkoutService.post("/api/checkout", {
        products: cart.items,
        discount: { type: cart.DISCOUNT_TYPE, percentage: cart.DISCOUNT },
      });

      // Get the next ORDER_ID
      const { maxOrderId } = await cds.run(
        SELECT.one`max(ORDER_ID) as maxOrderId`.from("ORDERS")
      );
      const nextOrderId = (maxOrderId || 0) + 1;

      const res = await INSERT.into("ORDERS").entries({
        ORDER_ID: nextOrderId,
        ...cart,
        STATUS: "Pending",
        TOTAL: order.total,
        SUBTOTAL: order.subtotal,
      });

      const items = cart.items.map((item) => {
        return {
          ORDER_ID: { ORDER_ID: nextOrderId },
          PRODUCT_ID: item.PRODUCT_ID,
          PRICE: item.PRICE,
        };
      });

      await INSERT.into("ORDER_ITEMS").entries(items);

      await DELETE.from("CART_ITEMS").where(id);

      await UPDATE("CART")
        .set({ DISCOUNT_TYPE: null, DISCOUNT: null })
        .where(id);

      const insertedOrder = await SELECT.one
        .from("ORDERS")
        .columns((a) => {
          a`.*`,
            a.items((b) => {
              b`.*`;
            });
        })
        .where({ ORDER_ID: nextOrderId });

      return insertedOrder;
    });

    this.on("discount", "Cart.cart", async ({ params: [id], data }) => {
      const { type, percentage } = data;

      await UPDATE("CART")
        .set({ DISCOUNT_TYPE: type, DISCOUNT: percentage })
        .where(id);

      return { ...id, DISCOUNT_TYPE: type, DISCOUNT: percentage };
    });

    this.before("CREATE", "Cart.cart_items", async (req) => {
      const productService = await cds.connect.to(
        "rest:https://python-product.cfapps.us10-001.hana.ondemand.com"
      );

      const res = await productService.get(
        "/api/products/" + req.data.PRODUCT_ID
      );

      req.data.PRODUCT_NAME = res.name;
      req.data.PRICE = res.price;
    });
    return super.init();
  }
}
module.exports = CartService;
