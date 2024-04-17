$body = @{
    email = "giancarlo@magalli.it";
    username = "pippo";
    hashedPassw = "password123"
    # optional fields
    firstname = "giancarlo"
    surname = "magalli"
    dob = "1947-07-05"
} | ConvertTo-Json

Invoke-WebRequest -Uri "http://localhost:6039/userSignUp" -Method Post -Body $body -ContentType "application/json"