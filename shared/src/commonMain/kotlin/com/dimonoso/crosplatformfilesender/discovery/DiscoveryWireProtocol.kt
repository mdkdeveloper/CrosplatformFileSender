package com.dimonoso.crosplatformfilesender.discovery

private const val DiscoveryWireHeader = "CFS_DISCOVERY_V1"
private const val KeywordFingerprintSalt = "crosplatform-file-sender:discovery:v1:"

internal data class DiscoveryAnnouncement(
    val deviceId: String,
    val displayName: String,
    val platformName: String,
    val port: Int,
    val transferPort: Int = port + 1,
    val keywordFingerprint: String,
)

internal object DiscoveryMessageCodec {
    fun encode(announcement: DiscoveryAnnouncement): ByteArray =
        buildString {
            appendLine(DiscoveryWireHeader)
            appendLine("type=announce")
            appendLine("deviceId=${announcement.deviceId.escapeWireValue()}")
            appendLine("displayName=${announcement.displayName.escapeWireValue()}")
            appendLine("platformName=${announcement.platformName.escapeWireValue()}")
            appendLine("port=${announcement.port}")
            appendLine("transferPort=${announcement.transferPort}")
            appendLine("keywordFingerprint=${announcement.keywordFingerprint.escapeWireValue()}")
        }.encodeToByteArray()

    fun decode(payload: ByteArray): DiscoveryAnnouncement? {
        val lines = runCatching { payload.decodeToString().lineSequence().toList() }.getOrNull()
            ?: return null
        if (lines.firstOrNull() != DiscoveryWireHeader) return null

        val fields = lines
            .drop(1)
            .filter { it.isNotBlank() }
            .mapNotNull { line ->
                val separatorIndex = line.indexOf('=')
                if (separatorIndex <= 0) return@mapNotNull null

                val key = line.substring(0, separatorIndex)
                val value = line.substring(separatorIndex + 1).unescapeWireValue() ?: return null
                key to value
            }
            .toMap()

        if (fields["type"] != "announce") return null
        val port = fields["port"]?.toIntOrNull()?.takeIf { it in 1..65535 } ?: return null
        val transferPort = fields["transferPort"]?.toIntOrNull()?.takeIf { it in 1..65535 } ?: (port + 1).coerceAtMost(65535)

        return DiscoveryAnnouncement(
            deviceId = fields["deviceId"]?.takeIf { it.isNotBlank() } ?: return null,
            displayName = fields["displayName"]?.takeIf { it.isNotBlank() } ?: return null,
            platformName = fields["platformName"]?.takeIf { it.isNotBlank() } ?: return null,
            port = port,
            transferPort = transferPort,
            keywordFingerprint = fields["keywordFingerprint"]?.takeIf { it.isNotBlank() } ?: return null,
        )
    }
}

internal fun discoveryKeywordFingerprint(keyword: String): String =
    Sha256.digest((KeywordFingerprintSalt + keyword).encodeToByteArray()).toHexString()

private fun String.escapeWireValue(): String =
    buildString(length) {
        for (char in this@escapeWireValue) {
            when (char) {
                '%' -> append("%25")
                '\n' -> append("%0A")
                '\r' -> append("%0D")
                '=' -> append("%3D")
                else -> append(char)
            }
        }
    }

private fun String.unescapeWireValue(): String? =
    buildString(length) {
        var index = 0
        while (index < this@unescapeWireValue.length) {
            val char = this@unescapeWireValue[index]
            if (char != '%') {
                append(char)
                index += 1
                continue
            }

            if (index + 2 >= this@unescapeWireValue.length) return null
            val hex = this@unescapeWireValue.substring(index + 1, index + 3)
            val code = hex.toIntOrNull(16) ?: return null
            append(code.toChar())
            index += 3
        }
    }

private fun ByteArray.toHexString(): String {
    val hexChars = "0123456789abcdef"
    return buildString(size * 2) {
        for (byte in this@toHexString) {
            val value = byte.toInt() and 0xff
            append(hexChars[value ushr 4])
            append(hexChars[value and 0x0f])
        }
    }
}

