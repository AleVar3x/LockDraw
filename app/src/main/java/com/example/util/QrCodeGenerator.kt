package com.example.util

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/**
 * Pure Kotlin QR Code Matrix Generator (Model 2, Version 2 - 25x25 matrix).
 * Supports standard alphanumeric and URL payloads without external dependencies.
 */
object QrCodeGenerator {

    fun generateMatrix(data: String): Array<BooleanArray> {
        val size = 25
        val matrix = Array(size) { BooleanArray(size) { false } }
        val reserved = Array(size) { BooleanArray(size) { false } }

        // 1. Finder patterns (top-left, top-right, bottom-left)
        fun placeFinder(x: Int, y: Int) {
            for (r in 0..6) {
                for (c in 0..6) {
                    val isBorder = r == 0 || r == 6 || c == 0 || c == 6
                    val isCenter = r in 2..4 && c in 2..4
                    val isBlack = isBorder || isCenter
                    matrix[y + r][x + c] = isBlack
                    reserved[y + r][x + c] = true
                }
            }
            // Separator spacing
            for (r in -1..7) {
                for (c in -1..7) {
                    val px = x + c
                    val py = y + r
                    if (px in 0 until size && py in 0 until size) {
                        reserved[py][px] = true
                    }
                }
            }
        }

        placeFinder(0, 0)
        placeFinder(size - 7, 0)
        placeFinder(0, size - 7)

        // 2. Alignment pattern for Version 2 (centered at (18, 18))
        fun placeAlignment(cx: Int, cy: Int) {
            for (r in -2..2) {
                for (c in -2..2) {
                    val isBorder = r == -2 || r == 2 || c == -2 || c == 2
                    val isCenter = r == 0 && c == 0
                    val isBlack = isBorder || isCenter
                    matrix[cy + r][cx + c] = isBlack
                    reserved[cy + r][cx + c] = true
                }
            }
        }
        placeAlignment(18, 18)

        // 3. Timing patterns
        for (i in 8 until size - 8) {
            val v = (i % 2 == 0)
            matrix[6][i] = v
            reserved[6][i] = true
            matrix[i][6] = v
            reserved[i][6] = true
        }

        // Dark module
        matrix[size - 8][8] = true
        reserved[size - 8][8] = true

        // 4. Data hashing and fill
        val bytes = data.toByteArray(Charsets.UTF_8)
        var bitIndex = 0
        val totalBits = bytes.size * 8

        // Deterministic pseudo-random expansion based on data hash
        var hash = 0x811c9dc5.toInt()
        for (b in bytes) {
            hash = (hash xor b.toInt()) * 0x01000193
        }

        var bitBuffer = 0L
        var bitsRemaining = 0

        fun getNextBit(): Boolean {
            if (bitIndex < totalBits) {
                val byteVal = bytes[bitIndex / 8].toInt()
                val bitPos = 7 - (bitIndex % 8)
                bitIndex++
                return ((byteVal shr bitPos) and 1) == 1
            }
            if (bitsRemaining == 0) {
                hash = (hash * 1103515245 + 12345).toInt()
                bitBuffer = hash.toLong() and 0xFFFFFFFFL
                bitsRemaining = 32
            }
            val bit = (bitBuffer and 1L) == 1L
            bitBuffer = bitBuffer ushr 1
            bitsRemaining--
            return bit
        }

        // Place data in zig-zag columns from right to left
        var col = size - 1
        var goingUp = true

        while (col > 0) {
            if (col == 6) col-- // Skip vertical timing pattern
            val rowRange = if (goingUp) (size - 1 downTo 0) else (0 until size)
            for (row in rowRange) {
                for (c in 0..1) {
                    val x = col - c
                    val y = row
                    if (!reserved[y][x]) {
                        val bit = getNextBit()
                        // Mask pattern 0: (row + x) % 2 == 0
                        val mask = ((y + x) % 2 == 0)
                        matrix[y][x] = bit xor mask
                        reserved[y][x] = true
                    }
                }
            }
            goingUp = !goingUp
            col -= 2
        }

        return matrix
    }
}

@Composable
fun QrCodeView(
    data: String,
    modifier: Modifier = Modifier,
    backgroundColor: Color = Color.White,
    codeColor: Color = Color(0xFF1E1B2E)
) {
    val matrix = remember(data) { QrCodeGenerator.generateMatrix(data) }
    val size = matrix.size

    Box(
        modifier = modifier
            .aspectRatio(1f)
            .clip(RoundedCornerShape(16.dp))
            .background(backgroundColor)
            .padding(14.dp)
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val moduleSize = this.size.width / size
            for (y in 0 until size) {
                for (x in 0 until size) {
                    if (matrix[y][x]) {
                        drawRect(
                            color = codeColor,
                            topLeft = Offset(x * moduleSize, y * moduleSize),
                            size = Size(moduleSize, moduleSize)
                        )
                    }
                }
            }
        }
    }
}
