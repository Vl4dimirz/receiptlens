package com.vl4dimirz.receiptlens.ui

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.vl4dimirz.receiptlens.R
import com.vl4dimirz.receiptlens.audit.AuditEngine
import com.vl4dimirz.receiptlens.audit.Finding
import com.vl4dimirz.receiptlens.audit.Severity
import com.vl4dimirz.receiptlens.category.Categorizer
import com.vl4dimirz.receiptlens.category.RuleCategorizer
import com.vl4dimirz.receiptlens.data.LineItem
import com.vl4dimirz.receiptlens.data.Receipt
import com.vl4dimirz.receiptlens.ocr.MlKitTextReader
import com.vl4dimirz.receiptlens.ocr.ReceiptTextReader
import com.vl4dimirz.receiptlens.parser.ReceiptParser
import com.vl4dimirz.receiptlens.ui.theme.Danger
import com.vl4dimirz.receiptlens.ui.theme.Mist
import com.vl4dimirz.receiptlens.ui.theme.Ok
import com.vl4dimirz.receiptlens.ui.theme.Warn
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun ScanScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val reader = remember { MlKitTextReader() }
    val categorizer = remember { RuleCategorizer() }

    var receipt by remember { mutableStateOf<Receipt?>(null) }
    var findings by remember { mutableStateOf<List<Finding>>(emptyList()) }
    var isReading by remember { mutableStateOf(false) }
    var errorMsg by remember { mutableStateOf<String?>(null) }
    var showCamera by remember { mutableStateOf(false) }

    var displayBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var displayRes by remember { mutableStateOf<Int?>(R.drawable.sample_receipt) }

    fun runOn(bmp: Bitmap) {
        scope.launch {
            isReading = true; errorMsg = null; receipt = null; findings = emptyList()
            try {
                val (r, f) = analyze(reader, bmp)
                receipt = r; findings = f
            } catch (e: Exception) {
                errorMsg = e.message
            } finally {
                isReading = false
            }
        }
    }

    fun scanSample(res: Int) {
        displayBitmap = null
        displayRes = res
        scope.launch {
            isReading = true; errorMsg = null; receipt = null; findings = emptyList()
            try {
                val bmp = withContext(Dispatchers.IO) {
                    BitmapFactory.decodeResource(context.resources, res)
                }
                val (r, f) = analyze(reader, bmp)
                receipt = r; findings = f
            } catch (e: Exception) {
                errorMsg = e.message
            } finally {
                isReading = false
            }
        }
    }

    fun scanUri(uri: Uri) {
        displayRes = null
        scope.launch {
            isReading = true; errorMsg = null; receipt = null; findings = emptyList()
            try {
                val bmp = withContext(Dispatchers.IO) { loadBitmap(context, uri) }
                    ?: error("โหลดรูปไม่สำเร็จ")
                displayBitmap = bmp
                val (r, f) = analyze(reader, bmp)
                receipt = r; findings = f
            } catch (e: Exception) {
                errorMsg = e.message
            } finally {
                isReading = false
            }
        }
    }

    // ภาพจากกล้อง
    fun onCameraCaptured(bmp: Bitmap) {
        displayRes = null
        displayBitmap = bmp
        runOn(bmp)
    }

    val picker = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) scanUri(uri)
    }

    val cameraPermLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) showCamera = true else errorMsg = "ต้องอนุญาตให้ใช้กล้องก่อน"
    }

    fun openCamera() {
        val granted = ContextCompat.checkSelfPermission(
            context, Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED
        if (granted) showCamera = true else cameraPermLauncher.launch(Manifest.permission.CAMERA)
    }

    if (showCamera) {
        CameraCapture(
            onCaptured = { bmp -> showCamera = false; onCameraCaptured(bmp) },
            onClose = { showCamera = false }
        )
        return
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("ReceiptLens", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.SemiBold)
        Text(
            text = "สแกน · ตรวจสอบ · บนเครื่อง",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(16.dp))

        val bmp = displayBitmap
        val res = displayRes
        val imgModifier = Modifier
            .fillMaxWidth()
            .height(240.dp)
        when {
            bmp != null -> Image(
                bitmap = bmp.asImageBitmap(),
                contentDescription = "ใบเสร็จ",
                modifier = imgModifier,
                contentScale = ContentScale.Fit
            )
            res != null -> Image(
                painter = painterResource(res),
                contentDescription = "ใบเสร็จ",
                modifier = imgModifier,
                contentScale = ContentScale.Fit
            )
        }
        Spacer(Modifier.height(16.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(onClick = { openCamera() }, enabled = !isReading, modifier = Modifier.weight(1f)) {
                Text("ถ่ายด้วยกล้อง")
            }
            Button(
                onClick = { picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
                enabled = !isReading,
                modifier = Modifier.weight(1f)
            ) {
                Text("เลือกจากเครื่อง")
            }
        }
        Spacer(Modifier.height(8.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = { scanSample(R.drawable.sample_receipt) }, enabled = !isReading) {
                Text("ตัวอย่างปกติ")
            }
            OutlinedButton(onClick = { scanSample(R.drawable.sample_receipt_bad) }, enabled = !isReading) {
                Text("ตัวอย่างน่าสงสัย")
            }
        }

        if (isReading) {
            Spacer(Modifier.height(8.dp))
            Text("กำลังสแกน...", style = MaterialTheme.typography.bodyMedium)
        }
        Spacer(Modifier.height(20.dp))

        errorMsg?.let {
            Text("ผิดพลาด: $it", color = MaterialTheme.colorScheme.error)
        }

        receipt?.let {
            if (it.items.isEmpty() && it.total == null && it.subtotal == null) {
                OutlinedCard(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "ไม่พบใบเสร็จในภาพ — ลองถ่ายหรือเลือกรูปใหม่ให้ชัดขึ้น",
                        modifier = Modifier.padding(16.dp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                ReceiptCard(it)
                Spacer(Modifier.height(12.dp))
                CategorySummary(it.items, categorizer)
                Spacer(Modifier.height(12.dp))
                FindingsSection(findings)
            }
        }
    }
}

/** OCR -> parser -> audit (ทำงานเบื้องหลัง) */
private suspend fun analyze(reader: ReceiptTextReader, bmp: Bitmap): Pair<Receipt, List<Finding>> {
    val lines = reader.readLines(bmp)
    val parsed = ReceiptParser.parse(lines)
    return parsed to AuditEngine.audit(parsed)
}

/** โหลด Bitmap จาก Uri (บังคับเป็น software bitmap เพื่อให้ ML Kit อ่านได้) */
private fun loadBitmap(context: Context, uri: Uri): Bitmap? {
    val resolver = context.contentResolver
    return try {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            val source = ImageDecoder.createSource(resolver, uri)
            ImageDecoder.decodeBitmap(source) { decoder, _, _ ->
                decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
            }
        } else {
            resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it) }
        }
    } catch (e: Exception) {
        null
    }
}

@Composable
private fun ReceiptCard(receipt: Receipt) {
    OutlinedCard(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Text(
                text = receipt.vendor ?: "(ไม่พบชื่อร้าน)",
                style = MaterialTheme.typography.titleLarge
            )
            receipt.date?.let {
                Text(
                    text = "วันที่: $it",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            HorizontalDivider(Modifier.padding(vertical = 10.dp))

            receipt.items.forEach { item ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(item.name, style = MaterialTheme.typography.bodyLarge)
                    Text(money(item.price), style = MaterialTheme.typography.bodyLarge)
                }
            }
            if (receipt.items.isEmpty()) {
                Text("(ไม่พบรายการสินค้า)", style = MaterialTheme.typography.bodyMedium)
            }

            HorizontalDivider(Modifier.padding(vertical = 10.dp))

            AmountRow("ยอดย่อย", receipt.subtotal)
            AmountRow("ภาษี (VAT)", receipt.tax)
            AmountRow("รวมทั้งสิ้น", receipt.total, bold = true)
        }
    }
}

@Composable
private fun AmountRow(label: String, value: Double?, bold: Boolean = false) {
    if (value == null) return
    val weight = if (bold) FontWeight.Bold else FontWeight.Normal
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, fontWeight = weight)
        Text(money(value), fontWeight = weight)
    }
}

