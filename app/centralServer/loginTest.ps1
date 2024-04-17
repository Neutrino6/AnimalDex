$body = @{
    email = "example@example.com";
    hashedPassw = "password123" 
} | ConvertTo-Json

Invoke-WebRequest -Uri "http://localhost:6039/userSignIn" -Method Post -Body $body -ContentType "application/json"