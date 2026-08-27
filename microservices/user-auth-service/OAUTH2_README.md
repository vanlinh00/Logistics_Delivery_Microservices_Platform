# OAuth2 & OpenID Connect (OIDC) Integration Guide

This guide explains how authentication and authorization work in the **Logistics & Delivery Platform**, covering:
1. **Google OAuth2 Login** (Social Login & Account Sync)
2. **Keycloak OIDC Integration** (Direct Access Grant & SSO)
3. **Cookie-based State & Request Management**
4. **Calling Protected Microservices with JWTs**

---

## 1. Google OAuth2 Login Flow

### Architecture Overview

```
 [Browser / Client]
        │
        │ 1. GET /oauth2/authorization/google
        ▼
 [user-auth-service] ── (Saves encrypted auth state in cookie) ──► Redirects to Google
        │
        ▼
 [Google Accounts] ── User signs in & consents
        │
        │ 2. Redirect with ?code=...&state=...
        ▼
 [user-auth-service] (/login/oauth2/code/google)
        │
        ├─► 3. Exchanges code for Google ID Token & Profile (email, name, sub)
        ├─► 4. Checks PostgreSQL `users` table
        │      ├─ If new user: creates user, assigns ROLE_CUSTOMER
        │      └─ If existing user: links googleId & updates profile
        ├─► 5. Provisions / Syncs user into Keycloak IAM (`logistics-realm`)
        └─► 6. Generates / Obtains JWT Tokens & returns JSON AuthResponse
```

---

### Step-by-Step Usage

#### Step 1: Initiate Login
Direct the user's browser to the Spring Security authorization endpoint:

```
GET http://localhost:8080/oauth2/authorization/google
```
*(If accessing `user-auth-service` directly: `http://localhost:8081/oauth2/authorization/google`)*

> ⚠️ **Important:** Never call or paste `/login/oauth2/code/google` directly. That URL is reserved exclusively as the redirection callback from Google.

#### Step 2: Google Authorization & Redirection
1. The user authenticates on Google (`accounts.google.com`).
2. Google redirects the browser back to:
   ```
   http://localhost:8080/login/oauth2/code/google?code=4/0Abc...&state=xyz...
   ```
3. `user-auth-service` automatically processes the code, validates state cookies, and issues the session response.

#### Step 3: Success Response
On successful authentication, the server returns:

```json
{
  "success": true,
  "code": "200",
  "message": "Google authentication successful",
  "data": {
    "accessToken": "eyJhbGciOiJSUzI1NiIsIn...",
    "refreshToken": "eyJhbGciOiJSUzI1NiIsIn...",
    "tokenType": "Bearer",
    "expiresIn": 86400,
    "userId": "d290f1ee-6c54-4b01-90e6-d701748f0851",
    "username": "an_nguyen",
    "email": "an.nguyen@example.com",
    "fullName": "Nguyễn Văn An",
    "role": "ROLE_CUSTOMER",
    "permissions": ["ORDER_CREATE", "ORDER_READ", "TRACKING_READ"],
    "requiresTwoFactor": false
  }
}
```

---

### Google Cloud Console Configuration

To configure Google OAuth credentials:
1. Navigate to [Google Cloud Console > Credentials](https://console.cloud.google.com/apis/credentials).
2. Open your **OAuth 2.0 Client ID**.
3. Under **Authorized JavaScript Origins**, add:
   - `http://localhost:3000` (Frontend)
   - `http://localhost:8080` (API Gateway)
4. Under **Authorized Redirect URIs**, add:
   - `http://localhost:8080/login/oauth2/code/google`
   - `http://localhost:8081/login/oauth2/code/google` (if debugging directly)
5. Update your `.env` or `application.yml`:
   ```yaml
   spring:
     security:
       oauth2:
         client:
           registration:
             google:
               client-id: ${GOOGLE_CLIENT_ID}
               client-secret: ${GOOGLE_CLIENT_SECRET}
   ```

---

## 2. Keycloak Direct Authentication (Username & Password)

For mobile apps or single-page apps using standard login forms:

### Register User
```bash
curl --location 'http://localhost:8080/api/v1/auth/register' \
--header 'Content-Type: application/json' \
--data-raw '{
  "username": "customer_an",
  "email": "an.nguyen@example.com",
  "password": "anPassword123!",
  "fullName": "Nguyễn Văn An",
  "phone": "0987112233",
  "role": "ROLE_CUSTOMER"
}'
```
*Creates the local user record, provisions the Keycloak user in `logistics-realm`, maps realm roles, and returns tokens.*

---

### Login User
```bash
curl --location 'http://localhost:8080/api/v1/auth/login' \
--header 'Content-Type: application/json' \
--data-raw '{
  "username": "customer_an",
  "password": "anPassword123!"
}'
```

---

### Refresh Token
```bash
curl --location 'http://localhost:8080/api/v1/auth/refresh' \
--header 'Content-Type: application/json' \
--data-raw '{
  "refreshToken": "eyJhbGciOiJSUzI1NiIsIn..."
}'
```

---

## 3. Calling Protected Microservices

Include the `accessToken` in the `Authorization` header:

```bash
curl --location 'http://localhost:8080/api/v1/orders' \
--header 'Authorization: Bearer <YOUR_ACCESS_TOKEN>'
```

Downstream services (such as `order-service`, `delivery-service`) automatically validate the RS256 signature against the Keycloak JWKS endpoint:
```
http://localhost:8180/realms/logistics-realm/protocol/openid-connect/certs
```

---

## 4. Troubleshooting & FAQ

| Issue / Error | Cause | Solution |
| :--- | :--- | :--- |
| `[invalid_request]` on `/login/oauth2/code/google` | Directly opening or refreshing the callback URL in browser or Postman without parameters. | Always start from `http://localhost:8080/oauth2/authorization/google`. |
| `redirect_uri_mismatch` on Google screen | Google Console redirect URI does not match current host/port. | Add exact callback URI `http://localhost:8080/login/oauth2/code/google` to Google Console. |
| User not found in Keycloak after registration | Keycloak Admin REST API credentials missing or unconfigured. | Verify `KEYCLOAK_ADMIN` and `KEYCLOAK_ADMIN_PASSWORD` (default: `admin`/`admin`) in `application.yml`. |
| Session lost across Gateway / redirects | In-memory session not shared. | Fixed by `HttpCookieOAuth2AuthorizationRequestRepository` using stateless encrypted cookies. |
