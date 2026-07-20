package com.vl4dimirz.receiptlens.data

/** สินค้าหนึ่งรายการในใบเสร็จ */
data class LineItem(
    val name: String,
    val price: Double
)

/** ใบเสร็จที่แกะออกมาเป็นโครงสร้างแล้ว (ค่าเป็น null = หาไม่เจอ) */
data class Receipt(
    val vendor: String? = null,
    val date: String? = null,
    val items: List<LineItem> = emptyList(),
    val subtotal: Double? = null,
    val tax: Double? = null,
    val total: Double? = null
)
