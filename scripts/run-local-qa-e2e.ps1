param(
    [int] $Port = 8080,
    [string] $JavaHome = "C:\Program Files\Java\jdk-21.0.4",
    [int] $TimeoutSeconds = 180
)

$ErrorActionPreference = "Stop"

$repoRoot = Split-Path -Parent $PSScriptRoot
$backend = & (Join-Path $PSScriptRoot "start-local-qa-backend.ps1") -Port $Port -JavaHome $JavaHome

try {
    & (Join-Path $PSScriptRoot "local-qa-smoke.ps1") `
        -BaseUrl "http://localhost:$Port/api" `
        -TimeoutSeconds $TimeoutSeconds
}
finally {
    $listeners = Get-NetTCPConnection -LocalPort $Port -State Listen -ErrorAction SilentlyContinue
    foreach ($listener in $listeners) {
        Stop-Process -Id $listener.OwningProcess -Force -ErrorAction SilentlyContinue
    }
    Stop-Process -Id $backend.Pid -Force -ErrorAction SilentlyContinue
}
