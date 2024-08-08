# Dockerfile.consumer

# Usa l'immagine ufficiale di Python come base
FROM python:3.11-slim

# Imposta la directory di lavoro
WORKDIR /app

# Copia il file di requirements e installa le dipendenze
COPY requirements.txt ./
RUN pip install --no-cache-dir -r requirements.txt

# Copia il codice sorgente nell'immagine
COPY consumer.py ./

# Comando per eseguire il consumatore
CMD ["python", "consumer.py"]
