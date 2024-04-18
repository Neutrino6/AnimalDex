# Usa un'immagine di Node.js come base
FROM node:latest

# Copia il package.json e il package-lock.json nella directory di lavoro
COPY package*.json ./

# Copia il codice sorgente nella directory di lavoro
COPY . .

# Esponi la porta su cui il server Node.js ascolterà le richieste
EXPOSE 3000

# Comando per avviare il server Node.js
CMD ["node", "FrontendUser.js"]