# Email Draft to Zoho Support

---

**To:** support@zohocatalyst.com

**Subject:** CloudScale DataStore Connection to AppSail Backend - Error & Configuration Steps Required

---

Dear Zoho Catalyst Support Team,

I am writing to request urgent assistance with connecting **CloudScale DataStore tables** to our **AppSail Backend** service. We have been troubleshooting data store connectivity for several days and would like to establish a clear, working solution.

## Project Details
- **Project Name:** HospiiCo
- **Project ID:** 26566000000013009
- **Backend Service:** AppSail (Java Spring Boot)
- **Previous Support Ticket:** Reference issue from Jan 28 regarding Data Store Connection Error

---

## Summary of Issues Encountered

We have attempted to connect to Zoho Data Store using OAuth tokens but faced repeated authentication failures:

### Error 1: Initial Token Exchange Failure
**Error Response:**
```json
{
  "auth_token_error": "Failed to obtain Zoho access token",
  "auth_token_status": "FAILED"
}
```

### Error 2: OAuth Scope Mismatch
In your previous response (Jan 28), you mentioned that our refresh token requires specific scopes:
- `ZohoCatalyst.zcql.CREATE`
- `ZohoCatalyst.datastore.READ`
- `ZohoCatalyst.datastore.CREATE`

However, we are unable to locate where in the Catalyst Console to configure these OAuth scopes for our application Client ID: `1000.VJZHI5O3V1D0FTX43ZF5GO5DYVH6WL`

### Error 3: CloudScale Configuration Unknown
We want to pivot to **CloudScale (NoSQL tables)** for better flexibility, but we lack clear steps on how to:
1. Create/configure CloudScale tables in the Catalyst project
2. Generate the correct OAuth credentials with required scopes
3. Configure AppSail backend environment variables to connect to CloudScale
4. Test the connection

---

## Request for Assistance

We are requesting **step-by-step guidance** on:

1. **Scope Configuration:** Where exactly in Catalyst Console do we configure OAuth scopes for our app? (Screenshots appreciated)
2. **CloudScale Setup:** How do we create and configure CloudScale tables within the HospiiCo project?
3. **AppSail Integration:** What environment variables must be set in AppSail to connect to CloudScale tables?
4. **Authentication:** Should we use OAuth Refresh Token or Personal Access Token for CloudScale? If OAuth, what scopes are mandatory?
5. **Sample Configuration:** Could you provide a sample application.properties or environment variable configuration that works?

---

## Current Configuration

**AppSail Environment Variables (Current):**
```
ZOHO_CLIENT_ID=1000.VJZHI5O3V1D0FTX43ZF5GO5DYVH6WL
ZOHO_CLIENT_SECRET=fe94ab88f6add088abd1df93d032d37d23b8b7767e
ZOHO_REFRESH_TOKEN=1000.6cbe7b7db8658ac0ab3ac4c4c85914b5.13a86848285f64f49ed2eb890ac2afc8
ZOHO_REGION=accounts.zoho.in
ZOHO_PROJECT_ID=26566000000013009
ZOHO_ENV_ID=60061261997
ZOHO_USERS_TABLE_ID=users
```

**Backend Properties (Spring Boot):**
```properties
zoho.enabled=true
zoho.project.id=${ZOHO_PROJECT_ID}
zoho.env.id=${ZOHO_ENV_ID}
zoho.region=${ZOHO_REGION}
zoho.users.table.id=${ZOHO_USERS_TABLE_ID}
zoho.client.id=${ZOHO_CLIENT_ID}
zoho.client.secret=${ZOHO_CLIENT_SECRET}
zoho.refresh.token=${ZOHO_REFRESH_TOKEN}
```

---

## Urgency

Our frontend is live in production, but the backend database integration remains blocked. Any guidance to unblock this would be greatly appreciated.

We are available for a quick call or chat if that would help expedite the resolution.

Thank you for your swift assistance.

Best regards,

**HospiiCo Development Team**
Project ID: 26566000000013009

---