private object Sha256 {
    private val constants = intArrayOf(
        0x428a2f98.toInt(),
        0x71374491.toInt(),
        0xb5c0fbcf.toInt(),
        0xe9b5dba5.toInt(),
        0x3956c25b.toInt(),
        0x59f111f1.toInt(),
        0x923f82a4.toInt(),
        0xab1c5ed5.toInt(),
        0xd807aa98.toInt(),
        0x12835b01.toInt(),
        0x243185be.toInt(),
        0x550c7dc3.toInt(),
        0x72be5d74.toInt(),
        0x80deb1fe.toInt(),
        0x9bdc06a7.toInt(),
        0xc19bf174.toInt(),
        0xe49b69c1.toInt(),
        0xefbe4786.toInt(),
        0x0fc19dc6.toInt(),
        0x240ca1cc.toInt(),
        0x2de92c6f.toInt(),
        0x4a7484aa.toInt(),
        0x5cb0a9dc.toInt(),
        0x76f988da.toInt(),
        0x983e5152.toInt(),
        0xa831c66d.toInt(),
        0xb00327c8.toInt(),
        0xbf597fc7.toInt(),
        0xc6e00bf3.toInt(),
        0xd5a79147.toInt(),
        0x06ca6351.toInt(),
        0x14292967.toInt(),
        0x27b70a85.toInt(),
        0x2e1b2138.toInt(),
        0x4d2c6dfc.toInt(),
        0x53380d13.toInt(),
        0x650a7354.toInt(),
        0x766a0abb.toInt(),
        0x81c2c92e.toInt(),
        0x92722c85.toInt(),
        0xa2bfe8a1.toInt(),
        0xa81a664b.toInt(),
        0xc24b8b70.toInt(),
        0xc76c51a3.toInt(),
        0xd192e819.toInt(),
        0xd6990624.toInt(),
        0xf40e3585.toInt(),
        0x106aa070.toInt(),
        0x19a4c116.toInt(),
        0x1e376c08.toInt(),
        0x2748774c.toInt(),
        0x34b0bcb5.toInt(),
        0x391c0cb3.toInt(),
        0x4ed8aa4a.toInt(),
        0x5b9cca4f.toInt(),
        0x682e6ff3.toInt(),
        0x748f82ee.toInt(),
        0x78a5636f.toInt(),
        0x84c87814.toInt(),
        0x8cc70208.toInt(),
        0x90befffa.toInt(),
        0xa4506ceb.toInt(),
        0xbef9a3f7.toInt(),
        0xc67178f2.toInt(),
    )

    fun digest(input: ByteArray): ByteArray {
        var h0 = 0x6a09e667
        var h1 = 0xbb67ae85.toInt()
        var h2 = 0x3c6ef372
        var h3 = 0xa54ff53a.toInt()
        var h4 = 0x510e527f
        var h5 = 0x9b05688c.toInt()
        var h6 = 0x1f83d9ab
        var h7 = 0x5be0cd19

        val bitLength = input.size.toLong() * 8L
        val paddedLength = (((input.size + 8) / 64) + 1) * 64
        val padded = ByteArray(paddedLength)
        input.copyInto(padded)
        padded[input.size] = 0x80.toByte()
        for (index in 0 until 8) {
            padded[paddedLength - 1 - index] = ((bitLength ushr (8 * index)) and 0xff).toByte()
        }

        val words = IntArray(64)
        for (chunkStart in padded.indices step 64) {
            for (index in 0 until 16) {
                val offset = chunkStart + index * 4
                words[index] =
                    ((padded[offset].toInt() and 0xff) shl 24) or
                        ((padded[offset + 1].toInt() and 0xff) shl 16) or
                        ((padded[offset + 2].toInt() and 0xff) shl 8) or
                        (padded[offset + 3].toInt() and 0xff)
            }
            for (index in 16 until 64) {
                words[index] =
                    smallSigma1(words[index - 2]) +
                        words[index - 7] +
                        smallSigma0(words[index - 15]) +
                        words[index - 16]
            }

            var a = h0
            var b = h1
            var c = h2
            var d = h3
            var e = h4
            var f = h5
            var g = h6
            var h = h7

            for (index in 0 until 64) {
                val temp1 = h + bigSigma1(e) + choose(e, f, g) + constants[index] + words[index]
                val temp2 = bigSigma0(a) + majority(a, b, c)
                h = g
                g = f
                f = e
                e = d + temp1
                d = c
                c = b
                b = a
                a = temp1 + temp2
            }

            h0 += a
            h1 += b
            h2 += c
            h3 += d
            h4 += e
            h5 += f
            h6 += g
            h7 += h
        }

        return intArrayOf(h0, h1, h2, h3, h4, h5, h6, h7).toByteArray()
    }

    private fun bigSigma0(value: Int): Int =
        value.rotateRight(2) xor value.rotateRight(13) xor value.rotateRight(22)

    private fun bigSigma1(value: Int): Int =
        value.rotateRight(6) xor value.rotateRight(11) xor value.rotateRight(25)

    private fun smallSigma0(value: Int): Int =
        value.rotateRight(7) xor value.rotateRight(18) xor (value ushr 3)

    private fun smallSigma1(value: Int): Int =
        value.rotateRight(17) xor value.rotateRight(19) xor (value ushr 10)

    private fun choose(value: Int, first: Int, second: Int): Int =
        (value and first) xor (value.inv() and second)

    private fun majority(first: Int, second: Int, third: Int): Int =
        (first and second) xor (first and third) xor (second and third)

    private fun Int.rotateRight(bitCount: Int): Int =
        (this ushr bitCount) or (this shl (32 - bitCount))

    private fun IntArray.toByteArray(): ByteArray {
        val output = ByteArray(size * 4)
        forEachIndexed { index, value ->
            val offset = index * 4
            output[offset] = (value ushr 24).toByte()
            output[offset + 1] = (value ushr 16).toByte()
            output[offset + 2] = (value ushr 8).toByte()
            output[offset + 3] = value.toByte()
        }
        return output
    }
}
