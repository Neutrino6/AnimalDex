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
});

app.use((req, res) => {
    res.status(404);
    res.sendFile(path.join(__dirname, './public/404page.html'));
});

app.listen(3000, () => {
    console.log("App listening on port 3000!!");
});


//gestire get reindirizzamento logica server login/signup e collegarsi al database per riottenere le info sull'utente (anche nel caso in cui debbano essere cambiate)

app.get('/PersonalPage.html', (req, res) => {
    const userId = req.query.userId; // Supponiamo che l'ID utente sia passato come parametro query nella richiesta GET
  
    // Esegui una query per recuperare le informazioni dell'utente dal database
    pool.query('SELECT * FROM users WHERE user_id = $1', [userId], (err, result) => {
      if (err) {
        console.error('Errore nella query:', err);
        res.status(500).send('Errore nel recupero delle informazioni utente');
      } else {
        const user = result.rows[0]; // Supponiamo che l'utente sia il primo risultato della query
        // Fai qualcosa con le informazioni dell'utente, ad esempio restituisci una risposta JSON
        res.json(user);


        //-------------RISOLVERE PROBLEMA APPARIZIONE INFO NELLE AREE DELLA PAGINA PERSONALE E GESTIRE SESSIONE----------
      }
    });
  });
