class CheckoutService extends cds.ApplicationService {
    init() {
        this.on("checkout", async req => {
            const {products, discount} = req.data

            //sum up price of product list
            const subtotal = products.reduce((i, product) => i + Number(product.PRICE), 0)
            let total = subtotal;
            
            //discounts subtotal if appropriate discount is available
            if(discount.type === 'percentage'){
                total -= subtotal * Number(discount.percentage);
            }
        
            return({
                products: products, 
                discount: discount, 
                subtotal: subtotal, 
                total: total
            })
        
        })

        return super.init();
    }
}

module.exports = CheckoutService;