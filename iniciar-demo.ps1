# Sobe o CrivoNews para demonstração e cria um link público temporário (túnel da Cloudflare).
#
# Uso (na pasta do projeto):  powershell -ExecutionPolicy Bypass -File .\iniciar-demo.ps1
#
# - Backend com banco H2 em memória (contas e histórico somem ao reiniciar)
# - Frontend em http://localhost:9000, que repassa /api ao backend
# - Modelo local (Ollama) pré-carregado e mantido na memória
# - Computador impedido de entrar em suspensão enquanto a demonstração roda
# - Link público em logs\link.txt (muda a cada execução)
#
# Para encerrar tudo:  powershell -ExecutionPolicy Bypass -File .\parar-demo.ps1

$ErrorActionPreference = 'Stop'
$raiz = $PSScriptRoot
$logs = Join-Path $raiz 'logs'
New-Item -ItemType Directory -Force $logs | Out-Null
$cloudflared = 'C:\Program Files (x86)\cloudflared\cloudflared.exe'

function Esperar($descricao, [scriptblock]$teste, $segundos) {
    Write-Host "Aguardando $descricao..." -NoNewline
    for ($i = 0; $i -lt $segundos; $i += 2) {
        if (& $teste) { Write-Host ' ok'; return }
        Start-Sleep -Seconds 2
        Write-Host '.' -NoNewline
    }
    throw "$descricao não respondeu em $segundos s. Veja os arquivos em $logs"
}

function Responde($url) {
    try { (Invoke-WebRequest $url -UseBasicParsing -TimeoutSec 3).StatusCode -eq 200 } catch { $false }
}

foreach ($porta in 8080, 9000) {
    if (Get-NetTCPConnection -LocalPort $porta -State Listen -ErrorAction SilentlyContinue) {
        throw "A porta $porta já está em uso. Rode .\parar-demo.ps1 (ou feche o que estiver usando a porta) e tente de novo."
    }
}

# 1. Backend (Spring Boot, perfil h2). A chave da Groq vem de fakeNews\application.properties.
Start-Process cmd.exe -WindowStyle Hidden -WorkingDirectory (Join-Path $raiz 'fakeNews') `
    -ArgumentList '/c', '.\mvnw.cmd -q spring-boot:run "-Dspring-boot.run.profiles=h2" > ..\logs\backend.log 2>&1'

# 2. Frontend (Vite)
Start-Process cmd.exe -WindowStyle Hidden -WorkingDirectory (Join-Path $raiz 'crivo-frontend') `
    -ArgumentList '/c', 'npm.cmd run dev > ..\logs\frontend.log 2>&1'

Esperar 'o backend' { Responde 'http://localhost:8080/actuator/health' } 180
Esperar 'o frontend' { Responde 'http://localhost:9000' } 60

# 3. Modelo local carregado e mantido na memória (evita a espera da primeira análise)
try {
    Invoke-RestMethod -Method Post -Uri 'http://localhost:11434/api/generate' -TimeoutSec 300 `
        -ContentType 'application/json' -Body '{"model":"meu-llama-tcc","prompt":"","keep_alive":-1}' | Out-Null
    Write-Host 'Modelo local carregado.'
} catch {
    Write-Warning "Não consegui carregar o modelo local no Ollama ($($_.Exception.Message)). A análise pela Groq continua funcionando."
}

# 4. Mantém o computador e a tela acordados enquanto este processo existir (não altera as configurações
#    de energia; não impede uma suspensão feita manualmente pelo botão ou pelo menu Iniciar)
$acordado = @'
Add-Type -Name Energia -Namespace Win32 -MemberDefinition '[DllImport("kernel32.dll")] public static extern uint SetThreadExecutionState(uint esFlags);'
while ($true) { [Win32.Energia]::SetThreadExecutionState(0x80000003) | Out-Null; Start-Sleep -Seconds 60 }
'@
$codificado = [Convert]::ToBase64String([Text.Encoding]::Unicode.GetBytes($acordado))
$p = Start-Process powershell.exe -WindowStyle Hidden -PassThru -ArgumentList '-NoProfile', '-EncodedCommand', $codificado
Set-Content (Join-Path $logs 'acordado.pid') $p.Id

# 5. Túnel público da Cloudflare (sem conta; o endereço muda a cada execução)
Remove-Item (Join-Path $logs 'tunel.log') -ErrorAction SilentlyContinue
Start-Process $cloudflared -WindowStyle Hidden `
    -ArgumentList 'tunnel', '--no-autoupdate', '--url', 'http://localhost:9000', '--logfile', (Join-Path $logs 'tunel.log')

$link = $null
Esperar 'o túnel' {
    $texto = Get-Content (Join-Path $logs 'tunel.log') -Raw -ErrorAction SilentlyContinue
    if ($texto -match 'https://[a-z0-9]+(-[a-z0-9]+)+\.trycloudflare\.com') { $script:link = $Matches[0]; $true } else { $false }
} 60
Esperar 'o link responder' { Responde $link } 60
Set-Content (Join-Path $logs 'link.txt') $link

Write-Host ''
Write-Host '============================================================'
Write-Host " CrivoNews no ar: $link"
Write-Host ' (também em http://localhost:9000 neste computador)'
Write-Host '============================================================'
