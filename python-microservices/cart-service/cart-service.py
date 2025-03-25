import os
from flask import Flask, request, jsonify, json
import requests
import hdbcli.dbapi as hana

# Environment and configuration
PORT = int(os.environ.get('PORT', 3001))
PRODUCT_SERVICE = "https://cpp-product.cfapps.us10-001.hana.ondemand.com"
CHECKOUT_SERVICE = "https://cpp-checkout.cfapps.us10-001.hana.ondemand.com"

app = Flask(__name__)

# Database connection parameters
CONN_PARAMS = {
    'address': '8bc93cde-866b-48eb-ae6f-f9ee37acb91e.hana.trial-us10.hanacloud.ondemand.com',
    'port': 443,
    'user': 'DBADMIN',
    'password': '7605B1zEe'
}

def rows_to_dict(cursor, rows):
    """
    Convert database rows to a list of dictionaries.
    
    :param cursor: Database cursor
    :param rows: Rows to convert
    :return: List of dictionaries
    """
    # Get column names from cursor description
    columns = [column[0] for column in cursor.description]
    
    # Convert rows to list of dictionaries
    return [dict(zip(columns, row)) for row in rows]

def get_hana_connection():
    """Create and return a HANA database connection."""
    return hana.connect(**CONN_PARAMS)

def promise_exec_hana(statement, params=None, expect_result=True):
    """
    Execute a statement on the HANA database and return results.
    
    :param statement: SQL statement to execute
    :param params: Optional parameters for parameterized queries
    :param expect_result: Whether to expect and return results
    :return: Query results as list of dictionaries or None
    """
    conn = None
    try:
        conn = get_hana_connection()
        cursor = conn.cursor()

        if params:
            cursor.execute(statement, params)
        else:
            cursor.execute(statement)
        
        # Commit for modification statements
        if not statement.lower().startswith('select'):
            conn.commit()
        
        # Only fetch results if expected
        if expect_result:
            # Convert rows to dictionaries
            results = rows_to_dict(cursor, cursor.fetchall())
            cursor.close()
            return results
        
        cursor.close()
        return None
    except Exception as e:
        app.logger.error(f"Database error: {e}")
        if conn:
            conn.rollback()
        raise
    finally:
        if conn:
            conn.close()

def execute_hana_transaction(statements_with_params):
    """
    Execute multiple statements in a single transaction.
    
    :param statements_with_params: List of tuples (statement, params)
    :return: None
    """
    conn = None
    try:
        conn = get_hana_connection()
        cursor = conn.cursor()
        
        for statement, params in statements_with_params:
            if params:
                cursor.execute(statement, params)
            else:
                cursor.execute(statement)
        
        conn.commit()
        cursor.close()
    except Exception as e:
        if conn:
            conn.rollback()
        app.logger.error(f"Transaction error: {e}")
        raise
    finally:
        if conn:
            conn.close()

def empty_cart(user):
    """Create an empty cart for a user."""
    return {'id': user, 'items': [], 'discount': {}}

def get_cart(user):
    """
    Retrieve the cart for a specific user.
    
    :param user: User identifier
    :return: User's cart
    """
    try:
        # Get discount information
        discount_query = "SELECT * FROM cart WHERE USER_ID = ?"
        discount_res = promise_exec_hana(discount_query, (user,))
        
        if not discount_res:
            # Create a new cart if it doesn't exist
            promise_exec_hana("INSERT INTO cart VALUES (?, NULL, NULL)", (user,), expect_result=False)
            return empty_cart(user)
        
        # Get cart items
        items_query = "SELECT * FROM cart_items WHERE USER_ID = ?"
        items_res = promise_exec_hana(items_query, (user,))
        
        # Combine discount and items
        cart = {**discount_res[0], 'items': items_res}
        return cart
    except Exception as e:
        app.logger.error(f"Error retrieving cart: {e}")
        # Return an empty cart if there's an error
        return empty_cart(user)

def clear_cart(user):
    """
    Clear the cart for a specific user.
    
    :param user: User identifier
    :return: Empty cart
    """
    # Execute multiple statements in a transaction
    execute_hana_transaction([
        ("UPDATE cart SET DISCOUNT = NULL, DISCOUNT_TYPE = NULL WHERE USER_ID = ?", (user,)),
        ("DELETE FROM cart_items WHERE USER_ID = ?", (user,))
    ])
    
    return get_cart(user)

