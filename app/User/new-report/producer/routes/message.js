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

  const { position, user_id,comment } = req.body;
  const date = new Date().toISOString(); // Generate current date in ISO format

  if (!user_id || !position || !date || !comment) {
    return res.status(400).send('Invalid input format. All fields must be provided.');
  }

  // Construct the structured message
  const message = {
    user_id: user_id,
    position: position,
    date: date,
    comment: comment
  };

  // Send the message to RabbitMQ queue with persistence
  channel.sendToQueue(queue, Buffer.from(JSON.stringify(message)), {
    persistent: true // Ensure the message is persistent
  });

  res.render('index', { response: `Successfully sent message: ${JSON.stringify(message)}` });
});

module.exports = router;
