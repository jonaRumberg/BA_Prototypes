const express = require("express");
const bodyParser = require("body-parser");

const app = express()
const port = process.env.PORT || 3002

app.use(bodyParser.json())

/** Calculates price at checkout from list of products and discount codes
 *  products should be list of products with the shape [{id, price}]
 *  discounts should be the shape {id, type, percentage}
 */
app.post('/api/checkout', (req, res) => {

    const {products, discount} = req.body

    //sum up price of product list
    const subtotal = products.reduce((i, product) => i + Number(product.PRICE), 0)
    let total = subtotal;
    
    //discounts subtotal if appropriate discount is available
    if(discount.type === 'percentage'){
        total -= subtotal * Number(discount.percentage);
    }

    res.json({
        products: products, 
        discount: discount, 
        subtotal: subtotal, 
        total: total
    })
})

app.listen(port, ()=>{
    console.log(`Checkout Service is running on port ${port}`)
})
