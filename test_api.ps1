# Base URL
$BASE_URL = "http://localhost:8080/api"

# 1. Auth: Send OTP
Write-Host "1. Sending OTP to 9876543210..." -ForegroundColor Cyan
Invoke-RestMethod -Uri "$BASE_URL/auth/send-otp" -Method Post -Body (@{ mobile="9876543210" } | ConvertTo-Json) -ContentType "application/json"

# 2. Auth: Verify OTP (Verify generated OTP, assumes we intercepted it or using fixed mocked one for now, but let's try assuming it works or we need to look at logs. Actually, for this script to work without manual intervention, I should probably mock the OTP in dev mode or just print instructions. For now, let's just show the command)
# NOTE: In a real scenario, we'd need to fetch the OTP from Redis or logs. 
# For this script to be runnable, let's assume the user has to input it, OR we can't fully automate this step without a backdoor.
# check logs for OTP if needed.

Write-Host "`nTo verify, we need the OTP. Since we can't easily grab it from Redis in this script without extra tools, here is the command you would run:" -ForegroundColor Yellow
Write-Host "Invoke-RestMethod -Uri '$BASE_URL/auth/verify-otp' -Method Post -Body (@{ mobile='9876543210'; otp='<OTP_FROM_LOGS>' } | ConvertTo-Json) -ContentType 'application/json'" -ForegroundColor White

# 3. Admin: Create Exam (Simulated)
# We need a token for this. 
# Let's just output the commands for the user.

Write-Host "`n--- Sample Commands to Run in PowerShell ---" -ForegroundColor Green

Write-Host "`n# 1. Login & Get Token" -ForegroundColor Cyan
Write-Host '$response = Invoke-RestMethod -Uri "http://localhost:8080/api/auth/verify-otp" -Method Post -Body (@{ mobile="9876543210"; otp="1234" } | ConvertTo-Json) -ContentType "application/json"'
Write-Host '$token = $response.token'

Write-Host "`n# 2. Create Exam (Admin)" -ForegroundColor Cyan
Write-Host 'Invoke-RestMethod -Uri "http://localhost:8080/api/admin/exams" -Method Post -Headers @{ Authorization="Bearer $token" } -Body (@{ title="REET Level 1"; category="TEACHING"; iconUrl="http://img.com/reet.png"; languageSupported="BOTH" } | ConvertTo-Json) -ContentType "application/json"'

Write-Host "`n# 3. Get Exams (Client)" -ForegroundColor Cyan
Write-Host 'Invoke-RestMethod -Uri "http://localhost:8080/api/tests?examId=<EXAM_UUID>" -Method Get -Headers @{ Authorization="Bearer $token" }'

Write-Host "`n--------------------------------------------" -ForegroundColor Green
