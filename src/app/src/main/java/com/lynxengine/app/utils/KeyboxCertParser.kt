package com.lynxengine.app.utils

import android.util.Log
import org.w3c.dom.Element
import java.io.ByteArrayInputStream
import java.security.cert.CertificateFactory
import java.security.cert.X509Certificate
import javax.xml.parsers.DocumentBuilderFactory

/**
 * Extracts verifiedBootHash from the leaf certificate inside a keybox XML.
 *
 * The leaf cert (first <Certificate> in the chain) contains a KeyDescription
 * extension (OID 1.3.6.1.4.1.11129.2.1.17) with a RootOfTrust sequence:
 *
 *   RootOfTrust ::= SEQUENCE {
 *       verifiedBootKey   OCTET STRING,
 *       deviceLocked      BOOLEAN,
 *       verifiedBootState ENUMERATED,
 *       verifiedBootHash  OCTET STRING   ← what we need
 *   }
 *
 * The result is a lowercase hex string — the same format used in
 * ro.boot.vbmeta.digest — so the two values will match exactly.
 */
object KeyboxCertParser {

    private const val TAG = "KeyboxCertParser"

    // OID for Android KeyDescription extension: 1.3.6.1.4.1.11129.2.1.17
    private val KEY_DESCRIPTION_OID = "1.3.6.1.4.1.11129.2.1.17"

    // ASN.1 tags
    private const val TAG_SEQUENCE  = 0x30
    private const val TAG_OCTET     = 0x04
    private const val TAG_BOOLEAN   = 0x01
    private const val TAG_ENUM      = 0x0A
    private const val TAG_INTEGER   = 0x02

    /**
     * Entry point. Returns the verifiedBootHash hex string, or null on any failure.
     */
    fun extractVerifiedBootHash(keyboxXml: String): String? {
        return runCatching {
            val leafPem = extractLeafCertPem(keyboxXml) ?: return null
            val cert    = parseCert(leafPem)            ?: return null
            extractHashFromCert(cert)
        }.onFailure {
            Log.e(TAG, "Failed to extract verifiedBootHash", it)
        }.getOrNull()
    }

    // ── Step 1: pull the first <Certificate> PEM out of the XML ──────────

    private fun extractLeafCertPem(xml: String): String? {
        val doc = DocumentBuilderFactory.newInstance()
            .newDocumentBuilder()
            .parse(ByteArrayInputStream(xml.toByteArray(Charsets.UTF_8)))

        doc.documentElement.normalize()

        val certs = doc.getElementsByTagName("Certificate")
        if (certs.length == 0) {
            Log.e(TAG, "No <Certificate> elements found")
            return null
        }

        // First certificate in the chain is the leaf / device cert
        val pem = (certs.item(0) as? Element)?.textContent?.trim()
        if (pem.isNullOrBlank()) {
            Log.e(TAG, "Leaf certificate element is empty")
            return null
        }

        return pem
    }

    // ── Step 2: decode PEM → X509Certificate ─────────────────────────────

    private fun parseCert(pem: String): X509Certificate? {
        // Strip PEM headers and decode base64
        val base64 = pem
            .replace("-----BEGIN CERTIFICATE-----", "")
            .replace("-----END CERTIFICATE-----", "")
            .replace("\\s".toRegex(), "")

        val der = android.util.Base64.decode(base64, android.util.Base64.DEFAULT)

        val factory = CertificateFactory.getInstance("X.509")
        return factory.generateCertificate(ByteArrayInputStream(der)) as? X509Certificate
    }

    // ── Step 3: find KeyDescription extension, walk ASN.1 ────────────────

    private fun extractHashFromCert(cert: X509Certificate): String? {
        val extBytes = cert.getExtensionValue(KEY_DESCRIPTION_OID)
        if (extBytes == null) {
            Log.e(TAG, "KeyDescription extension not found in leaf cert")
            return null
        }

        // getExtensionValue wraps the value in an OCTET STRING — unwrap it
        val inner = unwrapOctetString(extBytes) ?: return null

        // Now we have the raw KeyDescription SEQUENCE. Walk into it to find RootOfTrust.
        val rootOfTrust = findRootOfTrust(inner) ?: return null

        // RootOfTrust ::= SEQUENCE {
        //   verifiedBootKey   [0] OCTET STRING,
        //   deviceLocked      [1] BOOLEAN,
        //   verifiedBootState [2] ENUMERATED,
        //   verifiedBootHash  [3] OCTET STRING   ← index 3
        // }
        val hash = readRootOfTrustField(rootOfTrust, targetIndex = 3) ?: return null

        return hash.joinToString("") { "%02x".format(it) }
    }

