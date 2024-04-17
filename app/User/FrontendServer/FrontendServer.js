const express = require('express'); 
const path = require('path');

const app = express();

app.get('/', (req, res) => {
    res.sendFile(path.join(__dirname, './public/RegistrationUser.html'));
});

app.use(express.static(path.join(__dirname, './public')));

app.use((req, res) => {
    res.status(404);
    res.sendFile(path.join(__dirname, './public/404page.html'));
});

app.listen(3000, () => {
    console.log("App listening on port 3000!!");
});


/*
const http = require('http');
const fs = require('fs');
const path = require('path');
const { Pool } = require('pg');
const { randomInt } = require('crypto');

const pool = new Pool({
    user: 'postgres',
    host: process.env.PGHOST,
    database: 'CentralDB',
    password: 'postgres',
    port: 5433,
});

const server = http.createServer((req, res) => {
    if (req.method === 'GET' && req.url === '/') {
        fs.readFile(path.join(__dirname, 'index.html'), (err, data) => {
            if (err) {
                res.writeHead(500, { 'Content-Type': 'text/plain' });
                res.end('Internal Server Error');
            } else {
                res.writeHead(200, { 'Content-Type': 'text/html' });
                res.end(data);
            }
        });
    } else if (req.method === 'POST' && req.url === '/Registration') {
        let body = '';
        req.on('data', chunk => {
            body += chunk.toString();
    });

        req.on('end', () => {
            const userData = JSON.parse(body);
            const {user_id=randomInt(10000000000), email, password, username, firstname, surname, points=0, birthday, fav_animal, forum_notify=false, emerg_notify=false, userImage=0, admin=false} = userData;

            pool.query('INSERT INTO users (user_id, email, passw, username, firstname, surname, points, birthday, fav_animal, forum_notify, emergency_notify, userImage, administrator) VALUES ($1, $2, $3, $4, $5, $6, $7, $8, $9, $10, $11, $12, $13)', [user_id, email, password, username, firstname, surname, points, birthday, fav_animal, forum_notify, emerg_notify, userImage, admin], (err, result) => {
                if (err) {
                    console.error('Error executing query', err);
                    res.writeHead(500, { 'Content-Type': 'application/json' });
                    res.end(JSON.stringify({ error: 'Error signing up user' }));
                } else {
                    console.log('User signed up successfully');
                    res.writeHead(200, { 'Content-Type': 'application/json' });
                    res.end(JSON.stringify({ message: 'Sign up successful!' }));
                }
            });
        });
    } else {
        res.writeHead(404, { 'Content-Type': 'text/plain' });
        res.end('Not Found');
    }
});

const PORT = process.env.PORT || 3000;
server.listen(PORT, () => {
    console.log(`Server is running on port ${PORT}`);
});
*/