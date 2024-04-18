const BlackListChars_username=["'", '"', " ", ",", ";", "(", ")", "[", "]", "{", "}", "<", ">", ":", "?", "!", "$", "&", "*", "#", "^", "/", "|", "-", "+", "~", "."];
const BlackListChars_passw=["'", '"', " ", ","];

function hasChar(s, chars) {
    for (let char of chars) {
      if (s.includes(char)) {
        return true;
      }
    }
    return false;
}

function controllo_email(){
    const emailRegex = /^[^\s@]+@[^\s@]+.[^\s@]+$/;
    if(document.reg.email.value.length > 50){
        alert("Email troppo lunga");
        return false;
    }
    if(emailRegex.test(document.reg.email.value)==false ){
        alert("Formato email errato");
        return false;
    }
    return true;
}

function validateReg() {
    if (hasChar(document.iscrizione.username.value, BlackListChars_username) ) {
        alert("I caratteri di punteggiatura, di parentesi e la maggior parte di quelli speciali non sono consentiti nel campo username");
        return false;
    }

    if (hasChar(document.iscrizione.name.value, BlackListChars_username) ) {
        alert("I caratteri di punteggiatura, di parentesi e la maggior parte di quelli speciali non sono consentiti nel campo username");
        return false;
    }

    if (hasChar(document.iscrizione.surname.value, BlackListChars_username) ) {
        alert("I caratteri di punteggiatura, di parentesi e la maggior parte di quelli speciali non sono consentiti nel campo username");
        return false;
    }

    if (hasChar(document.iscrizione.password.value, BlackListChars_passw)) {
        alert("I caratteri virgoletta, doppia virgoletta, spazio e virgola non sono consentiti nel campo password");
        return false;
    }

    if (hasChar(document.iscrizione.email.value, BlackListChars_username) ) {
        alert("I caratteri di punteggiatura, di parentesi e la maggior parte di quelli speciali non sono consentiti nel campo username");
        return false;
    }

    if (document.iscrizione.confirmpassword.value!=document.iscrizione.password.value) {
        alert("Password non corrispondenti");
        return false;
    }

    if(document.iscrizione.acceptterms.checked==false){
        alert("Accettare termini e condizioni per continuare");
        return false;
    }
    
    var bool = controllo_email();

    if(bool==false) {
        alert("Email non corretta");
        return false;
    }

    if (document.iscrizione.email.value.length > 255) {
        alert("Email troppo lunga");
        return false;
    }

    if (document.iscrizione.name.value.length > 255) {
        alert("name troppo lungo");
        return false;
    }

    if (document.iscrizione.username.value.length > 255) {
        alert("username troppo lungo");
        return false;
    }
    if (document.iscrizione.surname.value.length > 255) {
        alert("surname troppo lungo");
        return false;
    }

    return true;
}