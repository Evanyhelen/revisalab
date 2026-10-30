@echo off
echo ========================================
echo    Build do Backend RevisaLab
echo ========================================

REM Verificar se Java está instalado
java -version > nul 2>&1
if errorlevel 1 (
    echo ERRO: Java não encontrado!
    echo Instale o Java 17 ou superior.
    exit /b 1
)

REM Verificar se Maven está instalado
mvn -version > nul 2>&1
if errorlevel 1 (
    echo ERRO: Maven não encontrado!
    echo Instale o Maven 3.6 ou superior.
    exit /b 1
)

echo Limpando e construindo o projeto...
mvn clean compile

if errorlevel 1 (
    echo ERRO: Falha ao compilar o projeto!
    exit /b 1
)

echo Projeto compilado com sucesso!

echo.
echo Comandos disponíveis:
echo   mvn spring-boot:run   - Iniciar aplicação
echo   mvn test              - Executar testes
echo   mvn package           - Criar pacote JAR
echo.
echo Acessos:
echo   Aplicação:      http://localhost:8080
echo   API:            http://localhost:8080/api
echo   H2 Console:     http://localhost:8080/h2-console
echo   Swagger UI:     http://localhost:8080/swagger-ui/index.html
echo.
echo Credenciais:
echo   Admin:    admin@revisalab.com / admin123
echo   Usuário:  teste@revisalab.com / teste123