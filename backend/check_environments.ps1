# Zoho Catalyst Environment Checker

$clientId = "1000.56NL5XKO8B4W1XBL3TEJ52F62LQIEM"
$clientSecret = "a197f6020c7998385e4500d9c9bab92135e63a3060"
$refreshToken = "1000.24c39422690ccfae19a20c0db16496a7.27ef70eb85d76ac49e5e691b32a42f33"
$projectId = "26566000000013009"

# 1. Get Access Token
$tokenUrl = "https://accounts.zoho.in/oauth/v2/token"
$tokenBody = @{
    refresh_token = $refreshToken
    client_id     = $clientId
    client_secret = $clientSecret
    grant_type    = "refresh_token"
}

try {
    Write-Host "Fetching Access Token..." -ForegroundColor Cyan
    $tokenResp = Invoke-RestMethod -Uri $tokenUrl -Method Post -Body $tokenBody
    if ($tokenResp.error) { throw $tokenResp.error }
    $accessToken = $tokenResp.access_token
    Write-Host "Access Token Acquired." -ForegroundColor Green
} catch {
    Write-Host "Failed to get access token: $_" -ForegroundColor Red
    exit
}

# 2. Get Project Details (to find Environments)
$headers = @{
    Authorization = "Zoho-oauthtoken $accessToken"
}

# Note: Catalyst API structure for environments might be under project details or separate
# Let's try fetching the project structure to see environments
$projectUrl = "https://api.catalyst.zoho.in/baas/v1/project/$projectId"

try {
    Write-Host "Fetching Project Details..." -ForegroundColor Cyan
    $projResp = Invoke-RestMethod -Uri $projectUrl -Method Get -Headers $headers
    
    Write-Host "`nProject Info:" -ForegroundColor Yellow
    Write-Host "Name: $($projResp.data.project_name)"
    Write-Host "ID:   $($projResp.data.id)"
    
    # 3. List Environments (Development/Production)
    # The API for environments is often /project/{id}/environment or embedded
    # Trying specific endpoint often used for env retrieval
    
    # If not in standard project response, we might need a different endpoint. 
    # But usually project details contain basic info. 
    # Let's try a known endpoint pattern for environments if available.
    
    # Alternative: Use CLI logic approximation. 
    # Actually, the user's previous logs showed headers with 'x-lib-environment-id'.
    # We want to know what 60061261997 maps to.
    
    # Let's try to fetch environments specifically.
    $envUrl = "https://api.catalyst.zoho.in/baas/v1/project/$projectId/environment" 
    # Or console API often used: https://api.catalyst.zoho.com/baas/v1/projects/{project_id}/environments
    
    Write-Host "`nFetching Environments..." -ForegroundColor Cyan
    # Trying the standard API endpoint first
    try {
        $envResp = Invoke-RestMethod -Uri $envUrl -Method Get -Headers $headers
        $envs = $envResp.data
        
        Write-Host "`nAvailable Environments:" -ForegroundColor Magenta
        foreach ($env in $envs) {
            Write-Host "--------------------------------"
            Write-Host "Name:   $($env.environment_name)"
            Write-Host "ID:     $($env.id)"
            Write-Host "Status: $($env.status)"
            Write-Host "Type:   $($env.type)"
        }
        Write-Host "--------------------------------"
    } catch {
        Write-Host "Could not fetch environments directly via API: $($_.Exception.Message)" -ForegroundColor Red
        Write-Host "Attempting fallback (Project Summary)..."
    }

} catch {
    Write-Host "Error fetching project details: $_" -ForegroundColor Red
    if ($_.ErrorDetails) { Write-Host $_.ErrorDetails.Message -ForegroundColor Red }
}
