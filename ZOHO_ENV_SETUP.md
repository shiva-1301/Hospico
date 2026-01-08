# Zoho Data Store Environment Variables Setup

After deployment completes, configure these environment variables in the Catalyst console:

## Steps:
1. Go to: https://console.catalyst.zoho.in/
2. Select project: **Hospiico**
3. Navigate to: **AppSail** → **Hospiico-Backend** → **Configuration** → **Environment Variables**
4. Add the following variables:

## Environment Variables:

```
ZOHO_PROJECT_ID=26566000000013009
ZOHO_ENV_ID=60061261997
ZOHO_REGION=accounts.zoho.in
ZOHO_CLIENT_ID=1000.URGACLE5ZTT7GS958IRQZQFYPK4XJS
ZOHO_CLIENT_SECRET=073f7fb2cebe09867014a3267e95ff4118192a92d1
ZOHO_REFRESH_TOKEN=1000.4bff48e25275982f0b465c5f50d0565d.caba23696a4a3b34e080eb5edbfbbec6
```

## Additional Settings:
Set the active Spring profile to enable Zoho:
```
SPRING_PROFILES_ACTIVE=zoho
```

## After Adding Variables:
1. Save the configuration
2. Restart the AppSail service
3. Test the signup endpoint: `POST /api/auth/signup`

## Test Payload:
```json
{
  "name": "Test User",
  "email": "test@example.com",
  "phone": "+1234567890",
  "password": "TestPass123!"
}
```

## Expected Behavior:
- User will be created in Zoho Data Store `users` table
- JWT token will be returned
- Cookies will be set

## Verification:
Check the Data Store in Catalyst console:
https://console.catalyst.zoho.in/ → **Data Store** → **users** table
