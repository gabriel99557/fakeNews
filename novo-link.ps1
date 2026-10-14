# Reinicia só o túnel da Cloudflare e mostra o link novo (o backend e o frontend continuam rodando).
# Use quando o link parar de abrir (erro 530): o túnel gratuito não reconecta com o mesmo endereço.
#
# Uso (na pasta do projeto):  powershell -ExecutionPolicy Bypass -File .\novo-link.ps1

$ErrorActionPreference = 'Stop'
$logs = Join-Path $PSScriptRoot 'logs'
New-Item -ItemType Directory -Force $logs | Out-Null
$log = Join-Path $logs 'tunel.log'

# Garante o processo que mantém o PC e a tela acordados (se o PC suspender, o túnel morre)
$pidArquivo = Join-Path $logs 'acordado.pid'
$vivo = (Test-Path $pidArquivo) -and (Get-Process -Id (Get-Content $pidArquivo) -ErrorAction SilentlyContinue)
if (-not $vivo) {
    $acordado = @'
Add-Type -Name Energia -Namespace Win32 -MemberDefinition '[DllImport("kernel32.dll")] public static extern uint SetThreadExecutionState(uint esFlags);'
while ($true) { [Win32.Energia]::SetThreadExecutionState(0x80000003) | Out-Null; Start-Sleep -Seconds 60 }
'@
    $codificado = [Convert]::ToBase64String([Text.Encoding]::Unicode.GetBytes($acordado))
    $p = Start-Process powershell.exe -WindowStyle Hidden -PassThru -ArgumentList '-NoProfile', '-EncodedCommand', $codificado
    Set-Content $pidArquivo $p.Id
}

Get-Process cloudflared -ErrorAction SilentlyContinue | Stop-Process -Force -Confirm:$false
Start-Sleep -Seconds 1
Remove-Item $log -ErrorAction SilentlyContinue

Start-Process 'C:\Program Files (x86)\cloudflared\cloudflared.exe' -WindowStyle Hidden `
    -ArgumentList 'tunnel', '--no-autoupdate', '--url', 'http://localhost:9000', '--logfile', $log

$link = $null
for ($i = 0; $i -lt 60 -and -not $link; $i += 2) {
    Start-Sleep -Seconds 2
    $texto = Get-Content $log -Raw -ErrorAction SilentlyContinue
    if ($texto -match 'https://[a-z0-9]+(-[a-z0-9]+)+\.trycloudflare\.com') { $link = $Matches[0] }
}
if (-not $link) { throw "O túnel não informou o link em 60 s. Veja $log" }

for ($i = 0; $i -lt 60; $i += 3) {
    try { if ((Invoke-WebRequest $link -UseBasicParsing -TimeoutSec 5).StatusCode -eq 200) { break } } catch { }
    Start-Sleep -Seconds 3
}
Set-Content (Join-Path $logs 'link.txt') $link

Write-Host ''
Write-Host " Novo link: $link"
