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
    if(document.iscrizione.email.value.length > 50){
        alert("Email too long");
        return false;
    }
    if(emailRegex.test(document.iscrizione.email.value)==false ){
        alert("Wrong e-mail format");
        return false;
    }
    return true;
}

function validateReg() {
    if (hasChar(document.iscrizione.username.value, BlackListChars_username) ) {
        alert("Punctuation characters, brackets and most of special characters are not allowed in the username field");
        return false;
    }

    if (hasChar(document.iscrizione.name.value, BlackListChars_username) ) {
        alert("Punctuation characters, brackets and most of special characters are not allowed in the name field");
        return false;
    }

    if (hasChar(document.iscrizione.surname.value, BlackListChars_username) ) {
        alert("Punctuation characters, brackets and most of special characters are not allowed in the surname field");
        return false;
    }

    if (hasChar(document.iscrizione.password.value, BlackListChars_passw)) {
        alert("Comma, double comma, space and column are not allowed in the password field");
        return false;
    }

    if (hasChar(document.iscrizione.email.value, BlackListChars_username) ) {
        alert("Punctuation characters, brackets and most of special characters are not allowed in the e-mail field");
        return false;
    }

    if (document.iscrizione.confirmpassword.value!=document.iscrizione.password.value) {
        alert("Password does not correspond");
        return false;
    }

    if(document.iscrizione.acceptterms.checked==false){
        alert("Accept terms and conditions to continue");
        return false;
    }
    
    var bool = controllo_email();

    if(bool==false) {
        alert("Wrong e-mail format");
        return false;
    }

    if (document.iscrizione.email.value.length > 255) {
        alert("Email too long");
        return false;
    }

    if (document.iscrizione.name.value.length > 255) {
        alert("Name too long");
        return false;
    }

    if (document.iscrizione.username.value.length > 255) {
        alert("Username too long");
        return false;
    }
    if (document.iscrizione.surname.value.length > 255) {
        alert("Surname too long");
        return false;
    }

    return true;
}