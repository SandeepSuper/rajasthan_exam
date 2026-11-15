# Claude Connector Setup Guide (Rajasthan Exam Backend)

Is guide mein poora process hai: ab tak kya ho chuka hai, aur claude.ai ke connector se apne backend ko jodne ke liye **aapko kya-kya karna hai**.

---

## 1. Ye sab kaise kaam karta hai

```
claude.ai (Connector)
      │  HTTPS:  https://<mcp-app-url>/mcp/<SECRET>
      ▼
MCP Server  (naya AppSail app, Node.js, is folder ka code)
      │  admin login (JWT memory mein) + REST API calls
      ▼
Backend API  (purana AppSail app, Spring Boot)
      │
      ▼
Neon (Postgres) + Upstash (Redis)
```

- claude.ai aapke PC se seedha connect **nahi** kar sakta. Isliye MCP server ko internet par (HTTPS) deploy karna padta hai.
- MCP server backend ki APIs ko "tools" bana kar Claude ko deta hai.
- URL mein ek lamba **secret** hota hai. Sahi secret ke bina har request ka jawab `404` hota hai.

---

## 2. Ab tak kya ho chuka hai ✅

| Kaam | Status |
|---|---|
| Backend AppSail par chal raha hai (Swagger khulta hai) | ✅ |
| `SmsService` bean ki 500 error ki wajah mili: `MockSmsService` sirf `dev`, `prod`, `default` profile par chalta hai | ✅ |
| Fix tay hua: `SPRING_PROFILES_ACTIVE=prod,catalyst` | ⬜ AppSail mein lagana / check karna baaki (agar abhi tak nahi kiya) |
| Saari APIs ki list aur read/write ka batwara | ✅ |
| MCP server (TypeScript) `mcp-server/` folder mein bana | ✅ |
| 22 read-only tools (hamesha on) | ✅ |
| 19 write/delete tools (sirf `ENABLE_WRITE_TOOLS=true` par) | ✅ |
| stdio mode (Claude Code / Desktop) local par test hua | ✅ |
| HTTP mode (claude.ai connector) local par test hua: health `ok`, galat secret par `404`, `list_news` chala | ✅ |
| Admin login + `list_exams` ka test | ⬜ baaki (credentials ke saath karna hai) |
| MCP server ka AppSail deploy | ⬜ baaki |
| claude.ai mein connector jodna | ⬜ baaki |

Backend ka koi code nahi badla gaya. Kuch bhi commit / push nahi hua hai.

---

## 3. Aapko kya karna hai (checklist)

- [ ] **Step 0:** Backend ka profile fix (agar abhi tak nahi kiya)
- [ ] **Step 1:** Secret banana
- [ ] **Step 2:** Zip banana
- [ ] **Step 3:** AppSail par naya MCP app deploy karna
- [ ] **Step 4:** Health check (`ok`)
- [ ] **Step 5:** claude.ai mein connector jodna
- [ ] **Step 6:** Chat mein test karna
- [ ] **Step 7:** (Optional) git commit karna

---

### Step 0: Backend ka profile fix

Agar Swagger par `/v3/api-docs` abhi bhi 500 deta hai, to pehle ye karein:

1. Catalyst console → AppSail → **backend wala app** → Configuration
2. `SPRING_PROFILES_ACTIVE` ki value: `prod,catalyst` (order yahi rakhein, `catalyst` aakhir mein)
3. Naya deployment banayein: wahi jar/zip, wahi startup command, wahi saare env variables
4. Swagger kholein: `https://rajasthan-exam-api-50047311976.development.catalystappsail.in/swagger-ui/index.html`
5. Login, signup (email OTP) aur 1-2 GET APIs Swagger se chala kar dekhein (lazy init ki wajah se doosri galtiyan pehli request par hi dikhti hain)

> Ye zaroori hai, kyunki backend nahi chalega to MCP ke admin tools bhi nahi chalenge.

---

### Step 1: Secret banana

PowerShell mein:

```powershell
-join ((48..57)+(65..90)+(97..122) | Get-Random -Count 48 | ForEach-Object {[char]$_})
```

- 48 akshar ka jo text aaye, use kahin **safe** rakhein (password manager / Notepad).
- Ye **password jaisa** hai: chat, WhatsApp, git, screenshot mein na daalein.

---

### Step 2: Zip banana

```powershell
cd C:\Users\pc\Documents\project\rajasthan_exam_backend\backend\mcp-server
npm install
npm run build
Compress-Archive -Path dist, node_modules, package.json -DestinationPath mcp-server.zip -Force
```

