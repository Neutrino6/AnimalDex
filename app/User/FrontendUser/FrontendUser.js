const express = require('express'); 
const path = require('path');
const axios = require('axios'); // Per effettuare richieste HTTP
/*const { Pool } = require('pg');

// Configura la connessione al database PostgreSQL
const pool = new Pool({
  user: 'postgres',
  host: 'centralserver-postgres-central-1', // Questo è il nome del servizio del container Docker PostgreSQL nel tuo Docker Compose
  database: 'CentralDB',
  password: 'postgres',
  port: 5433,
});
*/

const app = express();

app.use(express.static(path.join(__dirname, './public')));


app.get('/', (req, res) => {
    res.sendFile(path.join(__dirname, './public/RegistrationUser.html'));
    console.log("Benvenuto!!");
});


app.get('/PersonalPageUser/:userId', async (req, res) => {
  const userId = req.params.userId;
  console.log(userId);

  try {
    // Effettua la richiesta al servizio esterno
    const response = await axios.get("http://host.docker.internal:6039/PersonalPageUser?user_id="+ userId);

    // Ricevuta la risposta, puoi manipolarla come desideri
    const userData = response.data; // Supponendo che la risposta contenga i dati dell'utente
    console.log(userData);

    // Concatena ogni valore della userData alla URL di reindirizzamento
    let redirectURL = "http://localhost:3000/PersonalPage.html?";
    for (const [key, value] of Object.entries(userData)) {
      redirectURL += `${key}=${value}&`;
    }

    // Rimuovi l'ultimo carattere '&' dalla URL
    redirectURL = redirectURL.slice(0, -1);

    // Effettua il reindirizzamento alla URL composta
    res.redirect(redirectURL);
  } catch (error) {
    // Gestione degli errori nel caso in cui la richiesta fallisca
    console.error('Errore durante la richiesta al servizio:', error.message);
    res.status(500).send('Errore durante la richiesta al servizio esterno');
  }
});

app.get('/PersonalPageOperator/:opCode', async (req, res) => {
  const code = req.params.opCode;
  console.log(code);

  try {
    // Effettua la richiesta al servizio esterno
    const response = await axios.get("http://host.docker.internal:6039/PersonalPageOperator?code="+ code);

    // Ricevuta la risposta, puoi manipolarla come desideri
    const operData = response.data; // Supponendo che la risposta contenga i dati dell'utente
    console.log(operData);

    // Concatena ogni valore della userData alla URL di reindirizzamento
    let redirectURL = "http://localhost:3000/PersonalPageOperator.html?";
    for (const [key, value] of Object.entries(operData)) {
      redirectURL += `${key}=${value}&`;
    }

    // Rimuovi l'ultimo carattere '&' dalla URL
    redirectURL = redirectURL.slice(0, -1);

    // Effettua il reindirizzamento alla URL composta
    res.redirect(redirectURL);
  } catch (error) {
    // Gestione degli errori nel caso in cui la richiesta fallisca
    console.error('Errore durante la richiesta al servizio:', error.message);
    res.status(500).send('Errore durante la richiesta al servizio esterno');
  }
});

/*così funziona, nel dubbio la lascerei così*/
// Route per le richieste non gestite
app.use((req, res, next) => {
  // Controlla se la richiesta corrisponde a una route delle risorse statiche
  if (req.path.startsWith('/')) {
      // La richiesta corrisponde a una route delle risorse statiche, passa alla route successiva
      next();
  } else {
      // La richiesta non corrisponde a nessuna delle route definite, invia la risposta 404
      res.status(404);
      res.sendFile(path.join(__dirname, './public/404page.html'));
      console.log("URL sbagliato!!");
  }
});




/* ORIGINALE ALE
app.use((req, res) => {
    res.status(404);
    res.sendFile(path.join(__dirname, './public/404page.html'));
    console.log("URL sbagliato!!");
}); */

app.listen(3000, () => {
    console.log("App listening on port 3000!!");
});