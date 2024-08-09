import pika
import time
from flask import Flask, jsonify
from flask_cors import CORS

app = Flask(__name__)
CORS(app)

host = 'rabbitmq'
queue = 'my-queue'

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

    while True:
        try:
            method_frame, header_frame, body = channel.basic_get(queue=queue)

            if method_frame:
                message = body.decode('UTF-8')
                print("Received:", message)
                
                channel.basic_ack(delivery_tag=method_frame.delivery_tag)
                return jsonify({"status": "success", "message": message})
            else:
                print("No messages in queue.")
                return jsonify({"status": "empty", "message": "No messages in queue."})

        except pika.exceptions.StreamLostError as e:
            print(f"Stream lost, reconnecting to RabbitMQ: {e}")
            connection, channel = connect_to_rabbitmq()

@app.route('/consume', methods=['GET'])
def consume_route():
    return consume_message()

if __name__ == '__main__':
    app.run(host='0.0.0.0', port=3002)

