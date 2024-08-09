const express = require('express'); 
const fs = require('fs');
const sha256 = require('crypto-js/sha256');
const cookieParser = require('cookie-parser');
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
app.use(cookieParser());

app.use(express.static(path.join(__dirname, './public')));


app.get('/', (req, res) => {
    res.sendFile(path.join(__dirname, './public/RegistrationUser.html'));
    console.log("Benvenuto!!");
});


app.get('/Redirect/:userId', async (req, res) => {
  const userId = req.params.userId;
  res.redirect('/PersonalPageUser/'+userId);
});

app.get('/PersonalPageUser/:userId', async (req, res) => {
  const userId = req.params.userId;
  console.log(userId);

  if (req.cookies && req.cookies.authCookie) {
    const authCookie = req.cookies.authCookie;
    if (!authCookie || !isValidAuthCookie(authCookie,userId)) {
      return res.redirect('http://localhost:3000/LoginUser.html');
    }
  }
  else{
    return res.redirect('http://localhost:3000/LoginUser.html');
  }

  try {
    // Effettua la richiesta al servizio esterno
    const response = await axios.get("http://host.docker.internal:6039/PersonalPageUser?user_id="+ userId);
    const response1 = await axios.get("http://host.docker.internal:6040/checkWinner?userId="+ userId);

    // Ricevuta la risposta, puoi manipolarla come desideri
    const userData = response.data; // Supponendo che la risposta contenga i dati dell'utente
    const winner = (response1.data === "User " + userId + " is a winner");
    console.log(userData);

    // Set cookie if admin is true
    if (userData.admin === true) {
      const adminCookieValue = sha256("ADMIN:" + userId).toString();
      res.cookie('admin', adminCookieValue, {
        domain: 'localhost',
        path: '/',
        httpOnly: true,
        maxAge: 24 * 60 * 60 * 1000 // Cookie valid for 1 day
      });
      console.log('Admin cookie set');
    }

    /*// Costruisci l'URL con i dati dell'utente come parametri query
    let redirectURL = `?userId=${userId}`;
    for (const [key, value] of Object.entries(userData)) {
      redirectURL += `&${key}=${value}`;
    }*/

    const filePath = path.join(__dirname, './public/PersonalPage.html');

    // Effettua il reindirizzamento alla URL composta
    fs.readFile(filePath, 'utf8', (err, data) => {
      if (err) {
        console.error('Errore durante la lettura del file HTML:', err);
        return res.status(500).send('Errore durante la lettura del file HTML');
      }
      
      // Sostituisci <<userData>> con i dati utente
      let modifiedHTML = data.replace('<<userData>>', JSON.stringify(userData));
      modifiedHTML = modifiedHTML.replace('<<winner>>', JSON.stringify(winner));

      // Invia il contenuto modificato come risposta
      res.send(modifiedHTML);
    });
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
    /*
    // Concatena ogni valore della userData alla URL di reindirizzamento
    let redirectURL = "http://localhost:3000/PersonalPageOperator.html?";
    for (const [key, value] of Object.entries(operData)) {
      redirectURL += `${key}=${value}&`;
    }

    // Rimuovi l'ultimo carattere '&' dalla URL
    redirectURL = redirectURL.slice(0, -1);

    // Effettua il reindirizzamento alla URL composta
    res.redirect(redirectURL);
    */
    const filePath = path.join(__dirname, './public/PersonalPageOperator.html');

    // Effettua il reindirizzamento alla URL composta
    fs.readFile(filePath, 'utf8', (err, data) => {
      if (err) {
        console.error('Errore durante la lettura del file HTML:', err);
        return res.status(500).send('Errore durante la lettura del file HTML');
      }
      
      // Sostituisci <<userData>> con i dati utente
      const modifiedHTML = data.replace('<<operData>>', JSON.stringify(operData));
      
      // Invia il contenuto modificato come risposta
      res.send(modifiedHTML);
    });
  } catch (error) {
    // Gestione degli errori nel caso in cui la richiesta fallisca
    console.error('Errore durante la richiesta al servizio:', error.message);
    res.status(500).send('Errore durante la richiesta al servizio esterno');
  }
});

