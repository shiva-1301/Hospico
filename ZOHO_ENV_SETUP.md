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
ZOHO_CLIENT_ID=<your_client_id>
ZOHO_CLIENT_SECRET=<your_client_secret>
ZOHO_REFRESH_TOKEN=<your_refresh_token>
```

## Generate Refresh Token (Self Client)
Use the **API Console** to create a Self Client OAuth app and generate a refresh token.

### Required Scopes (based on current backend usage)
**Data Store Row APIs**
- `ZohoCatalyst.tables.rows.CREATE`
- `ZohoCatalyst.tables.rows.READ`
- `ZohoCatalyst.tables.rows.UPDATE`
- `ZohoCatalyst.tables.rows.DELETE`

**ZCQL (used by query endpoints)**
- `ZohoCatalyst.zcql.CREATE`

**Optional (only for /api/test-zoho diagnostics)**
- `ZohoCatalyst.project.READ`
- `ZohoCatalyst.project.environments.READ`

### Steps (Self Client)
1. Open **Catalyst API Console** → **Self Client**.
2. Add the scopes above and generate an **authorization code**.
3. Run the token generator script:
  - `backend/generate_token.ps1`
4. Paste the authorization code when prompted to get the **refresh token**.

> Note: You must use a **Super Admin** or a **Collaborator** with access to this Catalyst project to generate tokens.

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
