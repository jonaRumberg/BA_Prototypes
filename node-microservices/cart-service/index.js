import express from "express"
import axios from "axios"
import bodyParser from "body-parser"
import hana from "@sap/hana-client"

const PORT = process.env.PORT || 3001
const app = express()

app.use(bodyParser.json())

const PRODUCT_SERVICE  = "https://cpp-product.cfapps.us10-001.hana.ondemand.com"
const CHECKOUT_SERVICE = "https://cpp-checkout.cfapps.us10-001.hana.ondemand.com"

const conn = hana.createConnection()
const connParams = {
    serverNode  : '8bc93cde-866b-48eb-ae6f-f9ee37acb91e.hana.trial-us10.hanacloud.ondemand.com:443',
    uid         : 'DBADMIN', //󰚌 󰚌 󰚌 
    pwd         : '7605B1zEe'
}
await new Promise((resolve, reject) => conn.connect(connParams, err => {
    if (err) reject(err)

    resolve()
}))


//------------------- Services ---------------------
/**
 * Provides a promise that executes a statment on the connected hana
 *
 * @param {string} statement - The statement that gets executed
 * @returns Promise<error|result> The statement result
 * 
 */
const promiseExecHana = statement => new Promise((resolve, reject) => {
    conn.exec(statement, (error, result) => {
        if (error) reject(error)
        resolve(result)
    })
})

/**
 * Creates an empty cart for a specific user.
 *
 * @param {string} user - The identifier for the user.
 * @returns {Object} An empty cart object for the user.
 */
const emptyCart = user => { return {id: user, items: [], discount: {}} }


/**
 * Retrieves the cart for a specific user.
 * If the user's cart does not exist, it creates a new empty cart for the user.
 *
 * @param {string} user - The identifier for the user.
 * @returns {Object} The cart for the user.
 */
const getCart = async (user) => {

    const discountRes = await promiseExecHana(`SELECT * FROM cart WHERE USER_ID = ${user}`)
    if(discountRes.length === 0){
        await promiseExecHana(`INSERT INTO cart VALUES (${user}, NULL, NULL)`)
        return emptyCart(user) 
    }

    const discount = discountRes[0]
    const itemsRes = await promiseExecHana(`SELECT * FROM cart_items WHERE USER_ID = ${user}`)
    const cart = {...discount, items: itemsRes }

    return cart
}


/**
 * Clears the cart for a specific user.
 * If the user's cart does not exist, it creates a new empty cart for the user.
 *
 * @param {string} user - The identifier for the user.
 * @returns {Object} The cleared or newly created cart for the user.
 */
const clearCart = async (user) => {

    await promiseExecHana(`UPDATE cart SET DISCOUNT = NULL, DISCOUNT_TYPE = NULL WHERE USER_ID = ${user}`)
    await promiseExecHana(`DELETE FROM cart_items WHERE USER_ID = ${user}`)

    return await getCart(user)
}


/**
 * Adds an item to the user's cart.
 * If the user's cart does not exist, it creates a new cart for the user.
 *
 * @param {string} user - The identifier for the user.
 * @param {Object} item - The item to be added to the cart.
 * @returns {Object} The updated cart for the user.
 */
const addItemToCart = async (user, item) => {

    const cart = await getCart(user)

    if (cart.items.some(cartItem => cartItem.PRODUCT_ID === item.id)) {
        return cart
    }
    await promiseExecHana(`INSERT INTO cart_items VALUES(${user}, ${item.id}, ${item.price}, '${item.name.replace("'", "")}')`)

    return await getCart(user)
}

/**
 * Deletes an item from the user's cart.
 *
 * @param {string} user - The identifier for the user.
 * @param {string} item - The identifier for the item to be deleted.
 * @returns {Object} The updated cart for the user.
 */
const deleteItemFromCart = async (user, item) => {
    await promiseExecHana(`DELETE FROM cart_items WHERE PRODUCT_ID = ${item}`)    

    return await getCart(user)
}

