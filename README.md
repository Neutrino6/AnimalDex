# AnimalDex
  
- Istruzioni per uso di docker (visualizzare il database):
 1) Nella cartella di AnimalDex eseguire il comando "docker compose up --build";
 2) Una volta eseguito, le immagini di postgres e pgadmin4 saranno buildate e pronte all'uso;
 3) Andate nel browser e digitate nella barra delle url "localhost:5050";
 4) Questo aprirà l'interfaccia di pgadmin4. Dovrete accedere con le credenziali specificate nel file "docker-compose.yml"
 5) Effettuato l'accesso, dovrete aggiungere un server in questo modo -> 
 Nome (nella sezione Generale) qualsiasi; 
 host name/Address = 172.18.0.1 solitamente, altrimenti eseguire i comandi "docker ps" per visualizzare gli id dei container e "docker inspect {postgres_container_id}" per visualizzare i dettagli del container di postgres e trovare l'indirizzo per connettersi;
 Username = postgres
 Password= postgres
 Port = 5432 o 5433 (a seconda del db)
 6) Una volta connessi, potrete trovare il database inizializzato con le tabelle specificate nel file init.sql;

 Quando viene apportato un cambiamento in un file *.sql, se una volta eseguito il container nel servizio postgres si torva "Skipping initialization", allora bisogna assicurarsi di rimuovere manualmente i volumi che vengono utilizzati da quel container. I voluni possono essere cancellati all'interno di Docker Desktop, ma può capitare che vi siano dei volumi ancora in uso che non sono possibili da eliminare. In tal caso esuguire "docker ps -a" e rimuovere i container che sono ancora in stato di running con il comando "docker rm <nome-container/id-container>"; infine si possono quindi eliminare i volumi dall'interno dei Docker Desktop.
