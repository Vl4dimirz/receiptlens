package com.vl4dimirz.receiptlens.ocr

import android.graphics.Bitmap
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * ข้อความหนึ่งบรรทัดจาก OCR พร้อม "ตำแหน่ง" บนภาพ
 * เราเก็บตำแหน่งไว้เพราะ ML Kit แยกชื่อสินค้ากับราคาเป็นคนละบล็อก
 * เราจะใช้ค่า y (บน-ล่าง) มาจับว่าอันไหนอยู่ "แถวเดียวกัน" ทีหลัง
 */
data class OcrLine(
    val text: String,
    val left: Int,
    val top: Int,
    val bottom: Int
) {
    val centerY: Int get() = (top + bottom) / 2
    val height: Int get() = bottom - top
}

/**
 * อ่านข้อความจากรูปภาพ (OCR)
 * ทำเป็น interface เพื่อสลับ "เครื่องยนต์" ได้ในอนาคตโดยไม่ต้องแก้หน้าจอ/parser
 */
interface ReceiptTextReader {
    suspend fun readLines(bitmap: Bitmap): List<OcrLine>
}

/**
 * ตัวจริง: Google ML Kit Text Recognition — ทำงานบนเครื่อง (on-device) ออฟไลน์
 */
class MlKitTextReader : ReceiptTextReader {

    private val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

    override suspend fun readLines(bitmap: Bitmap): List<OcrLine> =
        suspendCancellableCoroutine { cont ->
            val image = InputImage.fromBitmap(bitmap, 0)
            recognizer.process(image)
                .addOnSuccessListener { visionText ->
                    // ML Kit จัดผลเป็นชั้น: Text -> TextBlock -> Line -> Element
                    // เราดึงระดับ "Line" พร้อมกล่องกรอบ (boundingBox) มาใช้
                    val lines = buildList {
                        for (block in visionText.textBlocks) {
                            for (line in block.lines) {
                                val box = line.boundingBox ?: continue
                                add(OcrLine(line.text, box.left, box.top, box.bottom))
                            }
                        }
                    }
                    cont.resume(lines)
                }
                .addOnFailureListener { e -> cont.resumeWithException(e) }
        }
}
