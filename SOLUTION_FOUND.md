# Solution Found: AppSail Port Configuration Issue

## Critical Discovery

From Zoho Catalyst documentation - "Key Points to Remember":

> **"Your app will need to start listening in the listening port within 10 seconds. When no process in that port is found to be active within this duration, the user instance that will be created past the threshold will be killed, and the next request will trigger a new cold start instance."**

This explains the 10-second restart pattern!

## Root Cause: Wrong Port Configuration

From "Default Port for App Startups" section:

> "AppSail listens through HTTP ports for network access in your application. You can listen to the AppSail port provided by Catalyst by referring to the environmental variable key **X_ZOHO_CATALYST_LISTEN_PORT** in your application code to connect to the Internet. Catalyst will check for the process listening on this port."

And critically:

> "Note: You will **not be able to modify** the port that Catalyst assigns for network access."

## The Problem

Current setup:
- Application hardcoded to listen on: **port 8080**
- AppSail console configured for: **port 8080**
- AppSail actually checks for process on: **X_ZOHO_CATALYST_LISTEN_PORT** (dynamic, assigned by platform)

**When AppSail doesn't find a process on X_ZOHO_CATALYST_LISTEN_PORT, it kills the instance.**

## Solution: Use Dynamic Port from Environment Variable

### Step 1: Update application.yml

The application needs to listen on the AppSail-assigned port, not a hardcoded port.

```yaml
server:
  port: ${X_ZOHO_CATALYST_LISTEN_PORT:8080}
```

This tells Spring Boot to:
1. Use the X_ZOHO_CATALYST_LISTEN_PORT environment variable if set (AppSail)
2. Fall back to 8080 if the variable is not set (local development)

### Step 2: Verify in Java Code (if needed)

If you're using custom port configuration in Java, ensure it also respects this variable:

```java
System.getenv("X_ZOHO_CATALYST_LISTEN_PORT")
```

### Step 3: Update AppSail Deployment Configuration

In AppSail console, when deploying:
- **Do NOT set explicit port to 8080**
- Leave port configuration default or let AppSail assign dynamically
- AppSail will set X_ZOHO_CATALYST_LISTEN_PORT automatically

OR explicitly set the environment variable in console:
- Add environment variable: `X_ZOHO_CATALYST_LISTEN_PORT` with appropriate value

## Why This Fixes the Issue

1. **Timing**: Tomcat IS starting within 10 seconds (verified in logs at ~6 seconds)
2. **Port Mismatch**: But it's listening on port 8080, not X_ZOHO_CATALYST_LISTEN_PORT
3. **AppSail Check**: AppSail checks X_ZOHO_CATALYST_LISTEN_PORT at ~10 seconds
4. **No Process Found**: Can't find listening process on expected port
5. **Instance Killed**: AppSail kills instance thinking startup failed
6. **Restart Loop**: Cycle repeats

## Implementation Steps

### 1. Update application.yml

Edit: `C:\Users\chunk\Desktop\Qod\Hospico\backend\src\main\resources\application.yml`

Find the `server` section (around line 1):
```yaml
server:
  port: 8080
```

Replace with:
```yaml
server:
  port: ${X_ZOHO_CATALYST_LISTEN_PORT:8080}
```

### 2. Rebuild Docker Image

```bash
cd C:\Users\chunk\Desktop\Qod\Hospico\backend
docker build --no-cache -t crvpt/hospico-backend:v12-dynamic-port -t crvpt/hospico-backend:latest .
docker push crvpt/hospico-backend:v12-dynamic-port
docker push crvpt/hospico-backend:latest
```

### 3. Update AppSail Deployment

In AppSail console:
1. Go to service configuration
2. **Remove or reset the explicit port setting** (don't hardcode 8080)
3. Add environment variable (if not auto-set):
   - Key: `X_ZOHO_CATALYST_LISTEN_PORT`
   - Value: Leave blank or let AppSail set it
4. Startup command remains: `java -jar app.jar`
5. Redeploy with v12-dynamic-port image

### 4. Verify Deployment

Expected behavior:
- App starts and listens on X_ZOHO_CATALYST_LISTEN_PORT
- AppSail finds listening process on correct port at ~6-8 seconds
- No 10-second timeout trigger
- Container remains stable
- Logs show "Started HospitalBookingBackendApplication" ✅

## Verification Checklist

After deployment:
- [ ] Container does NOT restart after 10 seconds
- [ ] Logs show "Started HospitalBookingBackendApplication"
- [ ] Container stays running continuously
- [ ] Health endpoints respond correctly
- [ ] API endpoints accessible and functional
- [ ] Monitor for 30+ minutes to confirm stability

## Why Previous Attempts Didn't Work

- v11-liveness added health endpoints but didn't address port mismatch
- Health probes may have been on wrong port, causing failures
- Timeout was hardcoded platform behavior, not health-check dependent
- Only solution: make app listen on port AppSail is actually checking

## Summary

The 10-second timeout isn't a bug—it's a platform feature that:
- Starts a new instance
- Waits 10 seconds for app to start listening
- Checks the expected port (X_ZOHO_CATALYST_LISTEN_PORT)
- Kills instance if no process found on that port

By using the environment variable instead of hardcoding port 8080, the application will listen on the correct port that AppSail is checking, allowing startup to complete successfully.
