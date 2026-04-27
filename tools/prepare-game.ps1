[CmdletBinding()]
param(
    [Parameter(Position = 0)]
    [string]$JarPath,

    [switch]$KeepIntermediate,
    [switch]$Build
)

$ErrorActionPreference = "Stop"

$ClassMap = [ordered]@{
    "a" = "com/ratchetclank/goingmobile/EntPlayer"
    "b" = "com/ratchetclank/goingmobile/SndManager"
    "c" = "com/ratchetclank/goingmobile/LevelData"
    "d" = "com/ratchetclank/goingmobile/EntEnemy"
    "e" = "com/ratchetclank/goingmobile/GameUI"
    "f" = "com/ratchetclank/goingmobile/MenuFont"
    "g" = "com/ratchetclank/goingmobile/GameCanvas"
    "h" = "com/ratchetclank/goingmobile/EntBullet"
    "i" = "com/ratchetclank/goingmobile/EntBase"
    "ratchetandclank" = "com/ratchetclank/goingmobile/ratchetandclank"
}

function New-CleanDirectory {
    param(
        [string]$Path,
        [string[]]$KeepNames = @()
    )

    if (Test-Path -LiteralPath $Path) {
        Get-ChildItem -LiteralPath $Path -Force |
            Where-Object { $KeepNames -notcontains $_.Name } |
            Remove-Item -Recurse -Force
    } else {
        New-Item -ItemType Directory -Path $Path | Out-Null
    }
}

function Resolve-JarPath {
    param([string]$Path)

    if ([string]::IsNullOrWhiteSpace($Path)) {
        $Path = Read-Host "Enter path to Going Mobile 1.1.0.jar"
    }

    if (-not (Test-Path -LiteralPath $Path -PathType Leaf)) {
        throw "JAR file not found: $Path"
    }

    return (Resolve-Path -LiteralPath $Path).Path
}

function Read-U2 {
    param([byte[]]$Bytes, [ref]$Offset)
    $value = ([int]$Bytes[$Offset.Value] -shl 8) -bor [int]$Bytes[$Offset.Value + 1]
    $Offset.Value += 2
    return $value
}

function Write-U2 {
    param([System.Collections.Generic.List[byte]]$Out, [int]$Value)
    $Out.Add([byte](($Value -shr 8) -band 0xFF))
    $Out.Add([byte]($Value -band 0xFF))
}

function Write-U4 {
    param([System.Collections.Generic.List[byte]]$Out, [int64]$Value)
    $Out.Add([byte](($Value -shr 24) -band 0xFF))
    $Out.Add([byte](($Value -shr 16) -band 0xFF))
    $Out.Add([byte](($Value -shr 8) -band 0xFF))
    $Out.Add([byte]($Value -band 0xFF))
}

function Convert-DescriptorText {
    param([string]$Text)

    foreach ($oldName in $ClassMap.Keys) {
        $newName = $ClassMap[$oldName]
        $Text = $Text.Replace("L$oldName;", "L$newName;")
        $Text = $Text.Replace("[$oldName", "[$newName")
    }
    return $Text
}

