from flask import Flask, request, jsonify
from typing import List, Dict, Union

app = Flask(__name__)

@app.route('/api/checkout', methods=['POST'])
def checkout():
    """
    Calculates price at checkout from list of products and discount codes
    
    Expected request body:
    {
        "products": [{"id": str, "price": float}],
        "discount": {"id": str, "type": str, "percentage": float}
    }
    """
    # Get JSON data from request
    data = request.get_json()
    
    # Extract products and discount
    products = data.get('products', [])
    discount = data.get('discount', {})
    
    # Calculate subtotal
    subtotal = sum(float(product['PRICE']) for product in products)
    total = subtotal
    
    # Apply discount if applicable
    if discount.get('type') == 'percentage':
        discount_percentage = float(discount.get('percentage', 0))
        total -= subtotal * discount_percentage
    
    # Return checkout details
    return jsonify({
        'products': products,
        'discount': discount,
        'subtotal': subtotal,
        'total': total
    })

# Configure port
if __name__ == '__main__':
    port = 3002
    app.run(host='0.0.0.0', port=port)
