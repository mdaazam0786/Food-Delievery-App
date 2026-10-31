# 404 NOT_FOUND Error Fix - Auth Service on Vercel

## Problem

When accessing `https://food-delievery-app-auth-service.vercel.app/` (or any service root), received:
```
404 NOT_FOUND
Request ID: bom1::z92w7-1793389012171-9b725e3f4fcd
```

Also confirmed: `/actuator/health` endpoint returned 404, indicating the application wasn't running at all.

## Root Cause

The Docker build for all microservices was **failing silently** on Vercel because:

1. **Missing Local Dependency**: All microservices depend on the `common-exceptions` module:
   ```xml
   <dependency>
       <groupId>com.foodzie</groupId>
       <artifactId>common-exceptions</artifactId>
       <version>0.0.1-SNAPSHOT</version>
   </dependency>
   ```

2. **Build Context Issue**: The initial `Dockerfile.vercel` only copied the individual service directory (e.g., `auth-service/`), not the entire backend monorepo.

3. **Maven Resolution Failure**: When `mvn clean package` ran in the Docker build, it couldn't find the `common-exceptions` JAR in the Maven repository because it's a local SNAPSHOT dependency.

4. **Silent Failure**: The Docker build didn't explicitly fail—it just couldn't complete the build step, leaving no JAR artifact to run.

## Solution

Updated all `Dockerfile.vercel` files to:

1. **Copy entire backend directory** instead of just the service folder:
   ```dockerfile
   COPY backend ./
   ```

2. **Build common-exceptions first** before building each service:
   ```dockerfile
   WORKDIR /build/common-exceptions
   RUN mvn clean install -q
   
   WORKDIR /build/{service-name}
   RUN mvn clean package -DskipTests -q
   ```

3. **Update JAR paths** in the runtime stage to reflect the new structure:
   ```dockerfile
   COPY --from=builder /build/{service-name}/target/*.jar app.jar
   ```

### Key Change in Dockerfile Structure

**Before:**
```dockerfile
WORKDIR /build
COPY pom.xml .
RUN mvn dependency:go-offline -B
COPY src ./src
RUN mvn clean package -DskipTests -q
```

**After:**
```dockerfile
COPY backend ./
WORKDIR /build/common-exceptions
RUN mvn clean install -q
WORKDIR /build/{service-name}
RUN mvn clean package -DskipTests -q
```

## Vercel Deployment Requirements

For this to work on Vercel, the build context must be set from the **repository root** or **backend directory**:

```bash
# Correct - from repository root
docker build -t auth-service -f backend/auth-service/Dockerfile.vercel .

# Or - from backend directory  
cd backend && docker build -t auth-service -f auth-service/Dockerfile.vercel .
```

## Files Modified

Updated all 9 microservice Dockerfiles:
- `backend/auth-service/Dockerfile.vercel`
- `backend/user-service/Dockerfile.vercel`
- `backend/cart-service/Dockerfile.vercel`
- `backend/order-service/Dockerfile.vercel`
- `backend/payment-service/Dockerfile.vercel`
- `backend/restaurant-service/Dockerfile.vercel`
- `backend/search-service/Dockerfile.vercel`
- `backend/delivery-service/Dockerfile.vercel`
- `backend/order-fulfillment-service/Dockerfile.vercel`

## Commit

**Commit**: `bc57489`
**Message**: `fix: Update Dockerfile.vercel to include common-exceptions module for Vercel build context`

## Testing

After deploying with the updated Dockerfiles, verify the services are running:

```bash
# Check if service is running
curl https://food-delievery-app-auth-service.vercel.app/actuator/health

# Expected response (healthy):
{"status":"UP"}

# Check API endpoints
curl -X POST https://food-delievery-app-auth-service.vercel.app/api/v1/auth/register
```

## Important Notes

1. **Build Context**: Vercel or your CI/CD must use the correct build context (repository root or backend directory)

2. **Local Development**: When building locally for testing:
   ```bash
   cd /path/to/Foodzie
   docker build -t foodzie-auth-service -f backend/auth-service/Dockerfile.vercel .
   ```

3. **Maven Repository**: Ensure common-exceptions is built before other services or make it available in Maven Central

4. **Build Time**: Initial builds will take longer due to Maven dependency resolution and common-exceptions compilation

5. **Cache Optimization**: The multi-stage build helps keep the final image small (~200MB for JRE Alpine + app)

## Prevention Tips

- Always test Docker builds locally before pushing to Vercel
- Use `docker build --no-cache` to test complete builds
- Check Docker build logs for Maven resolution errors
- Consider publishing common-exceptions to a Maven repository (Nexus, Artifactory, Maven Central)