function Convert-ClassFile {
    param([byte[]]$Bytes)

    $offset = [ref]0
    if ($Bytes.Length -lt 10 -or $Bytes[0] -ne 0xCA -or $Bytes[1] -ne 0xFE -or $Bytes[2] -ne 0xBA -or $Bytes[3] -ne 0xBE) {
        return $Bytes
    }
    $offset.Value = 8
    $cpCount = Read-U2 $Bytes $offset
    $entries = New-Object object[] $cpCount

    for ($i = 1; $i -lt $cpCount; $i++) {
        $tag = $Bytes[$offset.Value]
        $offset.Value++
        switch ($tag) {
            1 {
                $len = Read-U2 $Bytes $offset
                $raw = New-Object byte[] $len
                [Array]::Copy($Bytes, $offset.Value, $raw, 0, $len)
                $offset.Value += $len
                $entries[$i] = [pscustomobject]@{ Tag = $tag; Text = [Text.Encoding]::UTF8.GetString($raw) }
            }
            3 { $entries[$i] = [pscustomobject]@{ Tag = $tag; Data = $Bytes[$offset.Value..($offset.Value + 3)] }; $offset.Value += 4 }
            4 { $entries[$i] = [pscustomobject]@{ Tag = $tag; Data = $Bytes[$offset.Value..($offset.Value + 3)] }; $offset.Value += 4 }
            5 { $entries[$i] = [pscustomobject]@{ Tag = $tag; Data = $Bytes[$offset.Value..($offset.Value + 7)] }; $offset.Value += 8; $i++ }
            6 { $entries[$i] = [pscustomobject]@{ Tag = $tag; Data = $Bytes[$offset.Value..($offset.Value + 7)] }; $offset.Value += 8; $i++ }
            7 { $entries[$i] = [pscustomobject]@{ Tag = $tag; A = Read-U2 $Bytes $offset } }
            8 { $entries[$i] = [pscustomobject]@{ Tag = $tag; A = Read-U2 $Bytes $offset } }
            9 { $entries[$i] = [pscustomobject]@{ Tag = $tag; A = Read-U2 $Bytes $offset; B = Read-U2 $Bytes $offset } }
            10 { $entries[$i] = [pscustomobject]@{ Tag = $tag; A = Read-U2 $Bytes $offset; B = Read-U2 $Bytes $offset } }
            11 { $entries[$i] = [pscustomobject]@{ Tag = $tag; A = Read-U2 $Bytes $offset; B = Read-U2 $Bytes $offset } }
            12 { $entries[$i] = [pscustomobject]@{ Tag = $tag; A = Read-U2 $Bytes $offset; B = Read-U2 $Bytes $offset } }
            15 { $refKind = $Bytes[$offset.Value]; $offset.Value++; $entries[$i] = [pscustomobject]@{ Tag = $tag; A = $refKind; B = Read-U2 $Bytes $offset } }
            16 { $entries[$i] = [pscustomobject]@{ Tag = $tag; A = Read-U2 $Bytes $offset } }
            18 { $entries[$i] = [pscustomobject]@{ Tag = $tag; A = Read-U2 $Bytes $offset; B = Read-U2 $Bytes $offset } }
            19 { $entries[$i] = [pscustomobject]@{ Tag = $tag; A = Read-U2 $Bytes $offset } }
            20 { $entries[$i] = [pscustomobject]@{ Tag = $tag; A = Read-U2 $Bytes $offset } }
            default { throw "Unsupported constant pool tag $tag at index $i" }
        }
    }
    $afterConstantPool = $offset.Value

    $classNameIndexes = @{}
    $descriptorIndexes = @{}
    $classNameReplacementIndexes = @{}
    $extraUtf8 = New-Object 'System.Collections.Generic.List[string]'
    for ($entryIndex = 1; $entryIndex -lt $cpCount; $entryIndex++) {
        $entry = $entries[$entryIndex]
        if ($null -eq $entry) { continue }
        if ($entry.Tag -eq 7) {
            $classNameIndexes[$entry.A] = $true
            $oldText = $entries[$entry.A].Text
            $newText = $oldText
            if ($ClassMap.Contains($oldText)) {
                $newText = $ClassMap[$oldText]
            } else {
                $newText = Convert-DescriptorText $oldText
            }
            if ($newText -ne $oldText) {
                $newIndex = $cpCount + $extraUtf8.Count
                $extraUtf8.Add($newText)
                $classNameReplacementIndexes[$entryIndex] = $newIndex
            }
        }
        if ($entry.Tag -eq 12) { $descriptorIndexes[$entry.B] = $true }
    }

    $out = New-Object 'System.Collections.Generic.List[byte]'
    Write-U4 $out 0xCAFEBABE
    Write-U2 $out (($Bytes[4] -shl 8) -bor $Bytes[5])
    Write-U2 $out (($Bytes[6] -shl 8) -bor $Bytes[7])
    Write-U2 $out ($cpCount + $extraUtf8.Count)

    for ($i = 1; $i -lt $cpCount; $i++) {
        $entry = $entries[$i]
        if ($null -eq $entry) { continue }
        $out.Add([byte]$entry.Tag)
        switch ($entry.Tag) {
            1 {
                $text = $entry.Text
                if ($descriptorIndexes.ContainsKey($i)) {
                    $text = Convert-DescriptorText $text
                }
                $data = [Text.Encoding]::UTF8.GetBytes($text)
                Write-U2 $out $data.Length
                $out.AddRange($data)
            }
            3 { $out.AddRange([byte[]]$entry.Data) }
            4 { $out.AddRange([byte[]]$entry.Data) }
            5 { $out.AddRange([byte[]]$entry.Data); $i++ }
            6 { $out.AddRange([byte[]]$entry.Data); $i++ }
            7 {
                if ($classNameReplacementIndexes.ContainsKey($i)) {
                    Write-U2 $out $classNameReplacementIndexes[$i]
                } else {
                    Write-U2 $out $entry.A
                }
            }
            8 { Write-U2 $out $entry.A }
            9 { Write-U2 $out $entry.A; Write-U2 $out $entry.B }
            10 { Write-U2 $out $entry.A; Write-U2 $out $entry.B }
            11 { Write-U2 $out $entry.A; Write-U2 $out $entry.B }
            12 { Write-U2 $out $entry.A; Write-U2 $out $entry.B }
            15 { $out.Add([byte]$entry.A); Write-U2 $out $entry.B }
            16 { Write-U2 $out $entry.A }
            18 { Write-U2 $out $entry.A; Write-U2 $out $entry.B }
            19 { Write-U2 $out $entry.A }
            20 { Write-U2 $out $entry.A }
        }
    }

    foreach ($text in $extraUtf8) {
        $out.Add([byte]1)
        $data = [Text.Encoding]::UTF8.GetBytes($text)
        Write-U2 $out $data.Length
        $out.AddRange($data)
    }

    $remaining = New-Object byte[] ($Bytes.Length - $afterConstantPool)
    [Array]::Copy($Bytes, $afterConstantPool, $remaining, 0, $remaining.Length)
    $out.AddRange($remaining)
    return $out.ToArray()
}