/**
 * Adds a discount to the user's cart.
 * If the user's cart does not exist, it creates a new empty cart for the user.
 *
 * @param {string} user - The identifier for the user.
 * @param {string} discountType - The type of discount to be applied.
 * @param {number} discoutPercentage - The percentage of the discount.
 * @returns {Object} The updated cart for the user.
 */
const addDiscountToCart = async (user, discountType, discoutPercentage) => {

    //make sure cart exists
    const discountRes = await promiseExecHana(`SELECT * FROM cart WHERE USER_ID = ${user}`)
    if(discountRes.length === 0){
        await promiseExecHana(`INSERT INTO cart VALUES (${user}, NULL, NULL)`)
        return emptyCart(user) 
    }

    await promiseExecHana(`UPDATE cart SET DISCOUNT = ${discoutPercentage}, DISCOUNT_TYPE = '${discountType}' WHERE USER_ID = ${user}`)

    return await getCart(user)
}

/**
 * Adds an order to the list of orders.
 *
 * @param {Object} order - The order to be added.
 * @returns {Object} The added order.
 */
const addOrder = async (order) => {

    await promiseExecHana(`INSERT INTO orders(USER_ID, DISCOUNT_TYPE, DISCOUNT, SUBTOTAL, TOTAL, STATUS) VALUES (${order.user}, '${order.discount.type}', ${order.discount.percentage ? order.discount.percentage : 0}, ${order.subtotal}, ${order.total}, '${order.status}')`)
  
    const orderQuery = await promiseExecHana(`SELECT * FROM orders WHERE USER_ID = ${order.user} ORDER BY CREATED_AT DESC LIMIT 1`)

    let query = `INSERT INTO order_items VALUES (`

    if(order.products.length == 1) {
        query += `${orderQuery[0].ORDER_ID}, ${order.products[0].PRODUCT_ID}, ${order.products[0].PRICE}`
    } else {
        order.products.forEach(product => {
            query += `(${orderQuery[0].ORDER_ID}, ${product.PRODUCT_ID}, ${product.PRICE})`
        })
    }

    query += `)`

    console.log(query)
    await promiseExecHana(query)

    return order;
}


/**
 * Retrieves all orders.
 *
 * @returns {Array} A list of all orders.
 */
const getOrders = async () => {
    const orders = await promiseExecHana(`SELECT * FROM orders ORDER BY CREATED_AT DESC`);
    return orders;
}

/**
 * Retrieves a specific order by its ID.
 *
 * @param {string} orderID - The identifier for the order.
 * @returns {Object|undefined} The order if found, otherwise undefined.
 */
const getOrder = async (orderID) => {
    const orders = await promiseExecHana(`SELECT * FROM orders WHERE ORDER_ID = ${orderID}`);
    if (orders.length === 0) {
        return undefined;
    }
    const orderItems = await promiseExecHana(`SELECT * FROM order_items WHERE ORDER_ID = ${orderID}`);
    return { ...orders[0], items: orderItems };
}

/**
 * Edits an existing order with new data.
 *
 * @param {string} orderId - The identifier for the order.
 * @param {Object} updatedOrderData - The new data for the order.
 * @returns {Object} The updated order.
 * @throws Will throw an error if the order is not found.
 */
const editOrder = async (orderId, updatedOrderData) => {
    const orderRes = await promiseExecHana(`SELECT * FROM orders WHERE ORDER_ID = ${orderId}`);

    if (orderRes.length === 0) {
        throw new Error("Order not found");
    }

    const updatedOrder = { ...orderRes[0], ...updatedOrderData };

    await promiseExecHana(`UPDATE orders SET DISCOUNT_TYPE = '${updatedOrder.DISCOUNT_TYPE}', DISCOUNT = ${updatedOrder.DISCOUNT}, SUBTOTAL = ${updatedOrder.SUBTOTAL}, TOTAL = ${updatedOrder.TOTAL}, STATUS = '${updatedOrder.STATUS}' WHERE ORDER_ID = ${orderId}`);

    return updatedOrder;
}


