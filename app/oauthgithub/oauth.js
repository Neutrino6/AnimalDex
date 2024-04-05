//"node oauth.js" avvia il server
//visitare url "localhost:8080/auth/github"

//02/04/24 siamo riusciti a autenticare e reidirizzare l'utente ad una pagina con github (continuare a vedere se le informazioni di autenticazione si possono utilizzare)

const express = require('express');
const session = require('express-session');
const passport = require('passport');
const GitHubStrategy = require('passport-github2').Strategy;

const app = express();

app.use(express.static('public'));

// Configurazione della sessione
app.use(session({
    secret: 'il_tuo_segreto_sess',
    resave: false,
    saveUninitialized: true
}));

// Inizializzazione di Passport
app.use(passport.initialize());
app.use(passport.session());

// Configurazione della strategia di autenticazione GitHub (GENERALIZZARE PER PIÙ UTENTI)
passport.use(new GitHubStrategy({
    clientID: CLIENT_ID,
    clientSecret: CLIENT_SECRET,
    callbackURL: "http://localhost:8080/auth/github/callback"
  },
  function(accessToken, refreshToken, profile, done) {
    // Qui puoi gestire l'utente autenticato come preferisci
    // Esempio: Salva i dati del profilo dell'utente nel database o utilizzali per l'autenticazione
    const user = {
        username: profile.username,
        password: profile.password
        // Puoi aggiungere altri campi del profilo dell'utente qui
    }; 
  
      // Esempio: Salva l'utente nel database o restituiscilo per il login
      return done(null, user);
    }
));


// Serializzazione e deserializzazione degli utenti
passport.serializeUser(function(user, done) {
  done(null, user);
});

passport.deserializeUser(function(obj, done) {
  done(null, obj);
});

// Rotte per l'autenticazione con GitHub
app.get('/auth/github',
  passport.authenticate('github', { scope: [ 'user:email' ] }));

app.get('/auth/github/callback',
  passport.authenticate('github', { failureRedirect: '/' }),
  function(req, res) {
    // Autenticazione riuscita, puoi reindirizzare l'utente dove preferisci
    const username = req.user.username;
    const passw = req.user.password;
    res.redirect(`/PersonalPage.html?username=${username}&password=${passw}`);
    //res.redirect('/PersonalPage.html');
  });

// Rotta protetta, solo utenti autenticati possono accedere
app.get('/PersonalPage.html', ensureAuthenticated, function(req, res) {
  //res.send('Welcome, ' + req.user.displayName + '!');
  res.sendFile(__dirname + '/public/PersonalPage.html');     
});

// Funzione middleware per assicurarsi che l'utente sia autenticato
function ensureAuthenticated(req, res, next) {
  if (req.isAuthenticated()) { return next(); }
  res.redirect('/');
}

// Avvio del server
const PORT = process.env.PORT || 8080;
app.listen(PORT, () => {
  console.log(`Server listening on port ${PORT}`);
});
