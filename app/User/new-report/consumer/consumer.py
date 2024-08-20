import pika
import time
import requests
import json
from flask import Flask, jsonify, request
from flask_cors import CORS

app = Flask(__name__)
CORS(app)

host = 'rabbitmq'
queue = 'my-queue'

# Store unacknowledged messages temporarily
unack_messages = {}  # Declare this variable globally

def connect_to_rabbitmq():
    """Try to establish a connection to RabbitMQ with retry logic."""
    connection_params = pika.ConnectionParameters(host=host)
    while True:
        try:
            connection = pika.BlockingConnection(connection_params)
            channel = connection.channel()
            channel.queue_declare(queue=queue, durable=True)
            print("RabbitMQ connection established and channel created.")
            return connection, channel
        except pika.exceptions.AMQPConnectionError as e:
            print(f"Failed to connect to RabbitMQ: {e}. Retrying in 5 seconds...")
            time.sleep(5)  # Retry after 5 seconds

def consume_message():
    connection, channel = connect_to_rabbitmq()

    method_frame, header_frame, body = channel.basic_get(queue=queue)
    if method_frame:
        message = body.decode('UTF-8')
        delivery_tag = method_frame.delivery_tag
        unack_messages[delivery_tag] = (message, channel)  # Save the unacknowledged message here
        return jsonify({"status": "success", "message": message, "delivery_tag": delivery_tag})
    else:
        print("No messages in queue.")
        return jsonify({"status": "empty", "message": "No messages in queue."})

@app.route('/consume', methods=['GET'])
def consume_route():
    return consume_message()

@app.route('/acknowledge', methods=['POST'])
def acknowledge_message():
    global unack_messages  # Ensure we're using the global variable
    
    data = request.json
    delivery_tag = data.get('delivery_tag')
    response_message = data.get('response_message')
    operator_id = data.get('operator_id')

    if delivery_tag in unack_messages:
        message, channel = unack_messages.pop(delivery_tag)
        channel.basic_ack(delivery_tag=delivery_tag)
        
        # Log the acknowledgment
        print(f"Message acknowledged: {message}")
        print(f"Response: {response_message}")
        
        # Parse the original message to extract necessary information
        try:
            message_data = json.loads(message)
            u_id = message_data.get('user_id')
            o_id = operator_id  # Assume you have a way to determine or get this ID
            writer = "operator"  # Set the writer, e.g., the current operator
            text = response_message
        except json.JSONDecodeError as e:
            return jsonify({"status": "error", "message": "Failed to parse original message."}), 400
        
        # Construct the payload to send to the sendMessage service
        payload = {
            'u_id': u_id,
            'o_id': o_id,
            'writer': writer,
            'text': text
        }
        
        # Send the payload to the sendMessage service
        try:
            send_message_url = "http://host.docker.internal:6039/sendMessage"  # Adjust the URL to your service's address
            response = requests.post(send_message_url, json=payload)  # Use `json=payload` to send data as JSON
            if response.status_code == 201:
                return jsonify({"status": "success", "message": "Message acknowledged and response sent."})
            else:
                return jsonify({"status": "error", "message": "Failed to send response message to the service."}), 500
        except requests.exceptions.RequestException as e:
            return jsonify({"status": "error", "message": f"Failed to communicate with the sendMessage service: {str(e)}"}), 500
    else:
        return jsonify({"status": "error", "message": "Delivery tag not found."}), 404

if __name__ == '__main__':
    app.run(host='0.0.0.0', port=3002)

