# Script de Verificacao - Java 21 para RevisaLab
# Execute com: .\VERIFICAR_AMBIENTE.ps1

Write-Host "========================================" -ForegroundColor Cyan
Write-Host "  Verificacao de Ambiente - Java 21" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

$allOk = $true

# 1. Verificar JAVA_HOME
Write-Host "1. Verificando JAVA_HOME..." -ForegroundColor Yellow
$javaHome = [Environment]::GetEnvironmentVariable("JAVA_HOME", "Machine")
if (-not $javaHome) {
    $javaHome = [Environment]::GetEnvironmentVariable("JAVA_HOME", "User")
}

if ($javaHome) {
    Write-Host "   ✅ JAVA_HOME configurado: $javaHome" -ForegroundColor Green
    
    if (Test-Path $javaHome) {
        Write-Host "   ✅ Diretorio existe" -ForegroundColor Green
    } else {
        Write-Host "   ❌ ERRO: Diretorio nao existe!" -ForegroundColor Red
        $allOk = $false
    }
} else {
    Write-Host "   ❌ ERRO: JAVA_HOME nao configurado!" -ForegroundColor Red
    Write-Host "   Execute: CONFIGURAR_JAVA21.bat como Administrador" -ForegroundColor Yellow
    $allOk = $false
}

Write-Host ""

# 2. Verificar Java
Write-Host "2. Verificando Java..." -ForegroundColor Yellow
try {
    $javaVersion = & java -version 2>&1 | Select-String "version"
    Write-Host "   ✅ Java instalado: $javaVersion" -ForegroundColor Green
    
    if ($javaVersion -match "21\.") {
        Write-Host "   ✅ Versao correta (Java 21)" -ForegroundColor Green
    } else {
        Write-Host "   ⚠️  AVISO: Versao nao e Java 21" -ForegroundColor Yellow
    }
} catch {
    Write-Host "   ❌ ERRO: Java nao encontrado no PATH!" -ForegroundColor Red
    $allOk = $false
}

Write-Host ""

# 3. Verificar Maven
Write-Host "3. Verificando Maven..." -ForegroundColor Yellow
try {
    $mavenVersion = & mvn -version 2>&1 | Select-String "Apache Maven"
    Write-Host "   ✅ Maven instalado: $mavenVersion" -ForegroundColor Green
    
    $mavenJava = & mvn -version 2>&1 | Select-String "Java version"
    Write-Host "   Maven usando: $mavenJava" -ForegroundColor Cyan
} catch {
    Write-Host "   ❌ ERRO: Maven nao encontrado no PATH!" -ForegroundColor Red
    $allOk = $false
}

Write-Host ""

# 4. Verificar GROQ_API_KEY
Write-Host "4. Verificando GROQ_API_KEY..." -ForegroundColor Yellow
$groqKey = [Environment]::GetEnvironmentVariable("GROQ_API_KEY", "Machine")
if (-not $groqKey) {
    $groqKey = [Environment]::GetEnvironmentVariable("GROQ_API_KEY", "User")
}

if ($groqKey) {
    $maskedKey = $groqKey.Substring(0, 15) + "..." + $groqKey.Substring($groqKey.Length - 4)
    Write-Host "   ✅ GROQ_API_KEY configurada: $maskedKey" -ForegroundColor Green
} else {
    Write-Host "   ⚠️  AVISO: GROQ_API_KEY nao configurada" -ForegroundColor Yellow
    Write-Host "   Chat IA nao funcionara sem esta chave" -ForegroundColor Yellow
}

Write-Host ""

# 5. Verificar pom.xml
Write-Host "5. Verificando pom.xml..." -ForegroundColor Yellow
if (Test-Path "backend/pom.xml") {
    $pomContent = Get-Content "backend/pom.xml" -Raw
    
    if ($pomContent -match "<java\.version>21</java\.version>") {
        Write-Host "   ✅ pom.xml configurado para Java 21" -ForegroundColor Green
    } else {
        Write-Host "   ❌ ERRO: pom.xml nao esta configurado para Java 21" -ForegroundColor Red
        $allOk = $false
    }
    
    if ($pomContent -match "<version>3\.4\.1</version>") {
        Write-Host "   ✅ Spring Boot 3.4.1 (compativel com Java 21)" -ForegroundColor Green
    } else {
        Write-Host "   ⚠️  AVISO: Verificar versao do Spring Boot" -ForegroundColor Yellow
    }
} else {
    Write-Host "   ❌ ERRO: pom.xml nao encontrado!" -ForegroundColor Red
    $allOk = $false
}

Write-Host ""
Write-Host "========================================" -ForegroundColor Cyan

if ($allOk) {
    Write-Host "  ✅ AMBIENTE OK PARA JAVA 21!" -ForegroundColor Green
    Write-Host "========================================" -ForegroundColor Cyan
    Write-Host ""
    Write-Host "Proximo passo:" -ForegroundColor Yellow
    Write-Host "  cd backend" -ForegroundColor White
    Write-Host "  mvn clean compile" -ForegroundColor White
} else {
    Write-Host "  ❌ CORRECOES NECESSARIAS" -ForegroundColor Red
    Write-Host "========================================" -ForegroundColor Cyan
    Write-Host ""
    Write-Host "Acoes necessarias:" -ForegroundColor Yellow
    Write-Host "  1. Execute CONFIGURAR_JAVA21.bat como Administrador" -ForegroundColor White
    Write-Host "  2. Reinicie o terminal/IDE" -ForegroundColor White
    Write-Host "  3. Execute este script novamente" -ForegroundColor White
}

Write-Host ""