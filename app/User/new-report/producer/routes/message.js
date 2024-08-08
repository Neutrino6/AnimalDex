const express = require('express');
const router = express.Router();
const amqp = require('amqplib/callback_api');

const url = 'amqp://guest:guest@rabbitmq:5672/';
const queue = 'my-queue';

let channel = null;

function connectRabbitMQ() {
  amqp.connect(url, (err, conn) => {
    if (err) {
      console.error("Failed to connect to RabbitMQ:", err);
      setTimeout(connectRabbitMQ, 5000); // Retry after 5 seconds
      return;
    }

    conn.createChannel((err, ch) => {
      if (err) {
        console.error("Failed to create a channel:", err);
        return;
      }
      channel = ch;
      console.log("RabbitMQ connection established and channel created.");
      
      // Ensure queue exists
      channel.assertQueue(queue, { durable: true });
    });

    conn.on('close', () => {
      console.log("RabbitMQ connection closed. Reconnecting...");
      setTimeout(connectRabbitMQ, 5000); // Retry after 5 seconds
    });
  });
}

// Start the connection process
connectRabbitMQ();

process.on('exit', (code) => {
  if (channel) {
    channel.close();
    console.log('Closing channel');
  }
});

router.post('/', (req, res) => {
  if (!channel) {
    return res.status(500).send('Channel is not available');
  }

  const { position, comment } = req.body;
  const date = new Date().toISOString(); // Generate current date in ISO format

  if (typeof position !== 'string' || typeof comment !== 'string') {
    return res.status(400).send('Invalid input format. Both position and comment must be strings.');
  }

  // Construct the structured message
  const message = {
    position,
    date,
    comment
  };

  // Send the message to RabbitMQ queue
  channel.sendToQueue(queue, Buffer.from(JSON.stringify(message)), { persistent: true });
  res.send(`Successfully sent message: ${JSON.stringify(message)}`);
});

module.exports = router;
