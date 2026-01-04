# Zoho Catalyst AppSail Support Request
**Date**: January 3, 2026  
**Priority**: High - Application Deployment Blocked

---

## Issue Summary

Backend container consistently killed at ~10 seconds during Spring Boot startup on AppSail platform, preventing successful deployment. Application never completes initialization despite clean startup logs with no errors or exceptions.

---

## Environment Details

- **Platform**: Zoho Catalyst AppSail
- **Application**: Spring Boot 3.4.5 backend (Java 21)
- **Database**: PostgreSQL 18.1 (Zoho Catalyst service)
- **AppSail Configuration**:
  - Memory: 2048 MB
  - Disk: 256 MB
  - Port: 8080
  - Startup Command: `java -jar app.jar`
- **Docker Images Tested**: 5 versions (v6-jan03-final, v7-ddl-none, v9-async-fix, v10-yaml-fix, v11-liveness)

---

## Problem Description

### Observed Behavior
Container starts normally but is abruptly terminated at 8-10 seconds into startup, immediately after HikariPool-1 database connection completes. The application never reaches the "Started HospitalBookingBackendApplication" message. This creates an endless restart loop.

### Consistent Restart Pattern (All Versions)
```
00:00 - Spring Boot application starts
~02s  - Tomcat initialized on port 8080
~04s  - Spring Data JPA repositories bootstrapped (7 found)
~06s  - Hibernate ORM initialized
~08s  - HikariPool-1 database connection started and completed
~10s  - ❌ CONTAINER ABRUPTLY KILLED (no error, no exception)
~10s  - Container automatically restarts, cycle repeats indefinitely
```

### Key Characteristics
- No error messages in application logs
- No Java exceptions or stack traces
- No Spring Boot shutdown logs ("Stopping service [Tomcat]")
- Termination occurs at exact same point across all tested versions
- Timing suggests platform-level timeout (~10 seconds)
- Application runs successfully in local Docker environment

---

## Evidence

### Sample Logs (v11-liveness - Latest Deployment)
```
2026-01-02T19:46:04.800Z  INFO --- Bootstrapping Spring Data JPA repositories
2026-01-02T19:46:04.904Z  INFO --- Finished Spring Data repository scanning in 74 ms. Found 7 JPA repository interfaces.
2026-01-02T19:46:06.423Z  INFO --- Tomcat initialized with port 8080 (http)
2026-01-02T19:46:06.442Z  INFO --- Starting service [Tomcat]
2026-01-02T19:46:06.484Z  INFO --- Root WebApplicationContext: initialization completed in 4065 ms
2026-01-02T19:46:07.075Z  INFO --- Hibernate ORM core version 6.6.13.Final
2026-01-02T19:46:07.528Z  INFO --- No LoadTimeWeaver setup: ignoring JPA class transformer
2026-01-02T19:46:09.194Z  INFO --- HikariPool-1 - Starting...
2026-01-02T19:46:09.209Z  INFO --- HikariPool-1 - Start completed.
[CONTAINER KILLED - NO FURTHER LOGS]
[RESTART CYCLE BEGINS AT 2026-01-02T19:46:16...]
```

### Restart Frequency
From logs dated 01:16:04 to 01:16:43 (39 seconds):
- Restart 1: 01:16:04 → 01:16:09 (killed after 5 seconds)
- Restart 2: 01:16:16 → 01:16:20 (killed after 4 seconds)
- Restart 3: 01:16:28 → 01:16:32 (killed after 4 seconds)
- Restart 4: 01:16:39 → 01:16:43 (killed after 4 seconds)

**Average kill time**: ~8-10 seconds after startup begins

---

## Attempted Solutions

### Version 1: v6-jan03-final
- **Changes**: Fixed Dockerfile paths, optimized configuration
- **Config**: `banner-mode: off`, `lazy-initialization: true`, minimal HikariCP pool
- **Result**: ❌ Still restarting at 10 seconds

### Version 2: v7-ddl-none
- **Changes**: Set `spring.jpa.hibernate.ddl-auto: none` (test if schema operations cause timeout)
- **Result**: ❌ Still restarting at 10 seconds
- **Conclusion**: Schema operations not the cause

### Version 3: v9-async-fix
- **Changes**: Changed CommandLineRunner beans to `@Async @EventListener(ApplicationReadyEvent.class)`
- **Purpose**: Remove blocking database operations from startup sequence
- **Result**: ❌ Initial YAML syntax error, but restart pattern identical

### Version 4: v10-yaml-fix
- **Changes**: Fixed duplicate YAML key causing DuplicateKeyException
- **Result**: ❌ Still restarting at 10 seconds (YAML error resolved but restart persists)

