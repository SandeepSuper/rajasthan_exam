# Rajasthan Exam MCP Server

Ye ek MCP (Model Context Protocol) server hai jo hamare Spring Boot backend ki REST APIs ko **tools** ke roop mein Claude ko deta hai. Isse Claude seedha exams, tests, questions, payments, reports, news aur community ka data padh sakta hai.

- Language: TypeScript, `@modelcontextprotocol/sdk` + `zod`
- Transport: abhi sirf **stdio** (Claude Desktop / Claude Code ke liye)
- Default mein sirf **read-only (GET)** tools on hain. Write/delete tools tabhi on honge jab `ENABLE_WRITE_TOOLS=true` ho.

---

## ⚠️ Zaroori note: admin session conflict

Backend Redis mein **ek admin ka sirf ek active session** rakhta hai (`session:<username>`, dekhein `AdminAuthController.kt`).

- Jab MCP server admin login karta hai, to **browser wala admin panel logout ho jaata hai**.
- Jab aap browser mein admin login karte hain, to MCP ka token invalid ho jaata hai. MCP agle call par 401/403 dekh kar **apne aap dobara login** kar leta hai, jisse browser fir se logout ho jaata hai.

Matlab dono ek saath use karoge to ek-doosre ko logout karte rahenge. Iska sahi fix hai MCP ke liye **alag admin user**, lekin uske liye **backend mein badlav** chahiye (abhi sirf ek hi admin username/password support hai). Ye baad ka kaam hai.

Tip: public tools (`list_news`, `list_notifications`, `list_top_referrers`, `get_public_app_config`) login nahi karte, to inse browser session par koi asar nahi padta.

---

## Setup aur run kaise karein

Node.js 20.12+ chahiye (hum Node 24 par test kar rahe hain).

```powershell
cd mcp-server
npm install
copy .env.example .env      # phir .env mein asli values bharein
npm run build
npm start                   # stdio par chalta hai; Ctrl+C se band karein
```

`npm start` chalane par terminal mein kuch "dikhega" nahi, kyunki stdio par server Claude ka wait karta hai. Logs **stderr** par aate hain (`[rajasthan-exam-mcp] ...`). Ye normally aap khud nahi chalayenge, Claude Desktop / Claude Code ise khud start karta hai.

---

## Environment variables

| Variable | Zaroori? | Matlab |
|---|---|---|
| `API_BASE_URL` | Haan | Backend ka URL, jaise `https://<app>.development.catalystappsail.in` (https, URL mein password nahi) |
| `ADMIN_USERNAME` | Admin tools ke liye | Admin login username |
| `ADMIN_PASSWORD` | Admin tools ke liye | Admin login password |
| `ENABLE_WRITE_TOOLS` | Nahi | `true` hone par hi write/delete tools register hote hain. Default: off |
| `MCP_TRANSPORT` | Nahi | `stdio` (default, Claude Code/Desktop) ya `http` (claude.ai connector) |
| `MCP_PATH_SECRET` | `http` mode mein haan | Secret path, endpoint `/mcp/<secret>` banta hai. Kam se kam 32 characters |
| `REQUEST_TIMEOUT_MS` | Nahi | Har request ka timeout, default `60000` (cold start ke liye) |
| `MAX_OUTPUT_CHARS` | Nahi | Tool output ki max length, default `50000` (zyada ho to truncate) |

Values `mcp-server/.env` se padhi jaati hain, ya Claude config ke `env` block se (env block wali value jeet-ti hai). `.env` git mein commit nahi hota.

---

## Claude Code mein add karna

Pehle `npm run build` kar lein. Phir repo folder se (path apne hisaab se badlein):

```powershell
claude mcp add rajasthan-exam -- node "C:\Users\pc\Documents\project\rajasthan_exam_backend\backend\mcp-server\dist\index.js"
```

Agar `.env` file bhari hui hai to itna kaafi hai. Ya env variables seedhe de sakte hain:

```powershell
claude mcp add rajasthan-exam `
  -e API_BASE_URL=https://<app>.development.catalystappsail.in `
  -e ADMIN_USERNAME=<username> `
  -e ADMIN_PASSWORD=<password> `
  -- node "C:\...\mcp-server\dist\index.js"
```

Check karne ke liye: `claude mcp list`, aur Claude Code ke andar `/mcp`.

## Claude Desktop mein add karna

`%APPDATA%\Claude\claude_desktop_config.json` kholein (Settings → Developer → Edit Config) aur ye jodein:

```json
{
  "mcpServers": {
    "rajasthan-exam": {
      "command": "node",
      "args": ["C:\\Users\\pc\\Documents\\project\\rajasthan_exam_backend\\backend\\mcp-server\\dist\\index.js"]
    }
  }
}
```

Credentials `.env` file mein rakhein, ya `"env": { "API_BASE_URL": "...", ... }` block jodein. Phir Claude Desktop ko poori tarah band karke dobara kholein.

## claude.ai connector (web / mobile) ke saath

claude.ai sirf **remote HTTPS MCP server** se connect hota hai. Iske liye server ko `MCP_TRANSPORT=http` mode mein deploy karte hain (streamable HTTP, stateless). Endpoint hota hai:

```
https://<mcp-app-url>/mcp/<MCP_PATH_SECRET>
```

Baaki har path par `404` aata hai, aur `/` par `ok` (health check).

### 1. Secret banayein

```powershell
-join ((48..57)+(65..90)+(97..122) | Get-Random -Count 48 | ForEach-Object {[char]$_})
```

Ye secret **password jaisa** hai: kisi ko na bhejein, git mein na daalein. Leak ho jaaye to naya bana kar AppSail mein badal dein aur connector dobara add karein.

### 2. Catalyst AppSail par deploy (naya Node.js app)

```powershell
cd mcp-server
npm install
npm run build
Compress-Archive -Path dist, node_modules, package.json -DestinationPath mcp-server.zip -Force
```

Catalyst console → AppSail → naya app:
- Stack: **Node.js** (20 ya upar)
- Upload: `mcp-server.zip`
- Startup command: `node dist/index.js`
- Environment variables:

| Variable | Value |
|---|---|
| `MCP_TRANSPORT` | `http` |
| `MCP_PATH_SECRET` | step 1 wala secret |
| `API_BASE_URL` | backend ka URL |
| `ADMIN_USERNAME` / `ADMIN_PASSWORD` | admin login |
| `ENABLE_WRITE_TOOLS` | `false` (jab tak zaroorat na ho) |

Port AppSail khud deta hai (`X_ZOHO_CATALYST_LISTEN_PORT`), use set karne ki zaroorat nahi. Deploy ke baad browser mein `https://<mcp-app-url>/` kholein, `ok` dikhna chahiye.

