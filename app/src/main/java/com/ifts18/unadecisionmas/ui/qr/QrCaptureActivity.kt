package com.ifts18.unadecisionmas.ui.qr

import android.view.ViewGroup
import com.ifts18.unadecisionmas.R
import com.journeyapps.barcodescanner.CameraPreview
import com.journeyapps.barcodescanner.CaptureActivity
import com.journeyapps.barcodescanner.DecoratedBarcodeView

/**
 * Actividad personalizada para el escáner QR que ubica el HUD de instrucción
 * por encima del recuadro de escaneo.
 */
class QrCaptureActivity : CaptureActivity() {

    override fun initializeContent(): DecoratedBarcodeView {
        setContentView(R.layout.activity_qr_capture)
        val scannerView = findViewById<DecoratedBarcodeView>(R.id.zxing_barcode_scanner)
        val statusView = scannerView.statusView

        scannerView.barcodeView.addStateListener(object : CameraPreview.StateListener {
            override fun previewSized() {
                val framingRect = scannerView.barcodeView.framingRect ?: return
                statusView?.post {
                    val params = statusView.layoutParams as? ViewGroup.MarginLayoutParams ?: return@post
                    val density = resources.displayMetrics.density
                    val gapPx = (16f * density).toInt()
                    val statusBarResId = resources.getIdentifier("status_bar_height", "dimen", "android")
                    val statusBarPx = if (statusBarResId > 0) resources.getDimensionPixelSize(statusBarResId) else 0
                    val minTopMargin = statusBarPx + (16f * density).toInt()
                    val desiredTop = framingRect.top - statusView.height - gapPx
                    params.topMargin = desiredTop.coerceAtLeast(minTopMargin)
                    statusView.layoutParams = params
                }
            }

            override fun previewStarted() = Unit
            override fun previewStopped() = Unit
            override fun cameraError(error: Exception?) = Unit
            override fun cameraClosed() = Unit
        })

        return scannerView
    }
}
