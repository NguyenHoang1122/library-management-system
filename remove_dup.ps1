$dir = "c:\codegym\model4-lam-lai\case-module4\new-foder\library-management-system\src\main\resources\templates"
$files = Get-ChildItem -Path $dir -Filter "*.html" -Recurse

foreach ($file in $files) {
    # Read with UTF8
    $content = Get-Content $file.FullName -Encoding UTF8 -Raw
    $newContent = [regex]::Replace($content, '(?m)^\s*<script src="https://cdn\.jsdelivr\.net/npm/bootstrap@5\.3\.0/dist/js/bootstrap\.bundle\.min\.js"></script>\s*\r?\n', '')
    if ($content -ne $newContent) {
        # Write back with UTF8 BOM-less or standard UTF8
        Set-Content -Path $file.FullName -Value $newContent -Encoding UTF8 -NoNewline
        Write-Output "Updated: $($file.FullName)"
    }
}
