param(
 [Parameter(Mandatory)][string]$Sdk,
 [Parameter(Mandatory)][string]$Jdk,
 [Parameter(Mandatory)][string]$KeyDir,
 [Parameter(Mandatory)][string]$Unsigned,
 [Parameter(Mandatory)][string]$Output
)
$ErrorActionPreference='Stop'
$src=(Resolve-Path -LiteralPath $Unsigned).Path
$dest=[IO.Path]::GetFullPath($Output)
if ($dest -eq $src) {throw 'Use a separate output file'}
[IO.Directory]::CreateDirectory([IO.Path]::GetDirectoryName($dest)) | Out-Null
[IO.Directory]::CreateDirectory($KeyDir) | Out-Null
$ks=Join-Path $KeyDir 'sticky-native.p12'
$pwFile=Join-Path $KeyDir 'password.dpapi'
if ((Test-Path -LiteralPath $ks) -ne (Test-Path -LiteralPath $pwFile)) {throw 'Incomplete signing identity; recover the matching pair before signing'}
try {
 $env:JAVA_HOME=$Jdk
 if (!(Test-Path -LiteralPath $ks)){
  $bytes=New-Object byte[] 40
  $rng=[Security.Cryptography.RandomNumberGenerator]::Create();try{$rng.GetBytes($bytes)}finally{$rng.Dispose()}
  $env:STICKY_PERSONAL_PASS=[Convert]::ToBase64String($bytes)
  $protected=(ConvertTo-SecureString $env:STICKY_PERSONAL_PASS -AsPlainText -Force) | ConvertFrom-SecureString
  [IO.File]::WriteAllText($pwFile,$protected)
  & (Join-Path $Jdk 'bin/keytool.exe') -genkeypair -alias sticky-native -keystore $ks -storetype PKCS12 -storepass:env STICKY_PERSONAL_PASS -keypass:env STICKY_PERSONAL_PASS -keyalg RSA -keysize 3072 -validity 10000 -dname 'CN=Sticky Todo Personal Android'
  if($LASTEXITCODE -ne 0){throw 'Key generation failed'}
 }else{
  $secure=(Get-Content -Raw -LiteralPath $pwFile) | ConvertTo-SecureString
  $env:STICKY_PERSONAL_PASS=[Net.NetworkCredential]::new('',$secure).Password
 }
 $signer=Join-Path $Sdk 'build-tools/35.0.0/apksigner.bat'
 & $signer sign --ks $ks --ks-key-alias sticky-native --ks-pass env:STICKY_PERSONAL_PASS --key-pass env:STICKY_PERSONAL_PASS --out $dest $src
 if($LASTEXITCODE -ne 0){throw 'APK signing failed'}
 & $signer verify --verbose $dest
 if($LASTEXITCODE -ne 0){throw 'APK signature did not verify'}
}finally{Remove-Item Env:STICKY_PERSONAL_PASS -ErrorAction SilentlyContinue}
