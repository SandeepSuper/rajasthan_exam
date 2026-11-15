# Rajasthan Exam App: Catalyst Deployment (Chat Summary)

Tarikh: 31 Oct 2026. Ye file poori chat ka saar hai: kya decide hua, kya ban chuka hai, aur aage kya karna hai.

> Is file mein koi password ya secret nahi hai. Passwords sirf Catalyst console mein daalein, kisi ko bhejein nahi.

---

## 1. App aur repo

- Repo: https://github.com/SandeepSuper/rajasthan_exam (public)
- Stack: Kotlin + Spring Boot 3.2.1, Java 17, Gradle (Kotlin DSL)
- Libraries: JPA, PostgreSQL, Flyway, Redis, Spring Security + JWT (jjwt), Thymeleaf, WebSocket, Mail, Razorpay, Google OAuth, springdoc (Swagger), OpenCSV
- **Kafka aur Eureka dependencies repo mein hain hi nahi**, isliye unki zaroorat nahi
- Actuator dependency nahi hai (`/actuator/health` abhi exist nahi karta)
- MCQ CSV files repo root mein hain (LDC 2018, sample_questions wagairah)
- `application.yml`, `application-dev.yml`, `application-prod.yml` pehle se repo mein hain. **`application-prod.yml` ko overwrite mat karna.**

## 2. Zaroori faisle

| Sawal | Jawab |
|---|---|
| Catalyst par Postgres? | **Nahi.** Catalyst Data Store apna relational DB hai (ZCQL), PostgreSQL nahi. Isliye Postgres bahar (Neon). |
| Catalyst par Redis? | **Nahi.** Redis bahar (Upstash). |
| Redis hata sakte hain? | **Nahi.** Email signup OTP, forgot-password OTP, session token aur rate limiting Redis par chalte hain. |
| Nginx? | AppSail mein zaroorat nahi, HTTPS platform deta hai. |
| docker-compose? | AppSail par nahi chalta. Postgres + Redis ke liye alag services. |
| Backend ke liye Slate? | **Nahi.** Slate frontend (React/Vue) ke liye hai. Backend ke liye **AppSail**. |

## 3. Zoho Catalyst (India data center)

- Account IN data center par hai: `console.catalyst.zoho.in`
- Organization: `Sandeep`, ID `60093691756`
- Project: `rajasthan-exam`, ID `104456000000013051` (Development environment active)
- Project domain: `https://rajasthan-exam-60093691756.development.catalystserverless.in`
- Dusra project `Project-Rainfall` (ID `104456000000013023`) pehle ka hai, deploy ke liye use nahi hoga
- Catalyst MCP connector (IN): `https://catalyst.zohomcp.in/mcp/message`. Connect hone par org aur project list sahi aaye.
- MCP se Catalyst ke AppSail ko sirf dekh sakte hain (list/get). **Deploy CLI se hota hai.**

## 4. Neon (PostgreSQL)

- Project bana: region AWS Singapore, Postgres 16
- **Direct URL use karein, `-pooler` wala nahi** (Flyway migrations ke liye)
- Neon ka URL `postgresql://...` hota hai, Spring ko `jdbc:postgresql://...` chahiye. `user:password@` JDBC URL mein nahi jaata.
- `channel_binding=require` hata dein, JDBC driver ke liye `sslmode=require` kaafi hai

| Env variable | Value |
|---|---|
| `SPRING_DATASOURCE_URL` | `jdbc:postgresql://ep-aged-mode-aof65j3o.c-2.ap-southeast-1.aws.neon.tech/neondb?sslmode=require` |
| `SPRING_DATASOURCE_USERNAME` | `neondb_owner` |
| `SPRING_DATASOURCE_PASSWORD` | Neon ke `.env` wale URL ka password |

## 5. Upstash (Redis)

- Database: `rajasthan-exam-redis`, Free Tier, AWS Mumbai (ap-south-1), TLS enabled, eviction off
- Free limit: **500k commands per month**, 250 MB storage. Har authenticated request par ek Redis `GET` hota hai (`JwtAuthenticationFilter`), to ~16,000 requests/din tak free chalega. Usage tab dekhte rahein.
- `application-prod.yml` mein `database: 1` likha hai, par Upstash par database `0` hi hota hai, isliye env variable se override karein

| Env variable | Value |
|---|---|
| `SPRING_DATA_REDIS_HOST` | `singular-malamute-245096.upstash.io` |
| `SPRING_DATA_REDIS_PORT` | `6379` |
| `SPRING_DATA_REDIS_USERNAME` | `default` |
| `REDIS_PASSWORD` | Upstash page ka Token |
| `SPRING_DATA_REDIS_SSL_ENABLED` | `true` |
| `SPRING_DATA_REDIS_DATABASE` | `0` |

