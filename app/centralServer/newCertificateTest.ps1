$body = @{
    animalName = "Frog"
} | ConvertTo-Json

Invoke-WebRequest -Uri "http://localhost:6039/newCertificate" -Method Post -Body $body -ContentType "application/json"