@Composable
private fun CategorySummary(items: List<LineItem>, categorizer: Categorizer) {
    if (items.isEmpty()) return
    val byCategory = items
        .groupBy { categorizer.categorize(it.name) }
        .mapValues { entry -> entry.value.sumOf { it.price } }
        .toList()
        .sortedByDescending { it.second }

    OutlinedCard(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Text(
                text = "สรุปตามหมวด",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(Modifier.height(8.dp))
            byCategory.forEach { (category, total) ->
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(category, style = MaterialTheme.typography.bodyLarge)
                    Text(money(total), style = MaterialTheme.typography.bodyLarge)
                }
            }
        }
    }
}

@Composable
private fun FindingsSection(findings: List<Finding>) {
    if (findings.isEmpty()) {
        OutlinedCard(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = "✓ ตรวจแล้ว ไม่พบความผิดปกติ",
                color = Ok,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(16.dp)
            )
        }
        return
    }
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = "พบ ${findings.size} จุดที่ต้องตรวจสอบ",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold
        )
        Spacer(Modifier.height(8.dp))
        findings.forEach { finding ->
            FindingRow(finding)
            Spacer(Modifier.height(8.dp))
        }
    }
}

@Composable
private fun FindingRow(finding: Finding) {
    OutlinedCard(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("●", color = severityColor(finding.severity))
                Spacer(Modifier.width(8.dp))
                Text(finding.title, fontWeight = FontWeight.SemiBold)
            }
            Spacer(Modifier.height(4.dp))
            Text(
                text = finding.detail,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

private fun severityColor(severity: Severity): Color = when (severity) {
    Severity.HIGH -> Danger
    Severity.MEDIUM -> Warn
    Severity.LOW -> Mist
}

private fun money(v: Double): String = "%,.2f".format(v)
