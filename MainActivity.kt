package com.cuentalmt17.gametraduccin

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import com.cuentalmt17.gametraduccin.overlay.FloatingBubbleService

class MainActivity : Activity() {

    companion object {
        private const val REQUEST_OVERLAY_PERMISSION = 1001
    }

    private lateinit var statusText: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val btnStart = Button(this).apply {
            text = "Activar botón flotante"
            setOnClickListener { checkOverlayAndStart() }
        }

        val btnStop = Button(this).apply {
            text = "Detener botón flotante"
            setOnClickListener {
                stopService(Intent(this@MainActivity, FloatingBubbleService::class.java))
                statusText.text = "Estado: detenido"
            }
        }

        statusText = TextView(this).apply {
            text = "Estado: listo para iniciar"
            setPadding(0, 24, 0, 0)
        }

        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(48, 80, 48, 48)
            addView(btnStart)
            addView(btnStop)
            addView(statusText)
        }

        setContentView(layout)
    }

    private fun checkOverlayAndStart() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(this)) {
            statusText.text = "Otorgá el permiso para mostrar sobre otras apps..."
            val intent = Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:$packageName")
            )
            startActivityForResult(intent, REQUEST_OVERLAY_PERMISSION)
        } else {
            startBubbleService()
        }
    }

    private fun startBubbleService() {
        val intent = Intent(this, FloatingBubbleService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent)
        } else {
            startService(intent)
        }
        statusText.text = "Estado: burbuja activa en pantalla"
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_OVERLAY_PERMISSION) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && Settings.canDrawOverlays(this)) {
                startBubbleService()
            } else {
                statusText.text = "Permiso denegado: no se puede mostrar encima del juego"
            }
        }
    }
}
