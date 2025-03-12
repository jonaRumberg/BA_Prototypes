from flask import Flask, request, jsonify
import requests

app = Flask(__name__)

NORTHWIND_BASE_URL = "https://services.odata.org/V2/Northwind/Northwind.svc/Products"

@app.route("/api/products", methods=['GET'])
def get_products():
    # Get query parameter
    q = request.args.get('q', '')
    
    # Fetch products from Northwind API
    response = requests.get(f"{NORTHWIND_BASE_URL}?%24format=json")
    
    # Extract and transform products
    products = [
        {
            "id": product['ProductID'], 
            "name": product['ProductName'], 
            "price": product['UnitPrice']
        } 
        for product in response.json()['d']['results']
    ]
    
    # Filter products if query is provided
    if q:
        products = [
            product for product in products 
            if q.upper() in product['name'].upper()
        ]
    
    return jsonify(products)

@app.route("/api/products/<int:id>", methods=['GET'])
def get_product_by_id(id):
    try:
        # Fetch specific product
        response = requests.get(f"{NORTHWIND_BASE_URL}({id})?%24format=json")
        
        # Check if product exists
        product_data = response.json()['d']
        
        product = {
            "id": product_data['ProductID'],
            "name": product_data['ProductName'],
            "price": product_data['UnitPrice']
        }
        
        return jsonify(product)
    
    except Exception as e:
        return jsonify({"error": "Product not found"}), 404

# Run the application
if __name__ == '__main__':
    PORT = int(os.getenv("PORT", 8080))
    app.run(host='0.0.0.0', port=PORT, debug=True)