def add_item_to_cart(user, item):
    """
    Add an item to the user's cart.
    
    :param user: User identifier
    :param item: Item to add
    :return: Updated cart
    """
    cart = get_cart(user)
    
    # Check if item already exists
    if any(cart_item['PRODUCT_ID'] == item['id'] for cart_item in cart['items']):
        return cart
    
    # Insert new item
    promise_exec_hana(
        "INSERT INTO cart_items VALUES (?, ?, ?, ?)", 
        (user, item['id'], item['price'], item['name'].replace("'", "")),
        False
    )
    
    return get_cart(user)

def delete_item_from_cart(user, item):
    """
    Delete an item from the user's cart.
    
    :param user: User identifier
    :param item: Item identifier to delete
    :return: Updated cart
    """
    # Delete item and return cart, even if no rows were affected
    promise_exec_hana(
        "DELETE FROM cart_items WHERE USER_ID = ? AND PRODUCT_ID = ?", 
        (user, item), 
        expect_result=False
    )
    return get_cart(user)

def add_discount_to_cart(user, discount_type, discount_percentage):
    """
    Add a discount to the user's cart.
    
    :param user: User identifier
    :param discount_type: Type of discount
    :param discount_percentage: Discount percentage
    :return: Updated cart
    """
    # Ensure cart exists
    discount_query = "SELECT * FROM cart WHERE USER_ID = ?"
    discount_res = promise_exec_hana(discount_query, (user,))
    
    if not discount_res:
        promise_exec_hana("INSERT INTO cart VALUES (?, NULL, NULL)", (user,), False)
        return empty_cart(user)
    
    # Update discount
    promise_exec_hana(
        "UPDATE cart SET DISCOUNT = ?, DISCOUNT_TYPE = ? WHERE USER_ID = ?", 
        (discount_percentage, discount_type, user),
        False
    )
    
    return get_cart(user)

def get_orders():
    """
    Retrieve all orders from the database.
    
    :return: List of all orders
    """
    try:
        # Fetch all orders
        orders_query = "SELECT * FROM orders ORDER BY CREATED_AT DESC"
        orders = promise_exec_hana(orders_query)
        
        return orders
    except Exception as e:
        app.logger.error(f"Error retrieving orders: {e}")
        raise


def add_order(order):
    """
    Add an order to the orders table.
    
    :param order: Order details
    :return: Added order
    """
    conn = None
    try:
        conn = get_hana_connection()
        cursor = conn.cursor()
        
        # Insert order
        cursor.execute(
            "INSERT INTO orders(USER_ID, DISCOUNT_TYPE, DISCOUNT, SUBTOTAL, TOTAL, STATUS) VALUES (?, ?, ?, ?, ?, ?)",
            (
                order['user'], 
                order['discount']['type'], 
                order['discount']['percentage'] or 0, 
                order['subtotal'], 
                order['total'], 
                order['status']
            )
        )
        
        # Get the last inserted order
        cursor.execute("SELECT * FROM orders WHERE USER_ID = ? ORDER BY CREATED_AT DESC LIMIT 1", (order['user'],))
        last_order = rows_to_dict(cursor, cursor.fetchall())[0]
        
        # Insert order items
        for product in order['products']:
            cursor.execute(
                "INSERT INTO order_items VALUES (?, ?, ?)", 
                (last_order['ORDER_ID'], product['PRODUCT_ID'], product['PRICE'])
            )
        
        conn.commit()
        cursor.close()
    except Exception as e:
        if conn:
            conn.rollback()
        app.logger.error(f"Order creation error: {e}")
        raise
    finally:
        if conn:
            conn.close()
    
    return order

def get_order(order_id):
    """
    Retrieve a specific order by ID.
    
    :param order_id: Order identifier
    :return: Order details or None
    """
    conn = None
    try:
        conn = get_hana_connection()
        cursor = conn.cursor()
        
        # Get order details
        cursor.execute("SELECT * FROM orders WHERE ORDER_ID = ?", (order_id,))
        orders = rows_to_dict(cursor, cursor.fetchall())
        
        if not orders:
            return None
        
        # Get order items
        cursor.execute("SELECT * FROM order_items WHERE ORDER_ID = ?", (order_id,))
        order_items = rows_to_dict(cursor, cursor.fetchall())
        
        # Combine order details with items
        order = orders[0]
        order['items'] = order_items
        
        return order
    except Exception as e:
        app.logger.error(f"Error retrieving order: {e}")
        raise
    finally:
        if conn:
            conn.close()

