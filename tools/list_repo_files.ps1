$ErrorActionPreference = 'Stop'
$root = 'C:\Users\Administrator\Doubao\chats\2026-11-05\new-chat\NiuMaWidget'
$files = @()
foreach ($f in Get-ChildItem -LiteralPath $root -Recurse -File -Force) {
    $rel = $f.FullName.Substring($root.Length + 1) -replace '\\', '/'
    if ($rel -match '^(build|dist|k70_debug|gen|classes|dex)/') { continue }
    if ($rel -match '\.log$') { continue }
    if ($rel -match '\.keystore$') { continue }
    $files += , @{ rel = $rel; full = $f.FullName; len = $f.Length }
}
$files | ForEach-Object { [pscustomobject]@{ Path = $_.rel; Bytes = $_.len } } | Format-Table -AutoSize | Out-String
"共 " + $files.Count + " 个文件"
