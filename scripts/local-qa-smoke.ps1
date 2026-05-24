param(
    [string] $BaseUrl = "http://localhost:8080/api",
    [string] $Email = "admin@school.test",
    [string] $Password = "Admin@12345678",
    [int] $TimeoutSeconds = 90
)

$ErrorActionPreference = "Stop"

function Invoke-Json {
    param(
        [string] $Method,
        [string] $Uri,
        [object] $Body = $null,
        [string] $Token = $null
    )

    $headers = @{}
    if ($Token) {
        $headers.Authorization = "Bearer $Token"
    }

    $parameters = @{
        Method = $Method
        Uri = $Uri
        Headers = $headers
    }
    if ($null -ne $Body) {
        $parameters.ContentType = "application/json"
        $parameters.Body = ($Body | ConvertTo-Json -Depth 12)
    }

    Invoke-RestMethod @parameters
}

$deadline = (Get-Date).AddSeconds($TimeoutSeconds)
do {
    try {
        Invoke-Json -Method Get -Uri "$BaseUrl/v1/system/status" | Out-Null
        break
    }
    catch {
        if ((Get-Date) -gt $deadline) {
            throw "Backend did not become ready at $BaseUrl within $TimeoutSeconds seconds."
        }
        Start-Sleep -Seconds 2
    }
} while ($true)

$login = $null
do {
    try {
        $login = Invoke-Json -Method Post -Uri "$BaseUrl/v1/auth/login" -Body @{
            email = $Email
            password = $Password
        }
        break
    }
    catch {
        if ((Get-Date) -gt $deadline) {
            throw
        }
        Start-Sleep -Seconds 2
    }
} while ($true)
$token = $login.data.accessToken

$me = Invoke-Json -Method Get -Uri "$BaseUrl/v1/auth/me" -Token $token
$students = Invoke-Json -Method Get -Uri "$BaseUrl/v1/students?size=10" -Token $token
$users = Invoke-Json -Method Get -Uri "$BaseUrl/v1/users?size=20" -Token $token
$categories = Invoke-Json -Method Get -Uri "$BaseUrl/v1/fees/categories?size=20" -Token $token
$structures = Invoke-Json -Method Get -Uri "$BaseUrl/v1/fees/structures?size=20" -Token $token
$assignments = Invoke-Json -Method Get -Uri "$BaseUrl/v1/fees/assignments?size=20" -Token $token
$defaulters = Invoke-Json -Method Get -Uri "$BaseUrl/v1/fees/reports/defaulters?size=20" -Token $token
$auditLogs = Invoke-Json -Method Get -Uri "$BaseUrl/v1/audit-logs?size=20" -Token $token

$assignment = $assignments.data.content | Select-Object -First 1
$receipt = $null
if ($assignment -and ([decimal] $assignment.balanceAmount) -gt 0) {
    $amount = [Math]::Min([decimal] 1000.00, [decimal] $assignment.balanceAmount)
    $receipt = Invoke-Json `
        -Method Post `
        -Uri "$BaseUrl/v1/fees/assignments/$($assignment.id)/payments" `
        -Token $token `
        -Body @{
            amount = $amount
            paymentDate = (Get-Date).ToString("yyyy-MM-dd")
            paymentMode = "CASH"
            payerName = "Local QA Cash Desk"
            collectedBy = "localqa"
            remarks = "Automated local QA smoke payment."
            assessLateFee = $false
        }
    Invoke-Json -Method Get -Uri "$BaseUrl/v1/fees/receipts/$($receipt.data.receiptNumber)" -Token $token | Out-Null
}

[PSCustomObject]@{
    User = $me.data.email
    Roles = ($me.data.roles -join ",")
    Students = $students.data.totalElements
    Users = $users.data.totalElements
    FeeCategories = $categories.data.totalElements
    FeeStructures = $structures.data.totalElements
    FeeAssignments = $assignments.data.totalElements
    Defaulters = $defaulters.data.totalElements
    AuditLogs = $auditLogs.data.totalElements
    ReceiptNumber = if ($receipt) { $receipt.data.receiptNumber } else { "not-created" }
    Result = "PASS"
}