function Convert-GameJar {
    param([string]$InputJar, [string]$OutputJar)

    Add-Type -AssemblyName System.IO.Compression.FileSystem
    if (Test-Path -LiteralPath $OutputJar) {
        Remove-Item -LiteralPath $OutputJar -Force
    }

    $input = [System.IO.Compression.ZipFile]::OpenRead($InputJar)
    $output = [System.IO.Compression.ZipFile]::Open($OutputJar, [System.IO.Compression.ZipArchiveMode]::Create)
    try {
        foreach ($entry in $input.Entries) {
            if ([string]::IsNullOrEmpty($entry.Name)) { continue }
            if ($entry.FullName.StartsWith("META-INF/")) { continue }

            $targetName = $entry.FullName
            $bytes = $null
            $stream = $entry.Open()
            try {
                $memory = New-Object System.IO.MemoryStream
                $stream.CopyTo($memory)
                $bytes = $memory.ToArray()
            } finally {
                $stream.Dispose()
            }

            if ($entry.FullName.EndsWith(".class")) {
                $baseName = [System.IO.Path]::GetFileNameWithoutExtension($entry.FullName)
                if ($ClassMap.Contains($baseName)) {
                    $targetName = $ClassMap[$baseName] + ".class"
                }
                $bytes = Convert-ClassFile $bytes
            }

            $newEntry = $output.CreateEntry($targetName)
            $outStream = $newEntry.Open()
            try {
                $outStream.Write($bytes, 0, $bytes.Length)
            } finally {
                $outStream.Dispose()
            }
        }
    } finally {
        $input.Dispose()
        $output.Dispose()
    }
}

function Add-CanvasViewImport {
    param([string]$Path)

    $text = Get-Content -LiteralPath $Path -Raw
    if ($text.Contains("CanvasView.openResource") -and -not $text.Contains("import javax.microedition.lcdui.CanvasView;")) {
        $text = [regex]::Replace($text, "(?m)^import javax\.microedition", "import javax.microedition.lcdui.CanvasView;`r`nimport javax.microedition", 1)
        Set-Content -LiteralPath $Path -Value $text -NoNewline
    }
}

