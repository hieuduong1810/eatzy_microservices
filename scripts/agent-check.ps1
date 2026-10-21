param(
    [ValidateSet('doctor', 'test', 'artifacts', 'config', 'health')]
    [string]$Mode = 'doctor',
    [string]$Service,
    [string]$JavaHome
)

$ErrorActionPreference = 'Stop'
$repoRoot = Split-Path $PSScriptRoot -Parent
$originalJava = $env:JAVA_HOME
$originalPath = $env:PATH
Push-Location $repoRoot

function Invoke-Checked {
    param([string]$Command, [string[]]$CommandArgs)
    & $Command @CommandArgs
    if ($LASTEXITCODE -ne 0) { throw "$Command failed with exit code $LASTEXITCODE" }
}

function Select-Java17 {
    $candidates = @($JavaHome, $env:JAVA_HOME, (Join-Path $env:ProgramFiles 'Java\jdk-17'))
    $adoptium = Join-Path $env:ProgramFiles 'Eclipse Adoptium'
    if (Test-Path $adoptium) {
        $candidates += @(Get-ChildItem $adoptium -Directory -Filter 'jdk-17*' | ForEach-Object FullName)
    }
    foreach ($candidate in $candidates) {
        if (-not $candidate) { continue }
        $release = Join-Path $candidate 'release'
        if ((Test-Path $release) -and (Select-String -Path $release -Pattern '^JAVA_VERSION="17\.' -Quiet)) {
            $env:JAVA_HOME = $candidate
            $env:PATH = "$(Join-Path $candidate 'bin');$originalPath"
            return
        }
    }
    throw 'JDK 17 not found. Set JAVA_HOME or pass -JavaHome with an installed JDK 17 directory.'
}

try {
    switch ($Mode) {
        'doctor' {
            Select-Java17
            Invoke-Checked 'java' @('-version')
            Invoke-Checked 'docker' @('version', '--format', '{{.Server.Version}}')
            Invoke-Checked 'docker' @('compose', 'version')
            [pscustomobject]@{
                LocalEnvPresent = Test-Path '.env'
                DefaultAuthKeystorePresent = Test-Path 'eatzy-auth-service/src/main/resources/eatzy.jks'
                DockerJarCount = @(Get-ChildItem 'docker-artifacts' -Filter '*.jar' -ErrorAction SilentlyContinue).Count
            } | Format-List
        }
        'test' {
            Select-Java17
            $task = 'test'
            if ($Service) {
                if ($Service -notmatch '^eatzy-[a-z0-9-]+$' -or -not (Test-Path (Join-Path $Service 'build.gradle.kts'))) {
                    throw 'Service must name an existing Gradle module, such as eatzy-order-service.'
                }
                $task = ":${Service}:test"
            }
            Invoke-Checked '.\gradlew.bat' @($task, '--parallel', '--continue', '--no-daemon')
        }
        'artifacts' {
            Select-Java17
            Invoke-Checked '.\gradlew.bat' @('prepareDockerArtifacts', '--parallel', '--no-daemon')
            $count = @(Get-ChildItem 'docker-artifacts' -Filter '*.jar').Count
            if ($count -ne 11) { throw "Expected 11 Docker JARs; found $count." }
            Write-Output 'All 11 executable Docker JARs are present.'
        }
        'config' {
            # --quiet validates without dumping interpolated secrets.
            Invoke-Checked 'docker' @('compose', '-f', 'docker-compose.yml', 'config', '--quiet')
            Invoke-Checked 'docker' @('compose', '-f', 'docker-compose.prod.yml', 'config', '--quiet')
            Write-Output 'Both compose files are valid. Production reachability was not tested.'
        }
        'health' {
            $services = [ordered]@{
                gateway = 'http://127.0.0.1:8080/actuator/health'
                auth = 'http://127.0.0.1:8081/actuator/health'
                restaurant = 'http://127.0.0.1:8082/actuator/health'
                order = 'http://127.0.0.1:8083/actuator/health'
                communication = 'http://127.0.0.1:8084/actuator/health'
                cart = 'http://127.0.0.1:8085/actuator/health'
                payment = 'http://127.0.0.1:8086/actuator/health'
                interaction = 'http://127.0.0.1:8087/actuator/health'
                system = 'http://127.0.0.1:8088/actuator/health'
                ai = 'http://127.0.0.1:8089/api/v1/ai/health'
                discovery = 'http://127.0.0.1:8761/actuator/health'
                config = 'http://127.0.0.1:8888/actuator/health'
            }
            $failed = @()
            foreach ($name in $services.Keys) {
                try {
                    $response = Invoke-RestMethod -Uri $services[$name] -TimeoutSec 10
                    if ($response.status -ne 'UP' -and $response.status -ne 'healthy') { throw 'Health is not UP/healthy.' }
                    Write-Output "${name}: $($response.status)"
                } catch { $failed += $name; Write-Output "${name}: unavailable" }
            }
            if ($failed.Count) { throw "Unhealthy services: $($failed -join ', '). No containers were started or changed." }
        }
    }
} finally {
    $env:JAVA_HOME = $originalJava
    $env:PATH = $originalPath
    Pop-Location
}