def edit_order(order_id, updated_order_data):
    """
    Edit an existing order.
    
    :param order_id: Order identifier
    :param updated_order_data: New order data
    :return: Updated order
    """
    order_res = promise_exec_hana("SELECT * FROM orders WHERE ORDER_ID = ?", (order_id,))
    
    if not order_res:
        raise ValueError("Order not found")
    
    # Prepare updated order data
    current_order = dict(zip(['ORDER_ID', 'USER_ID', 'DISCOUNT_TYPE', 'DISCOUNT', 'SUBTOTAL', 'TOTAL', 'STATUS', 'CREATED_AT'], order_res[0]))
    current_order.update(updated_order_data)
    
    # Update order
    promise_exec_hana(
        "UPDATE orders SET DISCOUNT_TYPE = ?, DISCOUNT = ?, SUBTOTAL = ?, TOTAL = ?, STATUS = ? WHERE ORDER_ID = ?",
        (
            current_order['DISCOUNT_TYPE'], 
            current_order['DISCOUNT'], 
            current_order['SUBTOTAL'], 
            current_order['TOTAL'], 
            current_order['STATUS'], 
            order_id
        )
    )
    
    return current_order

# Flask Routes
@app.route('/api/cart/<int:user>', methods=['GET'])
def get_user_cart(user):
    """Retrieve cart for a specific user."""
    cart = get_cart(user)
    return jsonify(cart)

@app.route('/api/cart/<int:user>/items', methods=['POST'])
def add_cart_item(user):
    """Add an item to the user's cart."""
    item_id = request.json.get('id')
    
    try:
        response = requests.get(f"{PRODUCT_SERVICE}/api/products/{item_id}")
        response.raise_for_status()
        
        cart = add_item_to_cart(user, response.json())
        return jsonify(cart)
    except requests.RequestException:
        return "Product not found", 404

@app.route('/api/cart/<int:user>/items/<int:product>', methods=['DELETE'])
def remove_cart_item(user, product):
    """Remove an item from the user's cart."""
    try:
        cart = delete_item_from_cart(user, product)
        return jsonify(cart)
    except Exception as e:
        app.logger.error(f"Error removing product from cart: {e}")
        return jsonify(get_cart(user))  # Return current cart state

@app.route('/api/cart/<int:user>/discount', methods=['POST'])
def add_cart_discount(user):
    """Add a discount to the user's cart."""
    discount_type = request.json.get('type')
    discount_percentage = request.json.get('percentage')
    
    cart = add_discount_to_cart(user, discount_type, discount_percentage)
    return jsonify(cart)

@app.route('/api/order/<int:order_id>', methods=['PUT'])
def update_order(order_id):
    """Update an existing order."""
    try:
        updated_order = edit_order(order_id, request.json)
        return jsonify(updated_order)
    except ValueError:
        return "Order not found", 404
    except Exception as e:
        app.logger.error(f"Error updating order: {e}")
        return "Internal server error", 500

@app.route('/api/order', methods=['GET'])
def list_orders():
    """Retrieve all orders."""
    orders = get_orders()
    return jsonify(orders)

@app.route('/api/order/<int:order_id>', methods=['GET'])
def get_specific_order(order_id):
    """Retrieve a specific order."""
    order = get_order(order_id)
    
    if not order:
        return "Order not found", 404
    
    return jsonify(order)

@app.route('/api/cart/<int:user>/order', methods=['POST'])
def create_order(user):
    """Create an order from the user's cart."""
    cart = get_cart(user)
    
    if not cart['items']:
        return "Cart is empty. Cannot create order.", 400
    
    try:
        # Call checkout service

        jsonstring = json.dumps({
                'products': cart['items'],
                'discount': {
                    'type': cart.get('DISCOUNT_TYPE'), 
                    'percentage': cart.get('DISCOUNT')
                }})

        checkout_response = requests.post(
            f"{CHECKOUT_SERVICE}/api/checkout", 
            data=jsonstring,
            headers={
                'Content-type': 'application/json',
            }
        )
        checkout_response.raise_for_status()
        
        order = checkout_response.json()
        
        # Add order and clear cart
        add_order({
            **order, 
            'status': 'created', 
            'user': user
        })
        
        clear_cart(user)
        
        return jsonify(order)
    
    except requests.RequestException as e:
        app.logger.error(f"Error creating order: {e}")
        return "Internal server error", 500

if __name__ == '__main__':
    app.run(port=PORT, debug=True)
