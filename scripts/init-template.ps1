[CmdletBinding(SupportsShouldProcess)]
param(
    [Parameter(Mandatory)][ValidateNotNullOrEmpty()][string]$ProjectName,
    [Parameter(Mandatory)][ValidateNotNullOrEmpty()][string]$ModuleName,
    [Parameter(Mandatory)][ValidatePattern('^[A-Za-z][A-Za-z0-9_]*(\.[A-Za-z][A-Za-z0-9_]*)+$')][string]$ModulePackage,
    [Parameter(Mandatory)][ValidateNotNullOrEmpty()][string[]]$TargetPackages,
    [string]$LogTag = $ModuleName,
    [string]$PreferencesName
)

$ErrorActionPreference = 'Stop'
$repositoryRoot = (Resolve-Path (Join-Path $PSScriptRoot '..')).Path
$propertiesPath = Join-Path $repositoryRoot 'gradle.properties'

$normalizedTargets = $TargetPackages |
    ForEach-Object { $_.Split(',') } |
    ForEach-Object { $_.Trim() } |
    Where-Object { $_ } |
    Select-Object -Unique

$packagePattern = '^[A-Za-z][A-Za-z0-9_]*(\.[A-Za-z][A-Za-z0-9_]*)+$'
if ($normalizedTargets.Count -eq 0 -or ($normalizedTargets | Where-Object { $_ -notmatch $packagePattern })) {
    throw 'TargetPackages must contain valid Android package names.'
}

if (-not $PreferencesName) {
    $PreferencesName = ($ModulePackage -replace '[^A-Za-z0-9_]', '_').ToLowerInvariant()
}
if ($PreferencesName -notmatch '^[a-z][a-z0-9_]*$') {
    throw 'PreferencesName must use lowercase letters, digits, and underscores.'
}

$values = [ordered]@{
    xposedTargetPackages = $normalizedTargets -join ','
    xposedProjectName = $ProjectName
    xposedModuleName = $ModuleName
    xposedModulePackage = $ModulePackage
    xposedLogTag = $LogTag
    xposedPreferencesName = $PreferencesName
}

$content = Get-Content -LiteralPath $propertiesPath -Raw
foreach ($entry in $values.GetEnumerator()) {
    $pattern = "(?m)^$([Regex]::Escape($entry.Key))=.*$"
    if ($content -notmatch $pattern) { throw "Missing template property: $($entry.Key)" }
    $content = [Regex]::Replace($content, $pattern, "$($entry.Key)=$($entry.Value)")
}
if ($PSCmdlet.ShouldProcess($propertiesPath, 'Initialize Xposed template properties')) {
    Set-Content -LiteralPath $propertiesPath -Value $content -Encoding utf8NoBOM
    Write-Host "Initialized $ModuleName ($ModulePackage) for $($normalizedTargets -join ', ')."
    Write-Host 'Next: replace launcher assets and run both legacy and modern debug builds.'
}
