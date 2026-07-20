package com.vl4dimirz.receiptlens.ui

import android.graphics.Bitmap
import android.graphics.Matrix
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.camera.view.LifecycleCameraController
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner

/**
 * กล้องในแอพ (CameraX) — พรีวิวสด + ปุ่มถ่าย
 * ใช้ LifecycleCameraController (API ระดับสูง จัดการ lifecycle ให้เอง)
 * ภาพที่ถ่ายส่งกลับเป็น Bitmap (หมุนให้ตั้งตรงแล้ว) เข้า pipeline เดียวกับรูปที่เลือก
 */
@Composable
fun CameraCapture(
    onCaptured: (Bitmap) -> Unit,
    onClose: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val controller = remember { LifecycleCameraController(context) }
    val mainExecutor = remember { ContextCompat.getMainExecutor(context) }

    LaunchedEffect(Unit) {
        controller.bindToLifecycle(lifecycleOwner)
    }

    Box(
        Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        AndroidView(
            factory = { ctx ->
                PreviewView(ctx).apply { this.controller = controller }
            },
            modifier = Modifier.fillMaxSize()
        )

        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(24.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            OutlinedButton(onClick = onClose) { Text("ยกเลิก") }
            Button(onClick = {
                controller.takePicture(
                    mainExecutor,
                    object : ImageCapture.OnImageCapturedCallback() {
                        override fun onCaptureSuccess(image: ImageProxy) {
                            val rotation = image.imageInfo.rotationDegrees
                            val bmp = image.toBitmap()
                            image.close()
                            onCaptured(rotateBitmap(bmp, rotation))
                        }

                        override fun onError(exc: ImageCaptureException) {
                            onClose()
                        }
                    }
                )
            }) { Text("ถ่าย") }
        }
    }
}

private fun rotateBitmap(bmp: Bitmap, degrees: Int): Bitmap {
    if (degrees == 0) return bmp
    val matrix = Matrix().apply { postRotate(degrees.toFloat()) }
    return Bitmap.createBitmap(bmp, 0, 0, bmp.width, bmp.height, matrix, true)
}