function Apply-SourceFixups {
    param([string]$SourceDir)

    Get-ChildItem -LiteralPath $SourceDir -Filter "*.java" | ForEach-Object {
        $path = $_.FullName
        $text = Get-Content -LiteralPath $path -Raw

        $text = [regex]::Replace($text, '[A-Za-z0-9_""\[\]]+\.getClass\(\)\.getResourceAsStream\(', 'CanvasView.openResource(')
        $text = $text.Replace("this.getClass().getResourceAsStream(", "CanvasView.openResource(")
        $text = [regex]::Replace($text, "\*\* GOTO ([A-Za-z0-9_]+)", "/* GOTO `$1 */")
        $text = $text.Replace("(g g2)", "(GameCanvas g2)")

        if ($_.Name -eq "GameCanvas.java") {
            $text = $text.Replace("public byte[][] do;", "public byte[][] cfr_renamed_0;")
            $text = $text.Replace("this.do", "this.cfr_renamed_0")
            $text = $text.Replace("this.as.ac = -EntPlayer.o;", "this.as.ac = (short)(-EntPlayer.o);")
            $text = $text.Replace("this.bz[n] = n2;", "this.bz[n] = (short)n2;")
            $text = $text.Replace("this.bA[n2] = n;", "this.bA[n2] = (short)n;")
            $text = $text.Replace("this.bA[n] = n2;", "this.bA[n] = (short)n2;")
            $text = $text.Replace("this.bz[n2] = n;", "this.bz[n2] = (short)n;")
            $text = $text.Replace("this.cu[n4] = n3 == 0 ? (int)(this.a(this.af.nextInt()) % 2) : (n3 == 1 ? 2 : 3);", "this.cu[n4] = (byte)(n3 == 0 ? (int)(this.a(this.af.nextInt()) % 2) : (n3 == 1 ? 2 : 3));")

            $shopFix = @'
                    if ((this.as.P & 1 << this.ez + 1) == 0) {
                        if (this.bg >= EntPlayer.i[this.ez]) {
                            this.bg -= EntPlayer.i[this.ez];
                            this.as.P = (byte)(this.as.P | 1 << this.ez + 1);
                        } else {
                            this.cE = 0;
                            this.h = (byte)13;
                            this.v = null;
                            return;
                        }
                    }
                    // lbl47: upgrade weapon
                    if (this.bg >= this.bi[this.ez + 1]) {
                        this.bg -= this.bi[this.ez + 1];
                        v0 = this.ez + 1;
                        this.as.N[v0] = (short)(this.as.N[v0] + EntPlayer.e[(this.ez + 1) * 3 + this.as.Q[this.ez + 1]]);
                        if (this.as.N[this.ez + 1] > EntPlayer.e[(this.ez + 1) * 3 + this.as.Q[this.ez + 1]]) {
                            this.as.N[this.ez + 1] = EntPlayer.e[(this.ez + 1) * 3 + this.as.Q[this.ez + 1]];
                        }
                    } else {
                        this.cE = 0;
                        this.h = (byte)13;
                        this.v = null;
                        return;
                    }
                }
                this.cE = this.ez;
'@
            $shopFix = "`r`n" + $shopFix
            $text = [regex]::Replace(
                $text,
                '(?s)\s+if \(\(this\.as\.P & 1 << this\.ez \+ 1\) != 0\) /\* GOTO lbl47 \*/\s+if \(this\.bg >= EntPlayer\.i\[this\.ez\]\) \{.*?\r?\n\s+}\r?\n\s+this\.cE = this\.ez;',
                $shopFix,
                1)

            $crateFix = @'
                if (!(this.bA[var1_8] != -1 && this.bB[this.bA[var1_8]])) {
                    if (this.a(this.bu[var1_8], this.bv[var1_8] + this.bx[var1_8], var3_1, var4_2, var6_4 - var5_3, (int)var7_5, var8_6, (int)var9_7)) {
                        if (this.bw[var1_8] == 0) {
                            this.c(this.bu[var1_8] + (var3_1 >> 1), this.bv[var1_8] + this.bx[var1_8], 0);
                            this.c(this.bu[var1_8] + (var3_1 >> 1), this.bv[var1_8] + this.bx[var1_8], 0);
                        } else {
                            this.c(this.bu[var1_8] + (var3_1 >> 1), this.bv[var1_8] + this.bx[var1_8], this.bw[var1_8]);
                        }
                        this.k(var1_8);
                        this.e(this.bu[var1_8] + (var3_1 >> 1) << 8, this.bv[var1_8] + (var4_2 >> 1) + this.bx[var1_8] << 8, 30);
                        this.bw[var1_8] = -1;
                    } else {
                        var5_3 = 12;
                        var8_6 = 24;
                        var2_9 = GameCanvas.bj - 1;
                        while (var2_9 >= 0) {
                            if (this.ar[var2_9].Z >= 0) {
                                var6_4 = this.ar[var2_9].ab >> 8;
                                var7_5 = this.ar[var2_9].b();
                                if (var6_4 + 12 >= this.bu[var1_8] && var6_4 - 12 <= this.bu[var1_8] + var3_1 && this.a(this.bu[var1_8], this.bv[var1_8] + this.bx[var1_8], var3_1, var4_2, var6_4 - 12, (int)var7_5, 24, (int)var9_7)) {
                                    this.k(var1_8);
                                    this.bB[var1_8] = false;
                                    if (this.bw[var1_8] == 0) {
                                        this.c(this.bu[var1_8] + (var3_1 >> 1), this.bv[var1_8] + this.bx[var1_8], 0);
                                        this.c(this.bu[var1_8] + (var3_1 >> 1), this.bv[var1_8] + this.bx[var1_8], 0);
                                    } else {
                                        this.c(this.bu[var1_8] + (var3_1 >> 1), this.bv[var1_8] + this.bx[var1_8], this.bw[var1_8]);
                                    }
                                    this.e(this.bu[var1_8] + (var3_1 >> 1) << 8, this.bv[var1_8] + (var4_2 >> 1) + this.bx[var1_8] << 8, 30);
                                    this.bw[var1_8] = -1;
                                    break;
                                }
                            }
                            --var2_9;
                        }
                    }
                }
                if (this.bw[var1_8] != -1) {
                    v0 = var1_8;
                    this.bx[v0] = (short)(this.bx[v0] + 4);
                    if (this.bx[var1_8] + this.bv[var1_8] >= this.by[var1_8]) {
                        this.bv[var1_8] = this.by[var1_8];
                        this.bx[var1_8] = 0;
                        this.bB[var1_8] = false;
                    }
                }
            }
            --var1_8;
'@
            $crateFix = "`r`n" + $crateFix
            $text = [regex]::Replace(
                $text,
                '(?s)\s+if \(this\.bA\[var1_8\] != -1 && this\.bB\[this\.bA\[var1_8\]\]\) /\* GOTO lbl42 \*/.*?\r?\n\s+}\r?\n\s+}\r?\n\s+--var1_8;',
                $crateFix,
                1)

            $text = [regex]::Replace($text, "public final void run\(\) \{", "public final void run() {`r`n        int var1_1;", 1)
            $text = [regex]::Replace($text, "public final void l\(int var1_1, int var2_2\) \{", "public final void l(int var1_1, int var2_2) {`r`n        int var3_3;`r`n        int v0;", 1)
            $text = [regex]::Replace($text, "public final void v\(int var1_1, int var2_2\) \{", "public final void v(int var1_1, int var2_2) {`r`n        int var3_3;", 1)
            $text = [regex]::Replace($text, "public final void E\(\) \{", "public final void E() {`r`n        int var1_8;`r`n        int var2_9;`r`n        int var3_1;`r`n        int var4_2;`r`n        int var5_3;`r`n        int var6_4;`r`n        int var7_5;`r`n        int var8_6;`r`n        int var9_7;`r`n        int v0;", 1)
        } elseif ($_.Name -eq "SndManager.java") {
            $text = [regex]::Replace(
                $text,
                '(?s)public final byte\[\] a\(String string\) \{.*?\r?\n    /\*',
                "public final byte[] a(String string) {`r`n        ByteArrayOutputStream byteArrayOutputStream = null;`r`n        InputStream inputStream = null;`r`n        try {`r`n            inputStream = CanvasView.openResource(string);`r`n            byteArrayOutputStream = new ByteArrayOutputStream();`r`n            int n = inputStream.read();`r`n            while (n >= 0) {`r`n                byteArrayOutputStream.write(n);`r`n                n = inputStream.read();`r`n            }`r`n            return byteArrayOutputStream.toByteArray();`r`n        } catch (Exception exception) {`r`n            return null;`r`n        } finally {`r`n            try {`r`n                if (inputStream != null) inputStream.close();`r`n                if (byteArrayOutputStream != null) byteArrayOutputStream.close();`r`n            } catch (Exception exception) {}`r`n        }`r`n    }`r`n`r`n    /*",
                1)
        } elseif ($_.Name -eq "ratchetandclank.java") {
            $recordStoreFix = @'
RecordStore recordStore = RecordStore.openRecordStore((String)"RatchetJavBig", (boolean)false);
                recordStore.getRecord(k[3], this.p, 0);
                this.m = true;
                recordStore.closeRecordStore();
'@
            $text = [regex]::Replace(
                $text,
                'string\s*=\s*RecordStore\.openRecordStore\(\(String\)"RatchetJavBig", \(boolean\)false\);\s*string\.getRecord\(k\[3\], this\.p, 0\);\s*this\.m = true;\s*string\.closeRecordStore\(\);',
                $recordStoreFix,
                1)
            $text = [regex]::Replace(
                $text,
                "public static final Vector a\(String var0, int var1_1\) \{",
                "public static final Vector a(String var0, int var1_1) {`r`n        Vector<String> var2_2;`r`n        StringBuffer var3_3;`r`n        boolean var4_4;`r`n        int var5_5;`r`n        int var6_6;`r`n        int var7_7;`r`n        int var9_8;`r`n        int var10_9;`r`n        int var11_10;`r`n        char v0;`r`n        char var8_11;`r`n        char[] var12_12;",
                1)
        } elseif ($_.Name -eq "EntEnemy.java") {
            $text = $text.Replace("this.ac = -this.ac;", "this.ac = (short)(-this.ac);")
            $text = $text.Replace("this.ad = -EntEnemy.n[this.Z];", "this.ad = (short)(-EntEnemy.n[this.Z]);")
            $text = $text.Replace("this.ad = this.ao ? n[this.Z] : -n[this.Z];", "this.ad = (short)(this.ao ? n[this.Z] : -n[this.Z]);")
        } elseif ($_.Name -eq "EntPlayer.java") {
            $text = $text.Replace("this.ac = -EntPlayer.n;", "this.ac = (short)(-EntPlayer.n);")
            $text = $text.Replace("this.ad = -o;", "this.ad = (short)(-o);")
        }

        Set-Content -LiteralPath $path -Value $text -NoNewline
        Add-CanvasViewImport $path
    }
}

$toolsDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$projectRoot = (Resolve-Path (Join-Path $toolsDir "..")).Path
$appDir = Join-Path $projectRoot "going-mobile\app"
$assetsDir = Join-Path $appDir "src\main\assets"
$libsDir = Join-Path $appDir "libs"
$javaRoot = Join-Path $appDir "src\main\java"
$sourceDir = Join-Path $javaRoot "com\ratchetclank\goingmobile"
$tempDir = Join-Path $projectRoot "extracted"
$renamedJar = Join-Path $tempDir "going-mobile-renamed.jar"
$cfrJar = Join-Path $projectRoot "cfr.jar"

$resolvedJar = Resolve-JarPath $JarPath
$hash = Get-FileHash -Algorithm SHA256 -LiteralPath $resolvedJar

Write-Host "Preparing Ratchet & Clank: Going Mobile Android project"
Write-Host "JAR:    $resolvedJar"
Write-Host "SHA256: $($hash.Hash)"
Write-Host ""

if (-not (Test-Path -LiteralPath $cfrJar -PathType Leaf)) {
    throw "cfr.jar was not found at project root. Cannot generate source."
}

New-Item -ItemType Directory -Path $assetsDir -Force | Out-Null
New-Item -ItemType Directory -Path $libsDir -Force | Out-Null
New-CleanDirectory $assetsDir @(".gitkeep")
New-CleanDirectory $sourceDir @(".gitkeep", "GoingMobileActivity.java")

