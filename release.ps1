$ErrorActionPreference = 'Stop'
[Console]::OutputEncoding = [System.Text.Encoding]::UTF8

function Invoke-Native([string] $command, [string[]] $arguments) {
    $ErrorActionPreference = 'Continue'
    & $command @arguments
    if ($LASTEXITCODE -ne 0) {
        throw "$command $($arguments -join ' ') завершился с кодом $LASTEXITCODE"
    }
}

function Get-ReleaseNotes {
    $previousTag = git tag --list 'v*' --sort=-version:refname | Select-Object -First 1
    $range = if ($previousTag) { "$previousTag..HEAD" } else { 'HEAD' }
    return (git log $range '--pretty=format:- %s') -join "`n"
}

Push-Location $PSScriptRoot
try {
    $versionMatch = Select-String -Path 'composeApp\build.gradle.kts' -Pattern '^version = "(.+)"$'
    if ($null -eq $versionMatch) {
        throw 'В composeApp\build.gradle.kts нет строки version = "..."'
    }
    $version = $versionMatch.Matches[0].Groups[1].Value
    $tag = "v$version"
    $zip = "Poligon-$version.zip"

    if ((git rev-parse --abbrev-ref HEAD) -ne 'main') {
        throw 'Релиз выпускается только из main'
    }
    if (git status --porcelain) {
        throw 'Есть незакоммиченные изменения'
    }
    Invoke-Native 'git' @('fetch', '--tags', 'origin')
    if (git tag --list $tag) {
        throw "Тег $tag уже есть: подними version в composeApp\build.gradle.kts"
    }
    if (Get-Process -Name 'Poligon' -ErrorAction SilentlyContinue) {
        throw 'Закрой Poligon: запущенное приложение держит файлы сборки'
    }

    Invoke-Native '.\gradlew.bat' @('desktopTest', ':composeApp:packageZip')

    $buildDir = New-Item -ItemType Directory -Force (Join-Path $PSScriptRoot 'build')
    $notesFile = Join-Path $buildDir.FullName 'release-notes.md'
    [System.IO.File]::WriteAllText($notesFile, (Get-ReleaseNotes), (New-Object System.Text.UTF8Encoding $false))

    Invoke-Native 'git' @('tag', '-a', $tag, '-m', "Poligon $version")
    Invoke-Native 'git' @('push', 'origin', 'main', $tag)
    Invoke-Native 'gh' @('release', 'create', $tag, $zip, '--title', "Poligon $version", '--notes-file', $notesFile)
} finally {
    Pop-Location
}
