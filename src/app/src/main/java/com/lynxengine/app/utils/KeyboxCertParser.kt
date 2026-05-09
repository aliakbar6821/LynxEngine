package com.lynxengine.app.utils

import android.util.Base64
import android.util.Log
import org.w3c.dom.Element
import java.io.ByteArrayInputStream
import java.security.cert.CertificateFactory
import java.security.cert.X509Certificate
import javax.xml.parsers.DocumentBuilderFactory

object KeyboxCertParser {

    private const val TAG = "KeyboxCertParser"
    private const val KEY_DESCRIPTION_OID = "1.3.6.1.4.1.11129.2.1.17"

    fun extractVerifiedBootHash(keyboxXml: String): String? {
        return runCatching {
            val pem = extractLeafCertPem(keyboxXml) ?: run {
                Log.e(TAG, "No leaf cert found in keybox")
                return null
            }
            val cert = parseCert(pem) ?: run {
                Log.e(TAG, "Failed to parse leaf cert")
                return null
            }
            val hash = extractHashBouncyCastle(cert)
            if (hash != null) {
                Log.d(TAG, "Extracted verifiedBootHash: $hash")
            } else {
                Log.e(TAG, "Failed to extract hash from cert extension")
            }
            hash
        }.onFailure {
            Log.e(TAG, "extractVerifiedBootHash failed", it)
        }.getOrNull()
    }

    private fun extractLeafCertPem(xml: String): String? {
        val doc = DocumentBuilderFactory.newInstance()
            .newDocumentBuilder()
            .parse(ByteArrayInputStream(xml.toByteArray(Charsets.UTF_8)))
        doc.documentElement.normalize()
        val certs = doc.getElementsByTagName("Certificate")
        if (certs.length == 0) return null
        return (certs.item(0) as? Element)?.textContent?.trim()
    }

    private fun parseCert(pem: String): X509Certificate? {
        val base64 = pem
            .replace("-----BEGIN CERTIFICATE-----", "")
            .replace("-----END CERTIFICATE-----", "")
            .replace("\\s".toRegex(), "")
        val der = Base64.decode(base64, Base64.DEFAULT)
        return CertificateFactory.getInstance("X.509")
            .generateCertificate(ByteArrayInputStream(der)) as? X509Certificate
    }

