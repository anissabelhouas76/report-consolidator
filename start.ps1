param([switch]$Build)
$ErrorActionPreference = 'Stop'
Set-Location $PSScriptRoot
$war = Join-Path $PSScriptRoot 'target/report-consolidator.war'
$socketTemp = Join-Path $env:SystemRoot 'Temp'
if ($Build -or !(Test-Path -LiteralPath $war)) {
 $arguments = @('-Dmaven.test.skip=true', 'package')
 if (Get-Command mvn -ErrorAction SilentlyContinue) { & mvn @arguments }
 elseif (Test-Path -LiteralPath '../nouveau-projet/maven.ps1') { & '../nouveau-projet/maven.ps1' -MavenArgs $arguments }
 else { throw 'Installez Maven ou compilez le projet avec votre environnement Java.' }
 if ($LASTEXITCODE -ne 0) { throw 'La compilation ou les tests ont échoué.' }
}
& java "-Djdk.net.unixdomain.tmpdir=$socketTemp" -jar $war