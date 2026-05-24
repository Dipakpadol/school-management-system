$ErrorActionPreference = "Stop"

$repoRoot = Split-Path -Parent $PSScriptRoot
$logDir = Join-Path $repoRoot "logs"
New-Item -ItemType Directory -Force $logDir | Out-Null

$env:JAVA_HOME = "C:\Program Files\Java\jdk-21.0.4"
$env:Path = "$env:JAVA_HOME\bin;$env:Path"
$env:SERVER_PORT = "8080"
$env:SPRING_PROFILES_ACTIVE = "localqa"

Set-Location $repoRoot

.\mvnw.cmd `
    "-Dspring-boot.run.profiles=localqa" `
    "-Dspring-boot.run.useTestClasspath=true" `
    spring-boot:run `
    *> (Join-Path $logDir "backend-demo.out.log")
