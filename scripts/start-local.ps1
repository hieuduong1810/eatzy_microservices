param(
    [string]$JavaHome,
    [switch]$SkipBuild,
    [switch]$BuildOnly
)

$ErrorActionPreference = 'Stop'
$repoRoot = Split-Path -Parent $PSScriptRoot
$originalJavaHome = $env:JAVA_HOME
$originalPath = $env:PATH

function Assert-NativeCommand([string]$Step) {
    if ($LASTEXITCODE -ne 0) {
        throw "$Step failed (exit code $LASTEXITCODE)."
    }
}

Push-Location $repoRoot
try {
    if (-not (Test-Path -LiteralPath '.env' -PathType Leaf)) {
        throw 'Missing .env. Configure database, Redis password and application credentials first.'
    }

    & docker info --format '{{.ServerVersion}}' | Out-Null
    Assert-NativeCommand 'Docker Desktop connection'
    & docker compose config --quiet
    Assert-NativeCommand 'Compose configuration'

    $composeJson = & docker compose config --format json | Out-String
    Assert-NativeCommand 'Reading Compose configuration'
    $composeConfig = $composeJson | ConvertFrom-Json
    $keyMount = $composeConfig.services.'eatzy-auth-service'.volumes |
        Where-Object { $_.target -eq '/app/eatzy.jks' } |
        Select-Object -First 1
    if (-not $keyMount -or -not (Test-Path -LiteralPath $keyMount.source -PathType Leaf)) {
        throw 'JWT keystore is missing. Set JWT_KEYSTORE_FILE to an existing .jks file.'
    }

    if (-not $SkipBuild) {
        $javaCandidates = @($JavaHome, $env:JAVA_HOME, (Join-Path $env:ProgramFiles 'Java\jdk-17'))
        $adoptiumRoot = Join-Path $env:ProgramFiles 'Eclipse Adoptium'
        if (Test-Path -LiteralPath $adoptiumRoot) {
            $javaCandidates += @(Get-ChildItem -LiteralPath $adoptiumRoot -Directory -Filter 'jdk-17*' |
                Select-Object -ExpandProperty FullName)
        }
        $selectedJavaHome = $null
        foreach ($candidate in $javaCandidates) {
            if ([string]::IsNullOrWhiteSpace($candidate)) { continue }
            $releaseFile = Join-Path $candidate 'release'
            $javaExecutable = Join-Path $candidate 'bin\java.exe'
            if ((Test-Path -LiteralPath $releaseFile -PathType Leaf) -and
                (Test-Path -LiteralPath $javaExecutable -PathType Leaf) -and
                ([System.IO.File]::ReadAllText($releaseFile) -match '(?m)^JAVA_VERSION="17(?:\.|")')) {
                $selectedJavaHome = $candidate
                break
            }
        }
        if (-not $selectedJavaHome) {
            throw 'Java 17 was not found. Install JDK 17 or pass -JavaHome "C:\path\to\jdk-17".'
        }

        $env:JAVA_HOME = $selectedJavaHome
        $env:PATH = (Join-Path $selectedJavaHome 'bin') + [IO.Path]::PathSeparator + $originalPath

        Write-Host 'Preparing 11 executable JARs...'
        & .\gradlew.bat prepareDockerArtifacts --parallel --no-daemon
        Assert-NativeCommand 'Gradle Docker artifacts'

        # Jenkins also builds sequentially on Windows to avoid BuildKit storage races.
        Write-Host 'Building Docker images sequentially...'
        $buildServices = @($composeConfig.services.PSObject.Properties |
            Where-Object { $null -ne $_.Value.build } |
            Select-Object -ExpandProperty Name)
        foreach ($buildService in $buildServices) {
            & docker compose build $buildService
            Assert-NativeCommand ("Docker image build: " + $buildService)
        }
    }

    if (-not $BuildOnly) {
        Write-Host 'Starting local services and waiting for health checks...'
        & docker compose up -d --no-build --wait --wait-timeout 300
        Assert-NativeCommand 'Docker Compose startup'
        & docker compose ps
        Assert-NativeCommand 'Docker Compose status'
    }
}
finally {
    $env:JAVA_HOME = $originalJavaHome
    $env:PATH = $originalPath
    Pop-Location
}