Isse `mcp-server\mcp-server.zip` banegi. (`.gitignore` mein ye zip pehle se ignore hai.)

> Code mein kabhi bhi badlav ho, to `npm run build` aur zip dobara banana hoga, phir naya deployment.

---

### Step 3: AppSail par naya MCP app deploy karna

Catalyst console → apna project → **AppSail** → naya app banayein.
**Backend wale app ko mat chhediye**, ye alag app hai.

1. **Stack:** Node.js (20 ya upar)
2. **Upload:** `mcp-server.zip`
3. **Startup command:**
   ```
   node dist/index.js
   ```
4. **Environment variables:**

| Naam | Value |
|---|---|
| `MCP_TRANSPORT` | `http` |
| `MCP_PATH_SECRET` | Step 1 wala secret |
| `API_BASE_URL` | `https://rajasthan-exam-api-50047311976.development.catalystappsail.in` |
| `ADMIN_USERNAME` | aapka admin username |
| `ADMIN_PASSWORD` | aapka admin password |
| `ENABLE_WRITE_TOOLS` | `false` |

- Port set **nahi** karna. AppSail khud `X_ZOHO_CATALYST_LISTEN_PORT` deta hai, aur code use padh leta hai.
- `API_BASE_URL` ke aakhir mein `/` na lagayein.

5. Deploy karein.

---

### Step 4: Health check

Browser mein MCP app ka URL kholein (bina secret ke):

```
https://<mcp-app-url>/
```

- `ok` dikhe to server chal raha hai ✅
- Kuch aur dikhe to AppSail ke **logs** dekhein (neeche Troubleshooting dekhein)

---

### Step 5: claude.ai mein connector jodna

1. **claude.ai** kholein
2. Neeche baayein apne naam par click → **Settings** → **Connectors**
3. Neeche **Add custom connector** dabayein
4. Bharein:
   - **Name:** `Rajasthan Exam`
   - **Remote MCP server URL:**
     ```
     https://<mcp-app-url>/mcp/<Step-1-wala-secret>
     ```
   - **Advanced settings / OAuth fields:** khaali chhod dein
5. **Add** dabayein

> Custom connector ke liye claude.ai ka paid plan (Pro / Max / Team / Enterprise) chahiye. Team / Enterprise mein connector admin (Owner) ko jodna pad sakta hai.

---

### Step 6: Chat mein test karna

1. Naya chat kholein
2. Message box ke paas **+** (ya "Search and tools") button → **Rajasthan Exam** connector **on** karein
3. Ye poochein:
   - `saare exams dikhao`
   - `content overview batao`
   - `payment stats dikhao`
4. Claude tool chalane se pehle permission maang sakta hai. **Allow** karein.

Pehli call mein **1 minute** tak lag sakta hai, kyunki MCP app aur backend dono cold start se uth rahe hote hain. Timeout aaye to ek baar dobara poochein.

---

### Step 7: (Optional) git commit

Abhi kuch commit nahi hua hai. Jab sab chal jaaye:

```powershell
cd C:\Users\pc\Documents\project\rajasthan_exam_backend\backend
git status
git add mcp-server .gitignore
git commit -m "Add MCP server for Claude connector"
```

`git status` mein `mcp-server/.env`, `node_modules`, `dist` ya `.zip` **nahi** dikhne chahiye. Dikhein to commit na karein.

---

## 4. Claude ko kya-kya access milega

### Read-only tools (22), hamesha on

| Area | Tools |
|---|---|
| Exams / tests | `list_exams`, `list_tests`, `get_test`, `list_live_tests`, `list_questions`, `get_content_overview` |
| Users | `search_users`, `list_top_referrers`, `get_leaderboard` |
| Payments | `get_payment_stats`, `list_orders`, `list_exam_pricing` |
| Reports | `list_question_reports` |
| News / notifications | `list_news`, `list_notifications` |
| Community | `list_community_posts`, `list_post_comments`, `get_community_stats`, `list_blocked_users`, `get_user_rate_limit` |
| Settings | `get_admin_app_config`, `get_public_app_config` |

### Write / delete tools (19), default **OFF**

`create_exam`, `update_exam`, `set_exam_discount`, `set_exam_price`, `create_test`, `create_question`, `import_questions`, `update_app_config`, `delete_community_post`, `delete_community_comment`, `verify_community_post`, `block_user`, `unblock_user`, `reset_user_rate_limit`, `create_news`, `bulk_import_news`, `delete_news`, `send_notification`, `delete_notification`

