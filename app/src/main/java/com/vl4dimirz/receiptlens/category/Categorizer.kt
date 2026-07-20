package com.vl4dimirz.receiptlens.category

/**
 * จัดหมวดสินค้าจากชื่อ
 *
 * ทำเป็น interface เพื่อสลับ "เครื่องยนต์" ได้ในอนาคต:
 * ตอนนี้ = rule-based (คีย์เวิร์ด) ทำงานบนเครื่อง ไม่ต้องใช้ AI/เน็ต ข้อมูลไม่ออกจากมือถือ
 * อนาคต = on-device LLM (Gemini Nano) เมื่อรันบนเครื่องแรงได้ โดยไม่ต้องแก้หน้าจอ
 */
interface Categorizer {
    fun categorize(itemName: String): String
}

class RuleCategorizer : Categorizer {

    private val rules: List<Pair<String, List<String>>> = listOf(
        "อาหารและเครื่องดื่ม" to listOf(
            "coffee", "milk", "tea", "water", "croissant", "cake", "bread",
            "juice", "soda", "snack", "food", "meal", "lunch", "dinner", "rice"
        ),
        "เครื่องเขียน/สำนักงาน" to listOf(
            "notebook", "pen", "pencil", "stapler", "paper", "ink",
            "folder", "marker", "staple", "office", "note"
        ),
        "ของใช้ส่วนตัว" to listOf(
            "soap", "shampoo", "tissue", "towel", "brush", "toothpaste"
        ),
    )

    override fun categorize(itemName: String): String {
        val lower = itemName.lowercase()
        for ((category, keywords) in rules) {
            if (keywords.any { lower.contains(it) }) return category
        }
        return "อื่น ๆ"
    }
}
