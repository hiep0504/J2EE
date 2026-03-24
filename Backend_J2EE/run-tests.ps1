param(
  [string]$Password
)

$ErrorActionPreference = 'Stop'

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

Write-Host "Running mvnw test with MYSQL_PASSWORD env var..."
& "$PSScriptRoot\mvnw.cmd" test
exit $LASTEXITCODE