if (Test-Path -LiteralPath $tempDir) {
    Remove-Item -LiteralPath $tempDir -Recurse -Force
}
New-Item -ItemType Directory -Path $tempDir | Out-Null

Write-Host "[1/5] Extracting assets..."
Add-Type -AssemblyName System.IO.Compression.FileSystem
[System.IO.Compression.ZipFile]::ExtractToDirectory($resolvedJar, $tempDir)

$requiredClasses = @("ratchetandclank.class", "a.class", "b.class", "c.class", "d.class", "e.class", "f.class", "g.class", "h.class", "i.class")
$missingClasses = @($requiredClasses | Where-Object { -not (Test-Path -LiteralPath (Join-Path $tempDir $_) -PathType Leaf) })
if ($missingClasses.Count -gt 0) {
    throw "This does not look like the supported Going Mobile JAR. Missing: $($missingClasses -join ', ')"
}

$assetExtensions = @(".png", ".wav", ".mid", ".bin", ".dat", ".mu8")
$assetFiles = Get-ChildItem -LiteralPath $tempDir -File |
    Where-Object { $assetExtensions -contains $_.Extension.ToLowerInvariant() -or $_.Name -eq "mapData.txt" }
foreach ($asset in $assetFiles) {
    Copy-Item -LiteralPath $asset.FullName -Destination (Join-Path $assetsDir $asset.Name) -Force
}
Write-Host "      Copied $($assetFiles.Count) assets"

