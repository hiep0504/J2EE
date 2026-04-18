param(
  [string]$Password
)

$ErrorActionPreference = 'Stop'

function Import-DotEnv([string]$Path) {
  if (-not (Test-Path $Path)) {
    return
  }

  Get-Content $Path | ForEach-Object {
    $line = $_.Trim()
    if (-not $line -or $line.StartsWith('#')) {
      return
    }

    $parts = $line.Split('=', 2)
    if ($parts.Count -ne 2) {
      return
    }

    $key = $parts[0].Trim()
    $value = $parts[1].Trim().Trim('"')
    if ($key.Length -gt 0) {
      Set-Item -Path ("Env:" + $key) -Value $value
    }
  }
}

Import-DotEnv (Join-Path $PSScriptRoot '.env')

if (-not $Password -or $Password.Trim().Length -eq 0) {
  $secure = Read-Host "Enter MySQL root password" -AsSecureString
  $bstr = [Runtime.InteropServices.Marshal]::SecureStringToBSTR($secure)
  try {
    $Password = [Runtime.InteropServices.Marshal]::PtrToStringBSTR($bstr)
  } finally {
    [Runtime.InteropServices.Marshal]::ZeroFreeBSTR($bstr)
  }
}

$env:MYSQL_PASSWORD = $Password

Write-Host "Starting Spring Boot with MYSQL_PASSWORD env var..."
& "$PSScriptRoot\mvnw.cmd" spring-boot:run
exit $LASTEXITCODE