    // ── BouncyCastle-style ASN.1 walking ─────────────────────────────────────
    private fun extractHashBouncyCastle(cert: X509Certificate): String? {
        // getExtensionValue returns DER-encoded OCTET STRING wrapping the extension value
        val extBytes = cert.getExtensionValue(KEY_DESCRIPTION_OID) ?: run {
            Log.e(TAG, "KeyDescription extension not found (OID $KEY_DESCRIPTION_OID)")
            return null
        }

        Log.d(TAG, "Extension raw bytes length: ${extBytes.size}")

        // Unwrap outer OCTET STRING (tag 0x04)
        val inner = unwrapOctetString(extBytes) ?: run {
            Log.e(TAG, "Failed to unwrap outer OCTET STRING")
            return null
        }

        Log.d(TAG, "Inner bytes length: ${inner.size}")

        // inner is now the KeyDescription SEQUENCE
        // KeyDescription layout:
        //   SEQUENCE {
        //     INTEGER  attestationVersion       [0]
        //     ENUM     attestationSecurityLevel [1]
        //     INTEGER  keymasterVersion         [2]
        //     ENUM     keymasterSecurityLevel   [3]
        //     OCTET    attestationChallenge     [4]
        //     OCTET    uniqueId                 [5]
        //     SEQUENCE softwareEnforced         [6]
        //     SEQUENCE teeEnforced              [7]  ← RootOfTrust lives here
        //   }

        val topChildren = parseSequenceChildren(inner) ?: run {
            Log.e(TAG, "Failed to parse KeyDescription top-level SEQUENCE")
            return null
        }

        Log.d(TAG, "KeyDescription has ${topChildren.size} top-level children")

        if (topChildren.size < 8) {
            Log.e(TAG, "KeyDescription has fewer than 8 children: ${topChildren.size}")
            return null
        }

        // Child at index 7 is teeEnforced — a SEQUENCE of tagged objects
        val teeEnforced = topChildren[7]
        Log.d(TAG, "teeEnforced tag: 0x${(teeEnforced[0].toInt() and 0xFF).toString(16)}, length: ${teeEnforced.size}")

        val teeChildren = parseSequenceChildren(teeEnforced) ?: run {
            Log.e(TAG, "Failed to parse teeEnforced SEQUENCE")
            return null
        }

        Log.d(TAG, "teeEnforced has ${teeChildren.size} children")

        // Find RootOfTrust — it's a context-tagged [704] SEQUENCE
        // Tag 704 = 0x2C0 in base, but encoded as context-specific constructed:
        // 0xBF + length-of-tag-number + tag-number
        // OR as a tagged SEQUENCE directly
        // In practice it's encoded as [704] EXPLICIT SEQUENCE
        // which appears as a tagged object with tag number 704 (0x2C0)

        for ((idx, child) in teeChildren.withIndex()) {
            val tag = child[0].toInt() and 0xFF
            Log.d(TAG, "teeEnforced child[$idx]: tag=0x${tag.toString(16)}, len=${child.size}")

            // RootOfTrust is tagged [704] — look for it
            // Context-specific constructed with tag 704
            val tagNo = getTagNumber(child)
            Log.d(TAG, "  tagNo=$tagNo")

            if (tagNo == 704) {
                Log.d(TAG, "Found RootOfTrust at teeEnforced child[$idx]")
                val rotBytes = getTaggedObjectContent(child) ?: continue

                // RootOfTrust SEQUENCE:
                //   OCTET STRING verifiedBootKey   [0]
                //   BOOLEAN      deviceLocked      [1]
                //   ENUM         verifiedBootState [2]
                //   OCTET STRING verifiedBootHash  [3]

                val rotChildren = parseSequenceChildren(rotBytes) ?: run {
                    Log.e(TAG, "Failed to parse RootOfTrust SEQUENCE")
                    return null
                }

                Log.d(TAG, "RootOfTrust has ${rotChildren.size} children")

                if (rotChildren.size < 4) {
                    Log.e(TAG, "RootOfTrust has fewer than 4 children")
                    return null
                }

                // verifiedBootHash is at index 3
                val hashField = rotChildren[3]
                val hashBytes = getValueBytes(hashField) ?: run {
                    Log.e(TAG, "Failed to get verifiedBootHash value bytes")
                    return null
                }

                return hashBytes.joinToString("") { "%02x".format(it) }
            }
        }

        Log.e(TAG, "RootOfTrust [704] not found in teeEnforced")
        return null
    }

    // ── ASN.1 helpers ─────────────────────────────────────────────────────────

    private fun unwrapOctetString(data: ByteArray): ByteArray? {
        if (data.isEmpty()) return null
        val tag = data[0].toInt() and 0xFF
        if (tag != 0x04) {
            Log.w(TAG, "unwrapOctetString: expected tag 0x04, got 0x${tag.toString(16)}")
            // Try treating as-is (some implementations don't double-wrap)
            return data
        }
        val (len, headerLen) = readLength(data, 1)
        if (headerLen + len > data.size) return null
        return data.copyOfRange(headerLen, headerLen + len)
    }

