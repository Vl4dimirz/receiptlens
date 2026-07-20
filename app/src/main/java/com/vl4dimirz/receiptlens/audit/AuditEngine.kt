package com.vl4dimirz.receiptlens.audit

import com.vl4dimirz.receiptlens.data.Receipt
import kotlin.math.abs

/** ระดับความรุนแรงของสิ่งที่ตรวจพบ (เรียงจากหนัก->เบา) */
enum class Severity { HIGH, MEDIUM, LOW }

/** สิ่งผิดปกติหนึ่งรายการที่ audit เจอ */
data class Finding(
    val severity: Severity,
    val title: String,
    val detail: String
)

/**
 * เครื่องยนต์ตรวจสอบใบเสร็จ — logic ล้วน ไม่มี AI (deterministic)
 *
 * แต่ละ check เป็นฟังก์ชันบริสุทธิ์: รับ Receipt คืน Finding (ถ้าเจอปัญหา)
 * ทำงานบนเครื่อง ไม่ส่งข้อมูลไปไหน = ตรวจสลิป/ใบเสร็จได้แบบ private
 */
object AuditEngine {

    private const val EPS = 0.01          // ค่าคลาดเคลื่อนที่ยอมรับ (เศษสตางค์)
    private const val EXPECTED_VAT = 0.07 // VAT ไทย 7%

    fun audit(receipt: Receipt): List<Finding> {
        val findings = mutableListOf<Finding>()

        // 1) ผลรวมรายการ ต้องเท่ากับ ยอดย่อย
        if (receipt.subtotal != null && receipt.items.isNotEmpty()) {
            val itemsSum = receipt.items.sumOf { it.price }
            if (abs(itemsSum - receipt.subtotal) > EPS) {
                findings += Finding(
                    Severity.HIGH,
                    "รายการรวมไม่ตรงยอดย่อย",
                    "ผลรวมรายการ = ${money(itemsSum)} แต่ยอดย่อยระบุ ${money(receipt.subtotal)} " +
                        "(ต่างกัน ${money(abs(itemsSum - receipt.subtotal))})"
                )
            }
        }

        // 2) ยอดย่อย + ภาษี ต้องเท่ากับ ยอดรวม
        if (receipt.subtotal != null && receipt.total != null) {
            val expected = receipt.subtotal + (receipt.tax ?: 0.0)
            if (abs(expected - receipt.total) > EPS) {
                findings += Finding(
                    Severity.HIGH,
                    "ยอดรวมคำนวณไม่ตรง",
                    "ยอดย่อย + ภาษี = ${money(expected)} แต่ยอดรวมระบุ ${money(receipt.total)}"
                )
            }
        }

        // 3) อัตราภาษีผิดปกติ (คาดหวังราว 7%)
        if (receipt.subtotal != null && receipt.subtotal > 0 && receipt.tax != null) {
            val rate = receipt.tax / receipt.subtotal
            if (abs(rate - EXPECTED_VAT) > 0.01) {
                findings += Finding(
                    Severity.MEDIUM,
                    "อัตราภาษีผิดปกติ",
                    "ภาษีคิดเป็น ${percent(rate)} ของยอดย่อย (ปกติ VAT ไทย = 7%)"
                )
            }
        }

        // 4) อ่านยอดรวมไม่ได้
        if (receipt.total == null) {
            findings += Finding(
                Severity.MEDIUM,
                "ไม่พบยอดรวม",
                "อ่านยอดรวมไม่ได้ — ใบเสร็จอาจไม่ชัด หรือรูปแบบไม่ตรง"
            )
        }

        // 5) รายการซ้ำ (ชื่อ+ราคาเดียวกันปรากฏหลายครั้ง)
        receipt.items
            .groupingBy { it.name to it.price }
            .eachCount()
            .filter { it.value > 1 }
            .forEach { (key, count) ->
                findings += Finding(
                    Severity.LOW,
                    "รายการซ้ำ",
                    "\"${key.first}\" ${money(key.second)} ปรากฏ $count ครั้ง"
                )
            }

        // 6) ราคาติดลบหรือเป็นศูนย์
        receipt.items.filter { it.price <= 0 }.forEach {
            findings += Finding(
                Severity.MEDIUM,
                "ราคาผิดปกติ",
                "\"${it.name}\" ราคา ${money(it.price)}"
            )
        }

        // เรียงหนัก -> เบา (HIGH ก่อน)
        return findings.sortedBy { it.severity.ordinal }
    }

    private fun money(v: Double) = "%,.2f".format(v)
    private fun percent(v: Double) = "%.1f%%".format(v * 100)
}
