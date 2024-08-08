import pika
import json

def callback(ch, method, properties, body):
    report = json.loads(body)
    print(f"Received report: {report}")

def consume_reports():
    connection = pika.BlockingConnection(pika.ConnectionParameters('rabbitmq'))
    channel = connection.channel()

    channel.queue_declare(queue='animal_reports')

    channel.basic_consume(queue='animal_reports',
                          on_message_callback=callback,
                          auto_ack=True)
    
    print('Waiting for messages.')
    channel.start_consuming()

consume_reports()
