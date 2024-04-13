# AnimalDex

- user stories e lofi con balsamiq (and a textual description highlighting specific non functional requirements (if any).)
- function points e cocomo 2 (fogli excel)
- si inizia a lavorare segnando ogni volta i dati per lo scrum + documento word con la descrizione del progetto con tecnologie ecc.
- animal emergency (idea di implementazione): viene inviato un report ad una coda di messaggi (stile prof a lezione vedi rabbitmq e/o immuni like app da esame), dopo tot minuti viene mandato indietro un messaggio che notifica l'arrivo dei soccorsi. Dopo altri tot minuti viene inviato un messaggio tra tre possibili (randomico) che definisce l'esito del soccorso;  
- pagine: login/register, pagina personale user/admin, pagina principale mappa, help center generale, help center animali feriti, forum, pagina personale operatore, pagina animaldex (completo e/o personale), pagina eventi, pagina valutazioni utenti/operatori;
- In seguito sistemare la questione database legata alle notifiche per commenti e alarm (se tracciare o meno chi ha generato il commento/alarm e come farlo).
- Istruzioni per uso di docker (prova per visualizzare il database):
 1) Nella cartella di AnimalDex eseguire il comando "docker compose up --build";
 2) Una volta eseguito, le immagini di postgres e pgadmin4 saranno buildate e pronte all'uso;
 3) Andate nel browser e digitate nella barra delle url "localhost:5050";
 4) Questo aprirà l'interfaccia di pgadmin4. Dovrete accedere con le credenziali specificate nel file "docker-compose.yml"
 5) Effettuato l'accesso, dovrete aggiungere un server in questo modo -> 
 Nome (nella sezione Generale) qualsiasi; 
 host name/Address = indirizzo ip che trovate in fondo al terminale eseguiti i comandi "docker ps" per visualizzare gli id dei container e "docker inspect {postgres_container_id}" per visualizzare i dettagli del container di postgres e trovare l'indirizzo per connettersi;
 Username = postgres
 Password= postgres
 Port = 5432
 6) Una volta connessi, potrete trovare il database inizializzato con le tabelle specificate nel file init.sql;
 7) Questo dovrebbe risolvere il problema della gestione del database in quanto viene salvato nel container di postgres e i dati in esso mantenuti persistenti.

 Quando viene apportato un cambiamento in un file *.sql, se una volta eseguito il container nel servizio postgres si torva "Skipping initialization", allora bisogna assicurarsi di rimuovere manualmente i volumi che vengono utilizzati da quel container. I voluni possono essere cancellati all'interno di Docker Desktop, ma può capitare che vi siano dei volumi ancora in uso che non sono possibili da eliminare. In tal caso esuguire "docker ps -a" e rimuovere i container che sono ancora in stato di running con il comando "docekr rm <nome-container/id-container>"; infine si possono quindi eliminare i volumi dall'interno dei Docker Desktop.

Mercoledì dalle 17:30, Venerdì da definire, Domenica da definire