Write-Host "[2/5] Remapping obfuscated classes..."
Convert-GameJar $resolvedJar $renamedJar

Write-Host "[3/5] Decompiling remapped game source..."
if (-not $env:JAVA_HOME) {
    $androidStudioJbr = "C:\Program Files\Android\Android Studio\jbr"
    if (Test-Path -LiteralPath (Join-Path $androidStudioJbr "bin\java.exe")) {
        $env:JAVA_HOME = $androidStudioJbr
    }
}
$javaExe = if ($env:JAVA_HOME) { Join-Path $env:JAVA_HOME "bin\java.exe" } else { "java" }
& $javaExe -jar $cfrJar $renamedJar --outputdir $javaRoot --caseinsensitivefs true
if ($LASTEXITCODE -ne 0) {
    throw "CFR decompilation failed."
}

Write-Host "[4/5] Applying Android compatibility fixups..."
Apply-SourceFixups $sourceDir

Write-Host "[5/5] Cleaning temporary files..."
if (-not $KeepIntermediate) {
    Remove-Item -LiteralPath $tempDir -Recurse -Force
}
Remove-Item -LiteralPath (Join-Path $libsDir "going-mobile.jar") -Force -ErrorAction SilentlyContinue

if ($Build) {
    Write-Host ""
    Write-Host "Building debug APK..."
    Push-Location $projectRoot
    try {
        $wrapper = Join-Path $projectRoot "gradlew.bat"
        $wrapperJar = Join-Path $projectRoot "gradle\wrapper\gradle-wrapper.jar"
        if ((Test-Path -LiteralPath $wrapper) -and (Test-Path -LiteralPath $wrapperJar)) {
            & $wrapper ":going-mobile:app:assembleDebug"
        } elseif (Get-Command "gradle" -ErrorAction SilentlyContinue) {
            & gradle ":going-mobile:app:assembleDebug"
        } else {
            Write-Warning "No Gradle wrapper jar or system Gradle found. Open the project in Android Studio to build."
        }
        if ($LASTEXITCODE -ne 0) {
            throw "Gradle build failed."
        }
    } finally {
        Pop-Location
    }
}

Write-Host ""
Write-Host "Done."
Write-Host "Generated source: going-mobile/app/src/main/java/com/ratchetclank/goingmobile"
Write-Host "Assets:           going-mobile/app/src/main/assets"
Write-Host "Next: open this folder in Android Studio and run the app, or run Gradle assembleDebug."
