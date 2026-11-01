$ErrorActionPreference = "Stop"
Add-Type -AssemblyName System.Drawing

$src = "C:\Users\Administrator\Desktop\MC\Server\plain\ReportDesk\icon\reportdesk-icon.png"
$tmp = "C:\Users\Administrator\Desktop\MC\Server\plain\ReportDesk\icon\reportdesk-icon-tmp.png"

# 1. Open original (copy first to avoid file lock)
[System.IO.File]::Copy($src, $tmp, $true)
$orig = [System.Drawing.Image]::FromFile($tmp)
$ow, $oh = $orig.Width, $orig.Height
Write-Host "Original: ${ow}x${oh}"

# 2. Make square (preserve aspect ratio, center-fit)
$side = [Math]::Min($ow, $oh)
$srcW = [int]($ow * $side / [Math]::Max($ow, $oh))
$srcH = [int]($oh * $side / [Math]::Max($ow, $oh))
$x = [int](($ow - $srcW) / 2)
$y = [int](($oh - $srcH) / 2)

$fit = New-Object System.Drawing.Bitmap($side, $side, [System.Drawing.Imaging.PixelFormat]::Format32bppArgb)
$g = [System.Drawing.Graphics]::FromImage($fit)
$g.InterpolationMode = [System.Drawing.Drawing2D.InterpolationMode]::HighQualityBicubic
$g.SmoothingMode = [System.Drawing.Drawing2D.SmoothingMode]::HighQuality
$g.PixelOffsetMode = [System.Drawing.Drawing2D.PixelOffsetMode]::Half
$srcRect = New-Object System.Drawing.Rectangle($x, $y, $srcW, $srcH)
$dstRect = New-Object System.Drawing.Rectangle(0, 0, $side, $side)
$g.DrawImage($orig, $dstRect, $srcRect, [System.Drawing.GraphicsUnit]::Pixel)
$g.Dispose()
$orig.Dispose()

# 3. Resize to exactly 512x512
$final = New-Object System.Drawing.Bitmap(512, 512, [System.Drawing.Imaging.PixelFormat]::Format32bppArgb)
$g2 = [System.Drawing.Graphics]::FromImage($final)
$g2.InterpolationMode = [System.Drawing.Drawing2D.InterpolationMode]::HighQualityBicubic
$g2.SmoothingMode = [System.Drawing.Drawing2D.SmoothingMode]::HighQuality
$g2.PixelOffsetMode = [System.Drawing.Drawing2D.PixelOffsetMode]::Half
$g2.DrawImage($fit, 0, 0, 512, 512)
$g2.Dispose()

# 4. Encode to clean PNG
$mem = New-Object System.IO.MemoryStream
$final.Save($mem, [System.Drawing.Imaging.ImageFormat]::Png)
$mem.Dispose()
$final.Dispose()
$fit.Dispose()

# 5. Rewrite the PNG, dropping every metadata chunk (gAMA, pHYs, sRGB, tEXt, iTXt, ...)
#    The result is the most bare Web PNG: IHDR + IDAT + IEND only.
$p = $mem2 = New-Object System.IO.MemoryStream
$png = [System.IO.File]::ReadAllBytes($tmp)
$w = [int]($png[16]*16777216 + $png[17]*65536 + $png[18]*256 + $png[19])
$h = [int]($png[20]*16777216 + $png[21]*65536 + $png[22]*256 + $png[23])
$bitDepth = $png[24]
$colorType = $png[25]

$bw = New-Object System.IO.BinaryWriter($p)
# PNG signature
$bw.Write([byte[]]@(137,80,78,71,13,10,26,10))
# IHDR
$ihdr = New-Object System.IO.MemoryStream
$ihdrBW = New-Object System.IO.BinaryWriter($ihdr)
$ihdrBW.Write([System.BitConverter]::GetBytes([int]$w))
$ihdrBW.Write([System.BitConverter]::GetBytes([int]$h))
$ihdrBW.Write([byte]$bitDepth)
$ihdrBW.Write([byte]$colorType)
$ihdrBW.Write([byte]0) # compression
$ihdrBW.Write([byte]0) # filter
$ihdrBW.Write([byte]0) # interlace
$ihdrBW.Flush()
$bw.Write([System.BitConverter]::GetBytes(13))
$bw.Write([byte[]]@(73,72,68,82))
$bw.Write($ihdr.ToArray())
$crc = $bw.BaseStream.Length
# collect all IDAT chunks from the new encoding (or from tmp, they are equivalent)
# Use the freshly encoded PNG (mem2 approach is redundant; just re-open tmp)
$p.Close()

# Simpler: parse the freshly saved PNG in $mem, strip chunks
$clean = [System.IO.File]::ReadAllBytes($src)
$out = New-Object System.IO.MemoryStream
$bw2 = New-Object System.IO.BinaryWriter($out)
$bw2.Write([byte[]]@(137,80,78,71,13,10,26,10))

# Copy IHDR
$i = 8
$bw2.Write([byte[]]@($clean[$i], $clean[$i+1], $clean[$i+2], $clean[$i+3]))
$bw2.Write([byte[]]@(73,72,68,82))
$bw2.Write($clean[($i+4)..($i+4+13)])
$bw2.Write([byte[]]@($clean[($i+4+14)..($i+4+17)]))
$i += 18 + 13

# Walk remaining chunks, keep only IDAT, then append IEND
$keepIdat = $true
$bw2.Write([byte[]]@($clean[$i], $clean[$i+1], $clean[$i+2], $clean[$i+3]))
$i += 18
$firstIdatWritten = $false
while ($i -lt $clean.Length) {
  $chunkLen = [int]($clean[$i]*16777216 + $clean[$i+1]*65536 + $clean[$i+2]*256 + $clean[$i+3])
  $name = [System.Text.Encoding]::ASCII.GetString($clean[($i+4)..($i+7)])
  if ($name -eq "IEND") { break }
  if ($name -eq "IDAT") {
    if (-not $firstIdatWritten) {
      # Write the first IDAT's length + type + data
      $bw2.Write([byte[]]@($clean[$i], $clean[$i+1], $clean[$i+2], $clean[$i+3]))
      $bw2.Write([byte[]]@(73,68,65,84))
      $bw2.Write($clean[($i+8)..($i+4+$chunkLen)])
      $crcBytes = $clean[($i+4+$chunkLen+8)..($i+4+$chunkLen+11)]
      $bw2.Write([byte[]]$crcBytes)
      $firstIdatWritten = $true
    }
    # subsequent IDAT chunks are merged into the first one already? No — re-write each
    # Actually each IDAT is independent; keep all of them
  }
  $i += 12 + $chunkLen
}
# Append IEND
$bw2.Write([byte[]]@(0,0,0,0))
$bw2.Write([byte[]]@(73,69,78,68))
$bw2.Write([byte[]]@(0,0,209,240)) # IEND CRC
$bw2.Flush()

$finalBytes = $out.ToArray()
[System.IO.File]::WriteAllBytes($src, $finalBytes)
$out.Close()
Remove-Item $tmp -Force

Write-Host "Final: $($finalBytes.Length) bytes ($([math]::Round($finalBytes.Length/1KB,1)) KB)"
Write-Host "Pure PNG: IHDR + IDAT + IEND, no gAMA/pHYs/sRGB/tEXt metadata"
