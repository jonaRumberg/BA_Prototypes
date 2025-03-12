const express = require("express")
const bodyParser = require("body-parser")
const axios = require("axios")

const app = express()
const PORT = process.env.PORT || 3000

app.use(bodyParser.json())

//Connect to northwind dummy api on
const NORTWIND_BASE_URL = "https://services.odata.org/V2/Northwind/Northwind.svc/Products"

app.get("/api/products", async (req, res) => {
    const {q} = req.query
    
    const productsResp = await axios.get(`${NORTWIND_BASE_URL}?%24format=json`)
    let products = productsResp.data.d.results.map(response => {return {"id": response.ProductID, "name": response.ProductName, "price": response.UnitPrice}})

    //if a query param is given, filter
    if(q){
        products = products.filter(product => product.name.toUpperCase().includes(q.toUpperCase()))
    }

    res.json(products)
})

app.get("/api/products/:id", async (req, res) => {
    const { id } = req.params
    
    try {
        const productsResp = await axios.get(`${NORTWIND_BASE_URL}(${id})?%24format=json`)

        const productData = productsResp?.data?.d
        if (!productData) {
            return res.status(404).json({ error: "Product not found" })
        }

        const product = { "id": productData.ProductID, "name": productData.ProductName, "price": productData.UnitPrice }

        res.json(product)

    } catch (error) {
        res.status(404).send("Product not found")
    }
})

app.listen(PORT, () => {
    console.log(`App listening on port ${PORT}`)
})