    // ── ASN.1 helpers ─────────────────────────────────────────────────────

    /** Unwraps a DER OCTET STRING wrapper (used by getExtensionValue). */
    private fun unwrapOctetString(data: ByteArray): ByteArray? {
        if (data.isEmpty() || data[0].toInt() and 0xFF != TAG_OCTET) return null
        val (len, headerLen) = readLength(data, 1)
        return data.copyOfRange(headerLen, headerLen + len)
    }

    /**
     * The KeyDescription SEQUENCE layout (simplified) is:
     *   SEQUENCE {
     *     INTEGER  attestationVersion
     *     ENUM     attestationSecurityLevel
     *     INTEGER  keymasterVersion
     *     ENUM     keymasterSecurityLevel
     *     OCTET    attestationChallenge
     *     OCTET    uniqueId
     *     SEQUENCE softwareEnforced
     *     SEQUENCE teeEnforced      ← RootOfTrust lives inside here
     *   }
     *
     * We walk the top-level SEQUENCE children and look for a SEQUENCE child
     * that itself contains a SEQUENCE that matches the RootOfTrust shape.
     */
    private fun findRootOfTrust(keyDescDer: ByteArray): ByteArray? {
        val topChildren = parseSequenceChildren(keyDescDer) ?: return null

        for (child in topChildren) {
            if (child.isEmpty() || child[0].toInt() and 0xFF != TAG_SEQUENCE) continue

            // Candidate teeEnforced — walk its children looking for RootOfTrust
            val innerChildren = parseSequenceChildren(child) ?: continue
            for (inner in innerChildren) {
                if (inner.isEmpty() || inner[0].toInt() and 0xFF != TAG_SEQUENCE) continue
                // RootOfTrust starts with an OCTET STRING (verifiedBootKey)
                val rot = parseSequenceChildren(inner) ?: continue
                if (rot.size >= 4 && rot[0][0].toInt() and 0xFF == TAG_OCTET) {
                    return inner   // found it
                }
            }
        }
        return null
    }

    /**
     * Reads field at [targetIndex] inside a RootOfTrust SEQUENCE.
     * Returns the raw bytes of the value (not including tag/length).
     */
    private fun readRootOfTrustField(rootOfTrustDer: ByteArray, targetIndex: Int): ByteArray? {
        val children = parseSequenceChildren(rootOfTrustDer) ?: return null
        if (children.size <= targetIndex) return null
        val field = children[targetIndex]
        if (field.size < 2) return null
        val (len, headerLen) = readLength(field, 1)
        return field.copyOfRange(headerLen, headerLen + len)
    }

    /** Parses top-level TLV children of a DER SEQUENCE. */
    private fun parseSequenceChildren(der: ByteArray): List<ByteArray>? {
        if (der.isEmpty() || der[0].toInt() and 0xFF != TAG_SEQUENCE) return null
        val (seqLen, seqHeaderLen) = readLength(der, 1)

        val children = mutableListOf<ByteArray>()
        var pos = seqHeaderLen
        val end = seqHeaderLen + seqLen

        while (pos < end) {
            if (pos >= der.size) break
            val (childLen, childHeaderLen) = readLength(der, pos + 1)
            val totalLen = 1 + childHeaderLen + childLen - (pos + 1) // tag + header + value
            val childBytes = der.copyOfRange(pos, pos + 1 + (childHeaderLen - 1) + childLen)
            children.add(childBytes)
            pos += 1 + (childHeaderLen - 1) + childLen
        }

        return children
    }

    /**
     * Reads a DER length field starting at [offset].
     * Returns Pair(length, nextOffset).
     */
    private fun readLength(data: ByteArray, offset: Int): Pair<Int, Int> {
        val first = data[offset].toInt() and 0xFF
        return if (first < 0x80) {
            Pair(first, offset + 1)
        } else {
            val numBytes = first and 0x7F
            var len = 0
            for (i in 0 until numBytes) {
                len = (len shl 8) or (data[offset + 1 + i].toInt() and 0xFF)
            }
            Pair(len, offset + 1 + numBytes)
        }
    }
}
