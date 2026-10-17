package com.gosms.gateway.ui.scanner

import androidx.camera.core.CameraSelector
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import com.gosms.gateway.R
import com.gosms.gateway.data.model.QrPayload
import com.gosms.gateway.ui.theme.AppTheme
import com.squareup.moshi.Moshi
import java.util.concurrent.Executors

@Composable
fun QrScannerView(
    onQrDetected: (serverUrl: String, pairingCode: String) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val cameraExecutor = remember { Executors.newSingleThreadExecutor() }
    val moshi = remember { Moshi.Builder().build() }
    val adapter = remember { moshi.adapter(QrPayload::class.java) }

    DisposableEffect(Unit) {
        onDispose {
            cameraExecutor.shutdown()
        }
    }

    Box(modifier = modifier.fillMaxSize().background(Color.Black)) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { ctx ->
                val previewView = PreviewView(ctx)
                val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)

                cameraProviderFuture.addListener({
                    val cameraProvider = cameraProviderFuture.get()
                    val preview = Preview.Builder().build().also {
                        it.setSurfaceProvider(previewView.surfaceProvider)
                    }

                    val barcodeScanner = BarcodeScanning.getClient()
                    var hasHandled = false

                    val imageAnalysis = ImageAnalysis.Builder()
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                        .build()

                    imageAnalysis.setAnalyzer(cameraExecutor) { imageProxy ->
                        processImageProxy(barcodeScanner, imageProxy) { rawValue ->
                            if (!hasHandled && rawValue != null) {
                                try {
                                    val payload = adapter.fromJson(rawValue)
                                    if (payload != null && payload.serverUrl.isNotBlank() && payload.pairingCode.isNotBlank()) {
                                        hasHandled = true
                                        previewView.post {
                                            onQrDetected(payload.serverUrl, payload.pairingCode)
                                        }
                                    }
                                } catch (_: Exception) {}
                            }
                        }
                    }

                    try {
                        cameraProvider.unbindAll()
                        cameraProvider.bindToLifecycle(
                            lifecycleOwner,
                            CameraSelector.DEFAULT_BACK_CAMERA,
                            preview,
                            imageAnalysis
                        )
                    } catch (_: Exception) {}
                }, ContextCompat.getMainExecutor(ctx))

                previewView
            }
        )

        Canvas(modifier = Modifier.fillMaxSize()) {
            val canvasWidth = size.width
            val canvasHeight = size.height
            val frameSize = 240.dp.toPx()
            val left = (canvasWidth - frameSize) / 2
            val top = (canvasHeight - frameSize) / 2
            val cornerLength = 32.dp.toPx()
            val strokeWidth = 4.dp.toPx()
            val white = Color.White

            drawLine(white, Offset(left, top), Offset(left + cornerLength, top), strokeWidth, StrokeCap.Round)
            drawLine(white, Offset(left, top), Offset(left, top + cornerLength), strokeWidth, StrokeCap.Round)

            drawLine(white, Offset(left + frameSize, top), Offset(left + frameSize - cornerLength, top), strokeWidth, StrokeCap.Round)
            drawLine(white, Offset(left + frameSize, top), Offset(left + frameSize, top + cornerLength), strokeWidth, StrokeCap.Round)

            drawLine(white, Offset(left, top + frameSize), Offset(left + cornerLength, top + frameSize), strokeWidth, StrokeCap.Round)
            drawLine(white, Offset(left, top + frameSize), Offset(left, top + frameSize - cornerLength), strokeWidth, StrokeCap.Round)

            drawLine(white, Offset(left + frameSize, top + frameSize), Offset(left + frameSize - cornerLength, top + frameSize), strokeWidth, StrokeCap.Round)
            drawLine(white, Offset(left + frameSize, top + frameSize), Offset(left + frameSize, top + frameSize - cornerLength), strokeWidth, StrokeCap.Round)
        }

        IconButton(
            onClick = onBack,
            modifier = Modifier
                .padding(16.dp)
                .align(Alignment.TopStart)
        ) {
            Icon(
                imageVector = Icons.Rounded.ArrowBack,
                contentDescription = stringResource(R.string.back),
                tint = Color.White
            )
        }

        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 60.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = stringResource(R.string.point_camera),
                style = AppTheme.typography.subtitle,
                color = Color.White
            )
        }
    }
}

@androidx.annotation.OptIn(ExperimentalGetImage::class)
private fun processImageProxy(
    scanner: com.google.mlkit.vision.barcode.BarcodeScanner,
    imageProxy: ImageProxy,
    onSuccess: (String?) -> Unit
) {
    val mediaImage = imageProxy.image
    if (mediaImage != null) {
        val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
        scanner.process(image)
            .addOnSuccessListener { barcodes ->
                val firstBarcode = barcodes.firstOrNull { it.format == Barcode.FORMAT_QR_CODE }
                onSuccess(firstBarcode?.rawValue)
            }
            .addOnCompleteListener {
                imageProxy.close()
            }
    } else {
        imageProxy.close()
    }
}
