$clientId = Read-Host "Enter your Client ID"
$clientSecret = Read-Host "Enter your Client Secret"
$code = Read-Host "Enter the new Authorization Code (from Self Client)"

$url = "https://accounts.zoho.in/oauth/v2/token"
$body = @{
    client_id = $clientId
    client_secret = $clientSecret
    grant_type = "authorization_code"
    code = $code
}

try {
    $response = Invoke-RestMethod -Uri $url -Method Post -Body $body
    Write-Host "`n----------------------------------------"
    Write-Host "SUCCESS! Here is your new Refresh Token:" -ForegroundColor Green
    Write-Host $response.refresh_token -ForegroundColor Cyan
    Write-Host "----------------------------------------`n"
    Write-Host "Please update application-zoho.properties with this value."
} catch {
    Write-Host "Error: " $_.Exception.Message -ForegroundColor Red
    Write-Host "Details: " $_.ErrorDetails.Message -ForegroundColor Red
}
Pause
