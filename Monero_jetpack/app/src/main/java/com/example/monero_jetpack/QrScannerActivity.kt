package com.example.monero_jetpack

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.google.zxing.BarcodeFormat
import com.journeyapps.barcodescanner.DecoratedBarcodeView
import com.journeyapps.barcodescanner.DefaultDecoderFactory

class QrScannerActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
            != PackageManager.PERMISSION_GRANTED) {
            finish()
            return
        }

        setContent {
            Surface {
                EmbeddedScannerContent { scannedText ->
                    setResult(RESULT_OK, Intent().apply {
                        putExtra("SCAN_RESULT", scannedText)
                    })
                    finish()
                }
            }
        }
    }
}

@Composable
fun EmbeddedScannerContent(onScanResult: (String) -> Unit) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var barcodeView by remember { mutableStateOf<DecoratedBarcodeView?>(null) }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> barcodeView?.resume()
                Lifecycle.Event.ON_PAUSE -> barcodeView?.pause()
                else -> Unit
            }
        }

        lifecycleOwner.lifecycle.addObserver(observer)

        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        AndroidView(
            factory = { ctx ->
                DecoratedBarcodeView(ctx).apply {
                    barcodeView = this
                    val formats = listOf(BarcodeFormat.QR_CODE)
                    this.decoderFactory = DefaultDecoderFactory(formats)
                    decodeContinuous { result ->
                        result.text?.let { scannedText ->
                            onScanResult(scannedText)
                        }
                    }
                }
            },
            modifier = Modifier.fillMaxSize()
        )

        IconButton(
            onClick = { (context as? ComponentActivity)?.finish() },
            modifier = Modifier.padding(16.dp)
        ) {
            Icon(Icons.Default.Close, "Close")
        }
    }
}