/*  app.delete('/deleteAccount/:userId', async (req, res) => {
  const userId = req.params.userId;
  
  try {
    // Esegui la logica per eliminare l'account dal tuo archivio di dati
    // Supponiamo che tu abbia una funzione deleteUserFromDatabase che accetta userId come parametro e elimina l'account corrispondente dal database
    // await deleteUserFromDatabase(userId);
    await axios.get("http://host.docker.internal:6039/DeleteAccountUser?user_id="+ userId);
    // Rispondi con un messaggio di successo dopo l'eliminazione
    res.status(200).send("Account eliminato con successo.");
  } catch (error) {
    console.error('Errore durante l\'eliminazione dell\'account:', error.message);
    res.status(500).send('Errore durante l\'eliminazione dell\'account.');
  }
});  */

app.get('/:userId/sendAlarm', async (req, res) => {
  const userId = req.params.userId;
  console.log(userId);

  if (req.cookies && req.cookies.authCookie) {
    const authCookie = req.cookies.authCookie;
    if (!authCookie || !isValidAuthCookie(authCookie,userId)) {
      return res.redirect('http://localhost:3000/LoginUser.html');
    }
  }
  else{
    return res.redirect('http://localhost:3000/LoginUser.html');
  }
  try {
    res.sendFile(path.join(__dirname, './public/AnimalReportEmergency.html'));
  } catch (error) {
    // Gestione degli errori nel caso in cui la richiesta fallisca
    console.error('Errore durante la richiesta al servizio:', error.message);
    res.status(500).send('Errore durante la richiesta al servizio esterno');
  }
});

app.get('/PersonalPageUser/PersonalPageMap/:userId', async (req, res) => {
  const userId = req.params.userId;
  
  //question-mark as default if no image found
  const defaultImagePath = path.join(__dirname, './public/images/question-mark.jpg');
    const getDefaultImageBase64 = () => {
      try {
        const imageBuffer = fs.readFileSync(defaultImagePath);
        return imageBuffer.toString('base64');
      } catch (err) {
        console.error('Errore durante la lettura dell\'immagine di default:', err);
        return '';
      }
    };
    const BackgroundImagePath = path.join(__dirname, './public/images/italyMap.jpg');
    const getBackgroundImageBase64 = () => {
      try {
        const imageBuffer = fs.readFileSync(BackgroundImagePath);
        return imageBuffer.toString('base64');
      } catch (err) {
        console.error('Errore durante la lettura dell\'immagine di default:', err);
        return '';
      }
    };
  try {
    // ask for images to mapController
    const response = await axios.get(`http://host.docker.internal:7777/${userId}/map`);
    const images = response.data;
    let { north, center, south, islands } = images;

    north = north || getDefaultImageBase64();
    center = center || getDefaultImageBase64();
    south = south || getDefaultImageBase64();
    islands = islands || getDefaultImageBase64();

    const background = getBackgroundImageBase64();
    const filePath = path.join(__dirname, './public/Map.html');  //html page

    fs.readFile(filePath, 'utf8', (err, data) => {
      if (err) {
        console.error('Errore durante la lettura del file HTML:', err);
        return res.status(500).send('Errore durante la lettura del file HTML');
      }

      let modifiedHTML = data
        .replace('<<northImage>>', `data:image/jpeg;base64,${north}`)
        .replace('<<centerImage>>', `data:image/jpeg;base64,${center}`)
        .replace('<<southImage>>', `data:image/jpeg;base64,${south}`)
        .replace('<<islandsImage>>', `data:image/jpeg;base64,${islands}`)
        .replace('<<background>>', `data:image/jpeg;base64,${background}`)
        .replace('<<userid>>', JSON.stringify(userId));


      // send images to html page
      res.send(modifiedHTML);
    });
  } catch (error) {
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


function isValidAuthCookie(cookieValue, userId) {
  // Calcola l'hash SHA-256 della stringa fissa concatenata con l'ID dell'utente
  const expectedLoginCookieValue = sha256("LOGIN:" + userId).toString();
  const expectedOauthCookieValue = sha256("GOOGLE_OAUTH:" + userId).toString();

  // Confronta il valore del cookie con il valore atteso
  console.log(cookieValue);
  console.log(expectedLoginCookieValue);
  console.log(expectedOauthCookieValue);
  console.log(cookieValue === expectedLoginCookieValue);
  console.log(cookieValue === expectedOauthCookieValue);
  return cookieValue === expectedLoginCookieValue || cookieValue === expectedOauthCookieValue;
}

/* ORIGINALE ALE
app.use((req, res) => {
    res.status(404);
    res.sendFile(path.join(__dirname, './public/404page.html'));
    console.log("URL sbagliato!!");
}); */

app.listen(3000, () => {
    console.log("App listening on port 3000!!");
});