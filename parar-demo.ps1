# Encerra o que o iniciar-demo.ps1 subiu: túnel, frontend, backend e o processo que mantém o PC acordado.
$logs = Join-Path $PSScriptRoot 'logs'

Get-Process cloudflared -ErrorAction SilentlyContinue | Stop-Process -Force -Confirm:$false

foreach ($porta in 9000, 8080) {
    Get-NetTCPConnection -LocalPort $porta -State Listen -ErrorAction SilentlyContinue |
        ForEach-Object { Stop-Process -Id $_.OwningProcess -Force -Confirm:$false -ErrorAction SilentlyContinue }
}

$pidArquivo = Join-Path $logs 'acordado.pid'
if (Test-Path $pidArquivo) {
    Stop-Process -Id (Get-Content $pidArquivo) -Force -Confirm:$false -ErrorAction SilentlyContinue
    Remove-Item $pidArquivo
}

Write-Host 'Demonstração encerrada.'
