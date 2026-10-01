param([Parameter(Mandatory = $true)][string]$LogPath)
$ErrorActionPreference = 'Stop'

# Processa somente metadados dos eventos ia_fase; nao imprime o log original.
$measurements = @(Get-Content -LiteralPath $LogPath | ForEach-Object {
    if ($_ -match 'ia_fase fase=(\S+) duracaoMs=(\d+).*resultado=(\S+)') {
        [pscustomobject]@{
            Fase = $Matches[1]
            DuracaoMs = [long]$Matches[2]
            Resultado = $Matches[3]
        }
    }
})
if ($measurements.Count -eq 0) { throw 'Nenhuma medicao ia_fase com duracao encontrada.' }
$measurements | Group-Object Fase, Resultado | ForEach-Object {
    $times = @($_.Group.DuracaoMs | Sort-Object)
    [pscustomobject]@{
        Fase = $_.Group[0].Fase
        Resultado = $_.Group[0].Resultado
        Amostras = $times.Count
        P50Ms = $times[[int][math]::Ceiling($times.Count * 0.50) - 1]
        P95Ms = $times[[int][math]::Ceiling($times.Count * 0.95) - 1]
        TotalMs = ($times | Measure-Object -Sum).Sum
    }
} | Sort-Object Fase, Resultado | ConvertTo-Json