## 6. AppSail ke saare env variables (abhi tak ki list)

```
SPRING_PROFILES_ACTIVE=prod
SPRING_DATASOURCE_URL=...
SPRING_DATASOURCE_USERNAME=...
SPRING_DATASOURCE_PASSWORD=...
SPRING_DATA_REDIS_HOST=...
SPRING_DATA_REDIS_PORT=6379
SPRING_DATA_REDIS_USERNAME=default
REDIS_PASSWORD=...
SPRING_DATA_REDIS_SSL_ENABLED=true
SPRING_DATA_REDIS_DATABASE=0
```

Iske alawa JWT secret, Mail (SMTP) aur Razorpay ke keys bhi lagenge. Unke exact naam `application-prod.yml` dekhne par pata chalenge (abhi pending).

## 7. Deploy ke steps (aapke computer par)

1. Catalyst CLI install: `npm i -g zcatalyst-cli`
2. `catalyst login` (data center **IN - India** chunein). Login ho chuka hai.
3. Repo folder mein project link:
   ```
   catalyst init --org 60093691756 -p 104456000000013051 -ni
   ```
4. Jar build (PowerShell): `.\gradlew.bat bootJar`
5. `build/libs` mein `-plain.jar` wala nahi, bada jar `appsail/app.jar` naam se rakhein
6. `appsail/app-config.json` (stack aur memory Catalyst docs se verify kar lein):
   ```json
   {
     "command": "java -jar app.jar",
     "build_path": "app.jar",
     "stack": "java17",
     "memory": 1024
   }
   ```
7. Port: `server.port: ${X_ZOHO_CATALYST_LISTEN_PORT:8080}` (agar prod yml mein pehle se na ho)
8. Catalyst console mein AppSail ke env variables daalein (section 6)
9. `catalyst deploy`
10. Deploy ke baad Android app mein `BASE_URL` AppSail ka URL rakhein

Note: Node.js `v24.12.0` purana hai (October 2026 security patch). Deploy ruka nahi, par `v24.22.1` ya naya install kar lein.

## 8. MCQ CSV import ka recommended format

```
exam,year,subject,topic,question_hi,question_en,option_a,option_b,option_c,option_d,correct_option,explanation,difficulty
```

- UTF-8, comma separated, text quotes mein
- `correct_option`: A/B/C/D
- `difficulty`: EASY/MEDIUM/HARD
- Duplicate check aur row-wise error report

(Aapka importer abhi kaunse columns padhta hai, wo repo ke code se match kar lein.)

## 9. Production checklist

- [ ] Saare secrets env variables mein, repo mein nahi
- [ ] Repo public hai: purani commits mein kabhi secret gaya ho (Razorpay, JWT, DB password) to rotate karein
- [ ] `spring.profiles.active=prod`, `ddl-auto=validate`
- [ ] Flyway pehli baar Neon par migrations chalayega, ye logs mein check karein
- [ ] Neon DB ka backup/restore plan
- [ ] Health check ke liye actuator dependency jodna (ya `/v3/api-docs` jaisa koi khula endpoint)
- [ ] CORS sirf apne domain tak
- [ ] Redis commands limit ki monitoring (Upstash Usage tab)
- [ ] Android release build: signed AAB, production `BASE_URL`

## 10. Abhi ka status aur agla kaam

**Ban chuka hai:** Catalyst project, Neon Postgres, Upstash Redis, IN MCP connector, CLI login.

**Pending:**
1. `application-prod.yml` ka content (secrets hata kar) bhejna, taaki JWT/Mail/Razorpay ke env naam final ho
2. Jar build, `catalyst init`, `catalyst deploy`
3. Env variables Catalyst console mein daalna
4. Deploy ke baad API test aur Android `BASE_URL` update

## 11. Alternative: Linux VPS (Docker + Nginx)

Agar Catalyst mein dikkat aaye to ye bhi option hai, kyunki repo ka `docker-compose.yml` (Postgres 16 + Redis 7) wahan seedha chalta hai:
- Ubuntu server, Docker, Nginx, Certbot (HTTPS)
- Backend ka Dockerfile (multi-stage Gradle build, `eclipse-temurin:17`)
- Nginx `proxy_pass` to `127.0.0.1:8080`
- Daily `pg_dump` backup

## 12. Chat mein jo galtiyan sudhari gayi

- Pehle `/actuator/health` bataya tha, par repo mein actuator nahi hai
- Pehle `application-prod.yml` naya banane ko kaha tha, par wo repo mein pehle se hai (overwrite mat karna)
- Pehle laga Redis hata sakte hain, par OTP, session aur rate limiting Redis par chalte hain
- Chrome tool se Catalyst console `.com` par khul raha tha, jabki account `.in` par hai
