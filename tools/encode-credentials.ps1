# 重新生成 BilibiliClient.java 里混淆后的凭据常量。
# 用法：
#   .\tools\encode-credentials.ps1 -AccessKeyId xxx -AccessSecret yyy
# 把输出的两行 ACCESS_KEY_ID / ACCESS_SECRET 替换到
# */shared/src/main/java/net/ming/bilibilichatmcforge/utils/BilibiliClient.java 即可。
param(
    [Parameter(Mandatory = $true)][string]$AccessKeyId,
    [Parameter(Mandatory = $true)][string]$AccessSecret,
    [int]$IdKey = 0x3C,
    [int]$SecretKey = 0x5F
)

function Mask([string]$text, [int]$key) {
    $bytes = [System.Text.Encoding]::UTF8.GetBytes($text)
    for ($i = 0; $i -lt $bytes.Length; $i++) {
        # 与 Java 端 unmask() 保持一致：逐字节异或 key 和位置因子
        $bytes[$i] = $bytes[$i] -bxor $key -bxor (($i * 31) -band 0xFF)
    }
    return [Convert]::ToBase64String($bytes)
}

$idLine = 'private static final String ACCESS_KEY_ID = unmask("{0}", 0x{1:X2});' -f (Mask $AccessKeyId $IdKey), $IdKey
$secretLine = 'private static final String ACCESS_SECRET = unmask("{0}", 0x{1:X2});' -f (Mask $AccessSecret $SecretKey), $SecretKey

Write-Output $idLine
Write-Output $secretLine
