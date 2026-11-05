package com.jdroidx.jfindme.utils

import android.content.Context
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.DataInputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.RandomAccessFile
import java.security.MessageDigest
import java.util.zip.GZIPInputStream

/**
 * 🦋 JDeltaPatcher: Enterprise Android Differential Binary Reconstruction Engine.
 * 
 * Reconstructs a bit-for-bit identical target APK using the running app's installed base.apk
 * and a lightweight delta patch file (~1-3 MB) downloaded from GitHub/Cloud.
 * 
 * Security:
 * - Validates cryptographic SHA-256 of the source base.apk before patching.
 * - Validates cryptographic SHA-256 of the reconstructed target APK before install handshake.
 * - Fails safely with zero side effects, allowing immediate fallback to full APK download.
 */
object JDeltaPatcher {

    private const val TAG = "JDeltaPatcher"
    private val MAGIC = "JDELTA01".toByteArray(Charsets.US_ASCII)

    private const val OP_COPY: Byte = 0x01
    private const val OP_INSERT: Byte = 0x02
    private const val OP_END: Byte = 0xFF.toByte()
    private const val BUFFER_SIZE = 65536

    /**
     * Reconstructs the target APK from the installed source APK and the delta patch.
     * 
     * @param sourceApk The currently installed APK (usually context.applicationInfo.sourceDir)
     * @param patchFile The downloaded .delta patch file
     * @param targetApk The output file where the reconstructed APK will be written
     * @return True if reconstruction and SHA-256 verification succeeded 100%, false otherwise.
     */
    @JvmStatic
    fun applyPatch(sourceApk: File, patchFile: File, targetApk: File): Boolean {
        if (!sourceApk.exists() || !patchFile.exists()) {
            SystemLogger.e(TAG, "Source APK or Patch file does not exist.")
            return false
        }

        var sourceRaf: RandomAccessFile? = null
        var patchIn: FileInputStream? = null
        var targetOut: BufferedOutputStream? = null

        try {
            patchIn = FileInputStream(patchFile)
            val headerStream = DataInputStream(patchIn)

            // 1. Verify Magic Header
            val magicBuf = ByteArray(8)
            headerStream.readFully(magicBuf)
            if (!magicBuf.contentEquals(MAGIC)) {
                SystemLogger.e(TAG, "DELTA_CORRUPT: Invalid magic header in patch file.")
                return false
            }

            // 2. Read Expected Hashes
            val expectedSourceSha = ByteArray(32)
            headerStream.readFully(expectedSourceSha)

            val expectedTargetSha = ByteArray(32)
            headerStream.readFully(expectedTargetSha)

            val expectedTargetLength = headerStream.readLong()

            // 3. Verify Source APK SHA-256 against Expected
            val actualSourceSha = computeSha256(sourceApk)
            if (!actualSourceSha.contentEquals(expectedSourceSha)) {
                SystemLogger.w(TAG, "DELTA_MISMATCH: Installed base.apk SHA-256 does not match patch baseline.")
                return false
            }

            // 4. Prepare Destination
            if (targetApk.exists()) {
                targetApk.delete()
            }
            targetApk.parentFile?.mkdirs()

            sourceRaf = RandomAccessFile(sourceApk, "r")
            targetOut = BufferedOutputStream(FileOutputStream(targetApk), BUFFER_SIZE)

            // 5. Decode Compressed Operations Stream
            val gzipIn = GZIPInputStream(BufferedInputStream(patchIn, BUFFER_SIZE))
            val opsIn = DataInputStream(gzipIn)

            val buffer = ByteArray(BUFFER_SIZE)
            var totalWritten: Long = 0

            while (true) {
                val op = opsIn.readByte()
                if (op == OP_END) {
                    break
                }

                when (op) {
                    OP_COPY -> {
                        val srcOffset = opsIn.readLong()
                        val length = opsIn.readInt()
                        sourceRaf.seek(srcOffset)

                        var remaining = length
                        while (remaining > 0) {
                            val toRead = if (remaining > buffer.size) buffer.size else remaining
                            sourceRaf.readFully(buffer, 0, toRead)
                            targetOut.write(buffer, 0, toRead)
                            remaining -= toRead
                            totalWritten += toRead
                        }
                    }
                    OP_INSERT -> {
                        val length = opsIn.readInt()
                        var remaining = length
                        while (remaining > 0) {
                            val toRead = if (remaining > buffer.size) buffer.size else remaining
                            opsIn.readFully(buffer, 0, toRead)
                            targetOut.write(buffer, 0, toRead)
                            remaining -= toRead
                            totalWritten += toRead
                        }
                    }
                    else -> {
                        SystemLogger.e(TAG, "DELTA_CORRUPT: Unknown opcode: $op")
                        return false
                    }
                }
            }

            targetOut.flush()
            targetOut.close()
            targetOut = null

            // 6. Verify Target Length & SHA-256
            if (totalWritten != expectedTargetLength) {
                SystemLogger.e(TAG, "DELTA_SIZE_MISMATCH: Expected $expectedTargetLength bytes, wrote $totalWritten bytes.")
                if (targetApk.exists()) targetApk.delete()
                return false
            }

            val actualTargetSha = computeSha256(targetApk)
            if (!actualTargetSha.contentEquals(expectedTargetSha)) {
                SystemLogger.e(TAG, "DELTA_TARGET_SHA_MISMATCH: Reconstructed APK checksum verification failed.")
                if (targetApk.exists()) targetApk.delete()
                return false
            }

            SystemLogger.i(TAG, "🎉 DELTA_RECONSTRUCTION_SUCCESS: Reconstructed $totalWritten bytes with exact SHA-256 match.")
            return true

        } catch (e: Exception) {
            SystemLogger.e(TAG, "Delta patch execution error: ${e.message}", e)
            if (targetApk.exists()) targetApk.delete()
            return false
        } finally {
            try { targetOut?.close() } catch (ignored: Exception) {}
            try { sourceRaf?.close() } catch (ignored: Exception) {}
            try { patchIn?.close() } catch (ignored: Exception) {}
        }
    }

    /**
     * Computes the SHA-256 digest of a given file.
     */
    @JvmStatic
    fun computeSha256(file: File): ByteArray {
        val md = MessageDigest.getInstance("SHA-256")
        FileInputStream(file).use { fis ->
            val buf = ByteArray(BUFFER_SIZE)
            var n: Int
            while (fis.read(buf).also { n = it } != -1) {
                md.update(buf, 0, n)
            }
        }
        return md.digest()
    }

    /**
     * Converts a byte array to hex string.
     */
    @JvmStatic
    fun bytesToHex(bytes: ByteArray): String {
        val sb = StringBuilder()
        for (b in bytes) {
            sb.append(String.format("%02x", b))
        }
        return sb.toString()
    }
}
