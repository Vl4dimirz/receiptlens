package com.vl4dimirz.receiptlens.parser

import com.vl4dimirz.receiptlens.data.LineItem
import com.vl4dimirz.receiptlens.data.Receipt
import com.vl4dimirz.receiptlens.ocr.OcrLine

/**
 * แปลง "บรรทัด OCR ที่กระจัดกระจาย" ให้กลายเป็น Receipt ที่มีโครงสร้าง
 *
 * ทั้งหมดเป็น logic ธรรมดา (ไม่ใช้ AI เลย) = deterministic
 * ทำงานเหมือนกันทุกครั้ง ทดสอบได้ ตรวจสอบได้
 */
object ReceiptParser {

    // ตัวเลขเงิน เช่น 350.00 หรือ 1,250.00
    // ยอมรับทศนิยม 1-2 หลัก เพื่อกัน OCR อ่านหลักสุดท้ายเพี้ยน (เช่น "350.00" -> "350.0e")
    private val priceRegex = Regex("""\d{1,3}(?:,\d{3})*\.\d{1,2}""")

    // วันที่รูปแบบ 2026-07-20
    private val dateRegex = Regex("""\d{4}-\d{2}-\d{2}""")

    fun parse(lines: List<OcrLine>): Receipt {
        if (lines.isEmpty()) return Receipt()

        // ---- ขั้นที่ 1: จับบรรทัดที่อยู่ "แถวเดียวกัน" เข้าด้วยกัน ----
        // ML Kit แยก "Coffee Beans 250g" กับ "350.00" เป็นคนละบล็อก แต่ทั้งคู่มี y เท่ากัน
        // เรารวมมันคืนด้วยการดู centerY ที่ใกล้กัน
        val rows = groupIntoRows(lines)

        // ---- ขั้นที่ 2: แต่ละแถว เรียงซ้าย->ขวา แล้วต่อเป็นข้อความเดียว ----
        val rowTexts = rows.map { row ->
            row.sortedBy { it.left }.joinToString(" ") { it.text.trim() }
        }

        // ---- ขั้นที่ 3: อ่านความหมายของแต่ละแถว (จากบน->ล่าง) ----
        var vendor: String? = null
        var date: String? = null
        var subtotal: Double? = null
        var tax: Double? = null
        var total: Double? = null
        val items = mutableListOf<LineItem>()

        for (raw in rowTexts) {
            val row = raw.trim()
            if (row.isEmpty()) continue

            val upper = row.uppercase()
            val price = lastPrice(row)
            val foundDate = dateRegex.find(row)?.value

            when {
                // วันที่ (เอาอันแรกที่เจอ)
                date == null && foundDate != null -> date = foundDate

                // ยอดย่อย ต้องเช็คก่อน TOTAL เพราะคำว่า SUBTOTAL มี TOTAL อยู่ข้างใน
                upper.contains("SUBTOTAL") -> subtotal = price ?: subtotal

                // รวมทั้งสิ้น
                upper.contains("TOTAL") -> total = price ?: total

                // ภาษี
                upper.contains("VAT") || upper.contains("TAX") -> tax = price ?: tax

                // มีราคา + ไม่ใช่คำสรุปข้างบน = รายการสินค้า
                price != null -> {
                    // ชื่อ = ทุกอย่างก่อนราคาตัวขวาสุด (กันเศษ OCR ที่ติดหลังราคา เช่น "e")
                    val cut = priceRegex.findAll(row).last().range.first
                    val name = row.substring(0, cut).trim().trim('-', ' ')
                    if (name.isNotBlank()) items.add(LineItem(name, price))
                }

                // แถวบนสุดที่เป็นตัวอักษร ไม่มีราคา = ชื่อร้าน (เอาอันแรก)
                vendor == null && row.any { it.isLetter() } -> vendor = row
            }
        }

        return Receipt(vendor, date, items, subtotal, tax, total)
    }

    /** สำหรับ debug: คืนข้อความของแต่ละ "แถว" ที่จับกลุ่มได้ (ดูว่า OCR + grouping ทำงานถูกไหม) */
    fun debugRows(lines: List<OcrLine>): List<String> =
        groupIntoRows(lines).map { row ->
            row.sortedBy { it.left }.joinToString("  |  ") { it.text.trim() }
        }

    /**
     * จับ OcrLine เข้าเป็น "แถว" เดียวกัน โดยดูว่ากล่องข้อความ "ซ้อนกันแนวตั้ง" ไหม
     *
     * วิธีนี้ทนกว่าการวัดระยะ centerY มาก เพราะชื่อสินค้ากับราคาที่อยู่บรรทัดจริงเดียวกัน
     * กล่องมันจะซ้อนกันแนวตั้งเสมอ ไม่ว่าภาพจะเล็กหรือใหญ่ (ไม่ต้องพึ่ง threshold ตายตัว)
     */
    private fun groupIntoRows(lines: List<OcrLine>): List<List<OcrLine>> {
        val sorted = lines.sortedBy { it.top }
        val rows = mutableListOf<MutableList<OcrLine>>()
        val bounds = mutableListOf<IntArray>() // [top, bottom] ของแต่ละแถว

        for (line in sorted) {
            var placed = false
            for (i in rows.indices) {
                val top = bounds[i][0]
                val bottom = bounds[i][1]
                // ส่วนที่ซ้อนกันแนวตั้ง (พิกเซล) เทียบกับความสูงกล่องที่เตี้ยกว่า
                val overlap = minOf(bottom, line.bottom) - maxOf(top, line.top)
                val minHeight = minOf(bottom - top, line.height).coerceAtLeast(1)
                if (overlap > minHeight * 0.4) {
                    rows[i].add(line)
                    bounds[i][0] = minOf(top, line.top)
                    bounds[i][1] = maxOf(bottom, line.bottom)
                    placed = true
                    break
                }
            }
            if (!placed) {
                rows.add(mutableListOf(line))
                bounds.add(intArrayOf(line.top, line.bottom))
            }
        }

        // เรียงแถวจากบนลงล่าง
        return rows.sortedBy { row -> row.minOf { it.top } }
    }

    /** ดึง "ราคาสุดท้าย" ในบรรทัด (ตัวเลขขวาสุด = จำนวนเงิน) */
    private fun lastPrice(s: String): Double? =
        priceRegex.findAll(s).lastOrNull()?.value?.replace(",", "")?.toDoubleOrNull()
}