### Version 5: v11-liveness (Current)
- **Changes**: Added `/health/liveness` (always returns 200 OK) and `/health/readiness` endpoints
- **Purpose**: Respond immediately to health probes during startup
- **Result**: ❌ **NO IMPROVEMENT** - restart pattern completely unchanged
- **Docker Digest**: `sha256:c2b2e7916ead54bba9fb0785a8c6dac3421543cb78dd58524bc64abc9091207c`

### Additional Optimizations Applied
- Increased memory allocation to 2GB (ruled out memory limits)
- Set explicit startup command `java -jar app.jar`
- Disabled JMX, WebSocket, admin JMX autoconfiguration
- Minimized HikariCP pool size (minimum-idle: 1, maximum-pool-size: 2)
- Disabled Hibernate statistics and query logging
- Configured SecurityConfig to permit health endpoints without authentication

---

## Technical Analysis

### What We've Ruled Out
1. ❌ Memory limits (2GB allocated, far more than needed)
2. ❌ Schema operations (tested with `ddl-auto: none`)
3. ❌ Blocking CommandLineRunner beans (changed to async)
4. ❌ YAML syntax errors (fixed in v10)
5. ❌ Health check probe failures (v11 returns 200 OK immediately, no effect)
6. ❌ Startup command configuration (explicitly set)
7. ❌ Application code errors (clean logs, no exceptions)
8. ❌ Database connectivity (HikariPool-1 successfully completes before kill)

### Root Cause Hypothesis
AppSail platform has a **hard-coded ~10 second startup timeout** that cannot be configured through the console UI. Spring Boot application requires ~12-15 seconds to fully initialize and reach "Started" state, but platform kills container at 10 seconds before completion.

### Supporting Evidence
1. Consistent 10-second termination across all 5 versions
2. No error messages (indicates external kill signal, not application crash)
3. Termination at exact same startup phase (after HikariPool)
4. No configurable timeout options visible in AppSail console
5. No configurable health check/probe options in AppSail console
6. Health endpoint strategy (v11) had zero effect (would work if probes were the issue)
7. Application runs successfully in local Docker environment

---

## Requests

### 1. Primary Request: Increase Startup Timeout
**Current**: ~10 seconds (inferred from consistent kill timing)  
**Requested**: 30-60 seconds  
**Justification**: Spring Boot applications with JPA/Hibernate typically require 12-20 seconds for full initialization. This is standard for enterprise Java applications.

### 2. Health Check Configuration Access
**Requested Configuration**:
- **Liveness Probe**: Path `/health/liveness`, Initial Delay 5s, Period 10s, Timeout 5s
- **Readiness Probe**: Path `/health/readiness`, Initial Delay 10s, Period 5s, Timeout 3s
- **Startup Probe** (if available): Path `/health/liveness`, Period 2s, Failure Threshold 30 (60s total)

**Note**: Health endpoints already implemented in v11-liveness and respond correctly.

### 3. Platform Logs Access
Please provide access to platform-level logs that show:
- Why container is being killed (timeout, failed health check, resource limit, etc.)
- What signal is being sent to container (SIGTERM, SIGKILL)
- Health check probe results (if probes are being executed)

### 4. Alternative Solution Guidance
If startup timeout cannot be increased or configured, please provide:
- Alternative AppSail deployment strategies
- Recommended application architecture changes for faster startup
- Alternative Zoho Catalyst deployment options

---

## Impact

- **Current Status**: Application deployment completely blocked
- **Business Impact**: Unable to deploy backend API, frontend cannot function
- **Timeline**: Issue persists across 5 deployment attempts over 24+ hours
- **Urgency**: High - production deployment on hold

---

## Supporting Files Available

1. Complete application logs for all 5 versions
2. Docker images available on Docker Hub: `crvpt/hospico-backend:v11-liveness`
3. AppSail console screenshots showing configuration
4. Local Docker test results (successful startup in ~12 seconds)

---

## Expected Resolution

**If timeout can be increased**:
- Redeploy v11-liveness with increased timeout
- Application should complete startup in ~12-15 seconds
- Container remains stable, no restarts
- All API endpoints accessible and functional

**If timeout cannot be adjusted**:
- Guidance on alternative deployment approach
- Escalation to engineering team for platform enhancement request
- Migration path to alternative Zoho Catalyst deployment option

---

## Contact Information

- **Project**: Hospico Hospital Booking Backend
- **Docker Hub**: crvpt/hospico-backend
- **Preferred Response**: Email with ticket number and estimated resolution timeline
- **Availability**: Monitoring deployment 24/7, can provide additional logs/information immediately

---

## Additional Notes

- Application has been thoroughly tested locally with Docker and works correctly
- All standard Spring Boot optimization best practices have been applied
- Code base is production-ready, issue is purely platform-related
- Willing to participate in debugging/testing of platform solutions

Thank you for your assistance in resolving this critical deployment blocker.