On karne ke liye: AppSail mein `ENABLE_WRITE_TOOLS=true` → naya deployment → claude.ai mein connector disconnect / reconnect.
⚠️ Iske baad Claude **live data badal / delete** kar sakega. Sirf zaroorat par on karein, kaam ke baad wapas `false`.

### Jaan-boojh kar shaamil NAHI

User login / OTP / register, payment create / verify (asli paisa), test submit, seed endpoints, file upload, aur user ke apne JWT wale endpoints (profile, bookmarks, history, referrals).

---

## 5. Zaroori baatein (dhyan se padhein)

1. **Admin session conflict:** backend Redis mein ek admin ka sirf ek session rakhta hai.
   - Connector admin tool chalayega → **browser admin panel logout** ho jaayega.
   - Browser mein login karenge → connector ka token band. Connector agli call par khud dobara login kar lega (aur browser fir logout).
   - Pakka fix: MCP ke liye **alag admin user**, jiske liye backend mein badlav chahiye (baad mein).
   - Public tools (`list_news`, `list_notifications`, `list_top_referrers`, `get_public_app_config`) login nahi karte.
2. **Secret URL = password.** Jiske paas poora URL hai, wo admin data (payments, users) padh sakta hai. Leak ho to: naya secret → AppSail env update → naya deployment → claude.ai mein connector hata kar naye URL se jodein.
3. **Passwords / tokens kabhi output ya logs mein nahi aate.** JWT sirf memory mein rehta hai.
4. **Cold start:** 60 second timeout aur 1 retry pehle se set hai.

---

## 6. Troubleshooting

| Problem | Wajah / Hal |
|---|---|
| `https://<mcp-app-url>/` par `ok` nahi aata | AppSail logs dekhein. `Fatal: MCP_PATH_SECRET must be set...` aaye to secret missing hai ya 32 characters se chhota hai. `API_BASE_URL is not set` aaye to env variable missing hai. |
| claude.ai bolta hai "couldn't connect" | URL check karein: `/mcp/<secret>` poora hona chahiye, beech mein space ya aakhir mein extra text nahi. Health check `ok` hona chahiye. |
| Tool error: `ADMIN_USERNAME and ADMIN_PASSWORD must be set` | AppSail env mein dono daalein, phir naya deployment. |
| Tool error: `Admin login failed: invalid credentials` | Username / password backend ke `admin.username` / `admin.password` (AppSail env `ADMIN_USERNAME` / `ADMIN_PASSWORD`) se match nahi karte. |
| Tool error: `HTTP 500 ...` | Backend mein galti hai. Backend ke AppSail logs dekhein (Step 0 wala profile fix check karein). |
| Tool error: `Request timed out after 60s` | Cold start. 1 minute baad dobara try karein. |
| Browser admin panel baar-baar logout hota hai | Upar "Admin session conflict" dekhein. Normal hai. |
| Write tools nahi dikhte | Normal hai, default off hain (section 4). |

Kahin atak jaayein to us step ka **screenshot** ya **AppSail logs** (secret / password hata kar) Claude ko bhejein.

---

## 7. Baad ke kaam (production se pehle)

- [ ] `SecurityConfig.kt`: `/api/admin/**` abhi `permitAll()` hai, yani admin APIs **bina login ke** khuli hain. Ise admin-only banana.
- [ ] MCP ke liye alag admin user (session conflict khatam karne ke liye).
- [ ] `AdminAuthController.kt` mein default admin password code mein likha hai. AppSail par `ADMIN_PASSWORD` hamesha set rakhein, aur baad mein default hata dein.
- [ ] Mobile OTP endpoint: `MockSmsService` SMS nahi bhejta, sirf log mein likhta hai. Ise band karein ya asli SMS provider lagayein.
- [ ] Connector ke liye secret URL ki jagah **OAuth** (zyada surakshit).

---

## 8. Files kahan hain

```
mcp-server/
  CONNECTOR_SETUP.md     ← ye guide
  README.md              technical details (stdio, Claude Code / Desktop setup)
  .env.example           env variables ka template (sirf placeholders)
  package.json, tsconfig.json
  src/index.ts           start point (stdio ya http chunta hai)
  src/server.ts          tools register karta hai
  src/config.ts          env variables padhta hai
  src/api-client.ts      backend calls, login, retry, timeout
  src/redact.ts          secrets ko output / logs se hatata hai
  src/tools/read-tools.ts    22 read tools
  src/tools/write-tools.ts   19 write tools
  src/transports/stdio.ts    Claude Code / Desktop ke liye
  src/transports/http.ts     claude.ai connector ke liye
```

**Claude Code / Desktop** se (bina deploy ke, sirf apne PC par) jodna ho to `README.md` dekhein.
