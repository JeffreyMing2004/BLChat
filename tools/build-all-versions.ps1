# 一键编译全部版本的 BLChat 并把产物收进仓库根目录的 all\ 文件夹。
#
# 用法：
#   powershell -File tools\build-all-versions.ps1            # 完整构建 + 收集
#   powershell -File tools\build-all-versions.ps1 -SkipBuild # 只收集已有产物
#   powershell -File tools\build-all-versions.ps1 -DryRun    # 只打印将要执行的动作
#
# 说明：
#   - 各版本家族沿用自己 build-all.bat 里的 JDK 约定（1.20.x/1.21.x 用 JDK21，26.x 用 JDK25）
#   - 每个子工程取 build\libs 里最新的一份 jar（忽略 sources/javadoc）
#   - 产物统一命名为 BLChat-<MC版本段>-<模组版本>.jar，与既有发布命名保持一致

param(
    [switch]$SkipBuild,
    [switch]$DryRun
)

$ErrorActionPreference = 'Stop'
$root = Split-Path -Parent $PSScriptRoot
$outDir = Join-Path $root 'all'
if (-not (Test-Path $outDir)) {
    New-Item -ItemType Directory -Path $outDir | Out-Null
    Write-Host "已创建输出目录 all\"
}

$jdk21 = 'C:\Users\Administrator\.gradle\jdks\jdk-21\jdk-21.0.12+8'
$jdk25 = 'C:\Program Files\Eclipse Adoptium\jdk-25.0.3.9-hotspot'

$families = @(
    @{ Dir = '1.20.x'; Jdk = $jdk21; Projects = @('forge-1.20', 'forge-1.20.2', 'forge-1.20.6') }
    @{ Dir = '1.21.x'; Jdk = $jdk21; Projects = @('forge-1.21', 'forge-1.21.2', 'forge-1.21.6', 'forge-1.21.11') }
    @{ Dir = '26.1.x'; Jdk = $jdk25; Projects = @('forge-26.1', 'forge-26.1.1', 'forge-26.1.2') }
    @{ Dir = '26.2.x'; Jdk = $jdk25; Projects = @('forge-26.2') }
)

# 各子工程覆盖的 MC 版本段，用于发布文件名
$labels = @{
    'forge-1.20'    = '1.20-1.20.1'
    'forge-1.20.2'  = '1.20.2-1.20.4'
    'forge-1.20.6'  = '1.20.6'
    'forge-1.21'    = '1.21-1.21.1'
    'forge-1.21.2'  = '1.21.2-1.21.5'
    'forge-1.21.6'  = '1.21.6-1.21.10'
    'forge-1.21.11' = '1.21.11'
    'forge-26.1'    = '26.1'
    'forge-26.1.1'  = '26.1.1'
    'forge-26.1.2'  = '26.1.2'
    'forge-26.2'    = '26.2'
}

function Invoke-Build([string]$familyDir, [string]$project, [string]$jdk) {
    $projectDir = Join-Path $root (Join-Path $familyDir $project)
    if (-not (Test-Path (Join-Path $projectDir 'gradlew.bat'))) {
        Write-Warning "跳过 $familyDir\$project：找不到 gradlew.bat"
        return $false
    }
    Write-Host ""
    Write-Host "---- 构建 $familyDir\$project ----" -ForegroundColor Cyan
    if ($DryRun) { return $true }

    $env:JAVA_HOME = $jdk
    $env:PATH = "$jdk\bin;$env:PATH"
    Push-Location $projectDir
    try {
        cmd /c "gradlew.bat build --no-daemon" | Write-Host
        if ($LASTEXITCODE -ne 0) {
            Write-Host "[FAIL] $familyDir\$project 构建失败" -ForegroundColor Red
            return $false
        }
        return $true
    } finally {
        Pop-Location
    }
}

function Get-LatestJar([string]$projectDir) {
    Get-ChildItem -Path (Join-Path $projectDir 'build\libs') -Filter '*.jar' -ErrorAction SilentlyContinue |
        Where-Object { $_.Name -notmatch '-(sources|javadoc)\.jar$' } |
        Sort-Object LastWriteTime -Descending |
        Select-Object -First 1
}

# 从 jar 名里取模组版本号：从后往前找第一段形如 x.y(.z...) 的内容，
# 避免把发布名末尾的构建号（如 -274）误当成版本。
function Get-ModVersion([string]$jarName) {
    $segments = [IO.Path]::GetFileNameWithoutExtension($jarName).Split('-')
    for ($i = $segments.Length - 1; $i -ge 0; $i--) {
        if ($segments[$i] -match '^\d+(\.\d+)+$') {
            if (($i -eq $segments.Length - 2) -and ($segments[$i + 1] -match '^\d+$')) {
                return "$($segments[$i])-$($segments[$i + 1])"
            }
            return $segments[$i]
        }
    }
    return $segments[-1]
}

$failed = @()
$collected = @()

foreach ($fam in $families) {
    foreach ($project in $fam.Projects) {
        if (-not $SkipBuild) {
            if (-not (Invoke-Build $fam.Dir $project $fam.Jdk)) {
                $failed += "$($fam.Dir)\$project"
                continue
            }
        }
        if ($DryRun) { continue }

        $projectDir = Join-Path $root (Join-Path $fam.Dir $project)
        $jar = Get-LatestJar $projectDir
        if ($null -eq $jar) {
            Write-Warning "$($fam.Dir)\$project 没有找到构建产物"
            $failed += "$($fam.Dir)\$project"
            continue
        }

        $label = $labels[$project]
        $version = Get-ModVersion $jar.Name
        $targetName = if ($label) { "BLChat-$label-$version.jar" } else { $jar.Name }
        $target = Join-Path $outDir $targetName

        Move-Item -LiteralPath $jar.FullName -Destination $target -Force
        Write-Host "[OK] $($jar.Name) -> all\$targetName" -ForegroundColor Green
        $collected += $targetName
    }
}

Write-Host ""
Write-Host "============================================================" -ForegroundColor Cyan
if ($failed.Count -gt 0) {
    Write-Host ("完成，但有 {0} 个工程失败：{1}" -f $failed.Count, ($failed -join ', ')) -ForegroundColor Red
} elseif ($DryRun) {
    Write-Host "DryRun 结束，未实际构建或移动文件。"
} else {
    Write-Host ("全部完成，共收集 {0} 个 jar 到 all\" -f $collected.Count) -ForegroundColor Green
    $collected | Sort-Object | ForEach-Object { Write-Host "  $_" }
}
