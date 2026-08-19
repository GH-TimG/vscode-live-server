package com.kaltrack.app.ui.scan

import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Liest Produkt-Barcodes aus dem Kamerabild.
 *
 * Es wird bewusst nur auf die Handels-Formate gefiltert (EAN/UPC) – das
 * verhindert, dass ein QR-Code auf der Verpackung den Scan "gewinnt".
 * Nach dem ersten Treffer schaltet sich der Analyzer über [delivered] ab,
 * damit nicht 30-mal pro Sekunde navigiert wird.
 */
class BarcodeAnalyzer(private val onBarcode: (String) -> Unit) : ImageAnalysis.Analyzer {

    private val scanner = BarcodeScanning.getClient(
        BarcodeScannerOptions.Builder()
            .setBarcodeFormats(
                Barcode.FORMAT_EAN_13,
                Barcode.FORMAT_EAN_8,
                Barcode.FORMAT_UPC_A,
                Barcode.FORMAT_UPC_E,
            )
            .build(),
    )

    private val delivered = AtomicBoolean(false)

    @androidx.annotation.OptIn(markerClass = [ExperimentalGetImage::class])
    override fun analyze(imageProxy: ImageProxy) {
        val mediaImage = imageProxy.image
        if (mediaImage == null || delivered.get()) {
            imageProxy.close()
            return
        }

        val input = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
        scanner.process(input)
            .addOnSuccessListener { barcodes ->
                val code = barcodes.firstNotNullOfOrNull { it.rawValue }?.takeIf { it.isNotBlank() }
                if (code != null && delivered.compareAndSet(false, true)) {
                    // Task-Listener laufen auf dem Main-Thread – Navigation ist hier sicher.
                    onBarcode(code)
                }
            }
            .addOnCompleteListener { imageProxy.close() }
    }
}