//------------------ Endpoints ---------------------
/**
 * Endpoint to retrieve the cart for a specific user.
 */
app.get("/api/cart/:user", async (req, res) => {
    console.log(`GET /api/cart/${req.params.user} called`);
    const { user } = req.params

    const cart = await getCart(user) 

    res.json(cart)
})


/**
 * Endpoint to add an item to the user's cart.
 */
app.post("/api/cart/:user/items", async (req, res) => {
    console.log(`POST /api/cart/${req.params.user}/items called`);
    const { user } = req.params
    const { id } = req.body

    try {
        const response = await axios.get(`${PRODUCT_SERVICE}/api/products/${id}`)

        const cart = await addItemToCart(user, response.data)

        res.json(cart)
    } catch (error) {
        res.status(404).send("Product not found")
    }
})


/**
 * Endpoint to delete an item from the user's cart.
 */
app.delete("/api/cart/:user/items/:product", async (req, res) => {
    console.log(`DELETE /api/cart/${req.params.user}/items/${req.params.product} called`);
    const { user, product } = req.params

    try {

        const cart = await deleteItemFromCart(user, product)

        res.json(cart)
    } catch (error) {
        console.error("Error removing product from cart:", error.message)
        res.status(500).send("Internal server error")
    }
})


/**
 * Endpoint to add a discount to the user's cart.
 */
app.post("/api/cart/:user/discount", async (req, res) => {
    console.log(`POST /api/cart/${req.params.user}/discount called`);
    const { user } = req.params
    const { type, percentage } = req.body

    const cart = await addDiscountToCart(user, type, percentage)

    res.json(cart)
})


/**
 * Endpoint to update an existing order.
 */
app.put("/api/order/:orderId", async (req, res) => {
    console.log(`PUT /api/order/${req.params.orderId} called`);
    const { orderId } = req.params;
    const updatedOrderData = req.body;

    try {
        const updatedOrder = await editOrder(orderId, updatedOrderData);
        res.json(updatedOrder);
    } catch (error) {
        if (error.message === "Order not found") {
            return res.status(404).send(error.message);
        }
        console.error("Error updating order:", error.message);
        res.status(500).send("Internal server error");
    }
});

/**
 * Endpoint to retrieve all orders.
 */
app.get("/api/order", async (_req, res) => {
    console.log(`GET /api/order called`);

    const orders = await getOrders();

    res.json(orders);
});

/**
 * Endpoint to retrieve a specific order by its ID.
 */
app.get("/api/order/:orderId", async (req, res) => {
    console.log(`GET /api/order/${req.params.orderId} called`);
    const { orderId } = req.params;

    const order = await getOrder(orderId)

    if (!order) {
        return res.status(404).send("Order not found");
    }

    res.json(order);
});

/**
 * Endpoint to create an order from the user's cart.
 */
app.post("/api/cart/:user/order", async (req, res) => {
    console.log(`POST /api/cart/${req.params.user}/order called`);
    const { user } = req.params

    const cart = await getCart(user)

    if (cart.items.length === 0) {
        return res.status(400).send("Cart is empty. Cannot create order.");
    }

    try {
        const response = await axios.post(`${CHECKOUT_SERVICE}/api/checkout`, {
            products: cart.items,
            discount: {type: cart.DISCOUNT_TYPE, percentage: cart.DISCOUNT}
        })

        const order = response.data
        addOrder({...response.data, status: 'created', createdAt: new Date().toUTCString(), user: user})

        clearCart(user)

        res.json(order)

    } catch (error) {
        console.error("Error creating order:", error.message)
        res.status(500).send("Internal server error")
    }
})

app.listen(PORT, () => {
    console.log(`App listening on port ${PORT}`)
})

