param(
    [int] $Port = 8080,
    [string] $JavaHome = "C:\Program Files\Java\jdk-21.0.4"
)

$ErrorActionPreference = "Stop"

$repoRoot = Split-Path -Parent $PSScriptRoot
$logDir = Join-Path $repoRoot "logs"
New-Item -ItemType Directory -Force $logDir | Out-Null

if (-not (Test-Path (Join-Path $JavaHome "bin\java.exe"))) {
    throw "Java 21 was not found at '$JavaHome'. Pass -JavaHome with a valid JDK 21 path."
}

$outLog = Join-Path $logDir "localqa-backend.out.log"
$errLog = Join-Path $logDir "localqa-backend.err.log"
$runner = Join-Path $logDir "run-localqa-backend.cmd"
$runnerLines = @(
    "@echo off",
    "set ""JAVA_HOME=$JavaHome""",
    "set ""PATH=$JavaHome\bin;%PATH%""",
    "set ""SERVER_PORT=$Port""",
    "set ""SPRING_PROFILES_ACTIVE=localqa""",
    "cd /d ""$repoRoot""",
    "call ""$repoRoot\mvnw.cmd"" ""-Dspring-boot.run.profiles=localqa"" ""-Dspring-boot.run.useTestClasspath=true"" spring-boot:run > ""$outLog"" 2> ""$errLog"""
)
Set-Content -Path $runner -Value $runnerLines -Encoding ASCII

$process = Start-Process `
    -FilePath $runner `
    -WorkingDirectory $repoRoot `
    -WindowStyle Hidden `
    -PassThru

[PSCustomObject]@{
    Pid = $process.Id
    BaseUrl = "http://localhost:$Port/api"
    Profile = "localqa"
    Log = $outLog
    ErrorLog = $errLog
}