    private fun parseSequenceChildren(der: ByteArray): List<ByteArray>? {
        if (der.isEmpty()) return null

        val tag = der[0].toInt() and 0xFF

        // Accept SEQUENCE (0x30) or any constructed tag
        val isConstructed = (tag and 0x20) != 0
        if (!isConstructed) {
            Log.w(TAG, "parseSequenceChildren: tag 0x${tag.toString(16)} is not constructed")
            return null
        }

        val (seqLen, seqHeaderLen) = readLength(der, 1)
        val end = seqHeaderLen + seqLen

        if (end > der.size) {
            Log.e(TAG, "parseSequenceChildren: declared length $seqLen exceeds buffer ${der.size - seqHeaderLen}")
            return null
        }

        val children = mutableListOf<ByteArray>()
        var pos = seqHeaderLen

        while (pos < end) {
            if (pos >= der.size) break

            // Read tag (may be multi-byte)
            val tagStart = pos
            pos++ // skip first tag byte

            // Handle long-form tag
            val firstTagByte = der[tagStart].toInt() and 0xFF
            if ((firstTagByte and 0x1F) == 0x1F) {
                // Long form tag — skip continuation bytes
                while (pos < end && (der[pos].toInt() and 0x80) != 0) pos++
                pos++ // skip last tag byte
            }

            if (pos >= end) break

            val (childLen, childHeaderOffset) = readLength(der, pos)
            pos = childHeaderOffset

            val totalChildLen = (pos - tagStart) + childLen
            if (tagStart + totalChildLen > der.size) break

            children.add(der.copyOfRange(tagStart, tagStart + totalChildLen))
            pos += childLen
        }

        return children
    }

    private fun getTagNumber(tlv: ByteArray): Int {
        if (tlv.isEmpty()) return -1
        val firstByte = tlv[0].toInt() and 0xFF
        val baseTag = firstByte and 0x1F

        return if (baseTag != 0x1F) {
            // Short form tag
            baseTag
        } else {
            // Long form tag — read continuation bytes
            var tagNo = 0
            var i = 1
            while (i < tlv.size) {
                val b = tlv[i].toInt() and 0xFF
                tagNo = (tagNo shl 7) or (b and 0x7F)
                i++
                if ((b and 0x80) == 0) break
            }
            tagNo
        }
    }

    private fun getTaggedObjectContent(tlv: ByteArray): ByteArray? {
        // Skip tag bytes
        if (tlv.isEmpty()) return null
        var pos = 1
        val firstByte = tlv[0].toInt() and 0xFF
        if ((firstByte and 0x1F) == 0x1F) {
            while (pos < tlv.size && (tlv[pos].toInt() and 0x80) != 0) pos++
            pos++ // last tag byte
        }

        val (contentLen, headerEnd) = readLength(tlv, pos)
        if (headerEnd + contentLen > tlv.size) return null

        val content = tlv.copyOfRange(headerEnd, headerEnd + contentLen)

        // If the content starts with a SEQUENCE tag, return as-is for parseSequenceChildren
        // If it's implicit, wrap it as a SEQUENCE
        if (content.isEmpty()) return null

        val innerTag = content[0].toInt() and 0xFF
        return if (innerTag == 0x30) {
            content // already a SEQUENCE
        } else {
            // Wrap in SEQUENCE for uniform parsing
            val wrapped = ByteArray(content.size + 2)
            wrapped[0] = 0x30
            wrapped[1] = content.size.toByte()
            System.arraycopy(content, 0, wrapped, 2, content.size)
            wrapped
        }
    }

    private fun getValueBytes(tlv: ByteArray): ByteArray? {
        if (tlv.size < 2) return null
        var pos = 1
        val firstByte = tlv[0].toInt() and 0xFF
        if ((firstByte and 0x1F) == 0x1F) {
            while (pos < tlv.size && (tlv[pos].toInt() and 0x80) != 0) pos++
            pos++
        }
        val (len, headerEnd) = readLength(tlv, pos)
        if (headerEnd + len > tlv.size) return null
        return tlv.copyOfRange(headerEnd, headerEnd + len)
    }

    private fun readLength(data: ByteArray, offset: Int): Pair<Int, Int> {
        if (offset >= data.size) return Pair(0, offset)
        val first = data[offset].toInt() and 0xFF
        return if (first < 0x80) {
            Pair(first, offset + 1)
        } else {
            val numBytes = first and 0x7F
            var len = 0
            for (i in 0 until numBytes) {
                if (offset + 1 + i >= data.size) break
                len = (len shl 8) or (data[offset + 1 + i].toInt() and 0xFF)
            }
            Pair(len, offset + 1 + numBytes)
        }
    }
}