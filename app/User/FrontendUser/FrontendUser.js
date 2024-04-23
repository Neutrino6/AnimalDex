const express = require('express'); 
const path = require('path');
const { Pool } = require('pg');

// Configura la connessione al database PostgreSQL
const pool = new Pool({
  user: 'postgres',
  host: 'centralserver-postgres-central-1', // Questo è il nome del servizio del container Docker PostgreSQL nel tuo Docker Compose
  database: 'CentralDB',
  password: 'postgres',
  port: 5433,
});

const app = express();

app.use(express.static(path.join(__dirname, './public')));


app.get('/', (req, res) => {
    res.sendFile(path.join(__dirname, './public/RegistrationUser.html'));
    console.log("Benvenuto!!");
});

app.use((req, res) => {
    res.status(404);
    res.sendFile(path.join(__dirname, './public/404page.html'));
    console.log("URL sbagliato!!");
});

app.listen(3000, () => {
    console.log("App listening on port 3000!!");
});

//PROBLEMA DELLA URL (NON RIESCE A GESTIRE IL PATH DI REINDIRIZZAMENTO)
app.get('/PersonalPage.html', (req, res) => {
    const userId = req.query.userId; // Supponiamo che l'ID utente sia passato come parametro query nella richiesta GET
    console.log(userId);
    res.setHeader('Content-Type', 'application/json');
    // Esegui una query per recuperare le informazioni dell'utente dal database
    pool.query('SELECT * FROM users WHERE user_id = $1', [userId], (err, result) => {
      if (err) {
        console.error('Errore nella query:', err);
        res.status(500).send('Errore nel recupero delle informazioni utente');
      } else {
        const user = result.rows[0]; // Supponiamo che l'utente sia il primo risultato della query
        // Fai qualcosa con le informazioni dell'utente, ad esempio restituisci una risposta JSON
        console.log(user);
        res.setHeader('Content-Type', 'application/json');
        res.json(user);
      }
    });
  });