### 3. claude.ai mein connector jodein

claude.ai → Settings → Connectors → **Add custom connector**:
- Name: `Rajasthan Exam`
- URL: `https://<mcp-app-url>/mcp/<MCP_PATH_SECRET>`
- OAuth fields khaali chhod dein

Phir chat mein "+" / tools menu se connector on karein aur poochein, jaise "saare exams dikhao".

### Dhyan rakhein
- Secret URL ek simple protection hai: jiske paas poora URL hai wo admin data padh sakta hai. Production ke liye OAuth behtar hoga.
- Cold start: MCP app aur backend dono so sakte hain, to pehli call mein 1 minute tak lag sakta hai.
- Upar wala admin session conflict yahan bhi laagu hai: connector use karne par browser admin panel logout hoga.

---

## Tools

### Read-only (hamesha on), 22 tools

| Tool | API |
|---|---|
| `list_exams` | GET `/api/tests/exams` |
| `list_tests` | GET `/api/tests?examId=` |
| `get_test` | GET `/api/tests/{testId}` |
| `list_live_tests` | GET `/api/tests/live` |
| `list_questions` | GET `/api/tests/questions?testId=` |
| `get_content_overview` | GET `/api/admin/content/overview` |
| `search_users` | GET `/api/admin/users/search?q=` |
| `list_top_referrers` | GET `/api/users/top-referrers` (public) |
| `get_leaderboard` | GET `/api/leaderboard?testId=` |
| `get_payment_stats` | GET `/api/admin/payments/stats` |
| `list_orders` | GET `/api/admin/payments/orders` |
| `list_exam_pricing` | GET `/api/admin/payments/exams` |
| `list_question_reports` | GET `/api/admin/reports` |
| `list_news` | GET `/api/news` (public) |
| `list_notifications` | GET `/api/notifications` (public) |
| `list_community_posts` | GET `/api/community/posts` |
| `list_post_comments` | GET `/api/community/posts/{postId}/comments` |
| `get_community_stats` | GET `/api/admin/community/stats` |
| `list_blocked_users` | GET `/api/admin/community/users/blocked` |
| `get_user_rate_limit` | GET `/api/admin/community/rate-limit/{userId}` |
| `get_admin_app_config` | GET `/api/admin/config` |
| `get_public_app_config` | GET `/api/config` (public) |

### Write / delete (sirf `ENABLE_WRITE_TOOLS=true` par), 19 tools

Inke description mein "⚠️ MODIFIES DATA" likha hai, aur ye **live backend ka data badalte hain**.

`create_exam`, `update_exam`, `set_exam_discount`, `set_exam_price`, `create_test`, `create_question`, `import_questions` (local .csv/.xlsx/.xls file upload), `update_app_config` (pehle current config padh kar merge karta hai), `delete_community_post`, `delete_community_comment`, `verify_community_post`, `block_user`, `unblock_user`, `reset_user_rate_limit`, `create_news`, `bulk_import_news`, `delete_news`, `send_notification`, `delete_notification`.

Jaan-boojh kar **shaamil nahi**: user auth/OTP endpoints, payment create/verify, test submit, seed endpoints, file upload, aur wo endpoints jinhe asli user ka JWT chahiye (profile, bookmarks, history, referrals).

---

## Auth, retry aur security

- Pehli admin call par `ADMIN_USERNAME/ADMIN_PASSWORD` se login hota hai. JWT **sirf memory mein** rehta hai, disk ya logs mein kabhi nahi likha jaata.
- **401 ya 403** milne par ek baar dobara login karke request retry hoti hai (Spring expired/replaced token par 403 deta hai).
- Timeout 60 second. Cold start (20-40s) ke liye ek retry: GET ko timeout/network error/502-504 par, write ko sirf network error/502-504 par (timeout wali write shayad already chal chuki ho, isliye dobara nahi bhejte).
- Errors readable text mein aate hain, jaise `HTTP 404 Not Found: Exam not found`. Password, JWT aur `Bearer ...` output/logs se hata diye jaate hain, aur response JSON mein `password`/`token`/`secret` jaisi keys `[REDACTED]` ho jaati hain.

## Backend ke liye baad ke kaam (abhi kuch badla nahi gaya)

1. `SecurityConfig.kt` mein `/api/admin/**` abhi `permitAll()` hai, yani admin APIs bina login ke bhi chalti hain. Production se pehle ise admin-only karna chahiye.
2. MCP ke liye alag admin user (upar wala session conflict khatam karne ke liye).
3. `AdminAuthController.kt` mein default admin password code mein likha hai. AppSail par `ADMIN_PASSWORD` zaroor set rakhein.
