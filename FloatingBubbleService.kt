package com.cuentalmt17.gametraduccin.overlay

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.IBinder
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.TextView
import com.cuentalmt17.gametraduccin.capture.ScreenCaptureHelper
import com.cuentalmt17.gametraduccin.ocr.OcrHelper
import com.cuentalmt17.gametraduccin.translate.TranslatorHelper
import java.util.concurrent.atomic.AtomicInteger
import kotlin.math.abs

class FloatingBubbleService : Service() {

    companion object {
        const val EXTRA_RESULT_CODE = "extra_result_code"
        const val EXTRA_DATA_INTENT = "extra_data_intent"
        private const val CHANNEL_ID = "game_translator_channel"
        private const val NOTIFICATION_ID = 101
    }

    private lateinit var windowManager: WindowManager
    private var bubbleView: TextView? = null
    private var overlayView: TranslationOverlayView? = null

    private var captureHelper: ScreenCaptureHelper? = null
    private val ocrHelper = OcrHelper()
    private val translatorHelper = TranslatorHelper()

    private var isTranslatedVisible = false
    private var isBusy = false

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        startForegroundServiceNotification()
        showOverlayLayer()
        showFloatingBubble()
        translatorHelper.prepareModel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val resultCode = intent?.getIntExtra(EXTRA_RESULT_CODE, 0) ?: 0
        @Suppress("DEPRECATION")
        val dataIntent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent?.getParcelableExtra(EXTRA_DATA_INTENT, Intent::class.java)
        } else {
            intent?.getParcelableExtra(EXTRA_DATA_INTENT)
        }

        if (ScreenCaptureHelper.isValidResult(resultCode) && dataIntent != null) {
            captureHelper?.release()
            captureHelper = ScreenCaptureHelper(this, resultCode, dataIntent)
        }
        return START_STICKY
    }

    private fun startForegroundServiceNotification() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Traductor en pantalla",
                NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }

        val builder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Notification.Builder(this, CHANNEL_ID)
        } else {
            @Suppress("DEPRECATION")
            Notification.Builder(this)
        }

        val notification = builder
            .setContentTitle("Game Traductor activo")
            .setContentText("Tocá la burbuja para traducir la pantalla")
            .setSmallIcon(android.R.drawable.ic_menu_search)
            .build()

        startForeground(NOTIFICATION_ID, notification)
    }

    private fun overlayWindowType(): Int {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }
    }

    private fun showOverlayLayer() {
        val view = TranslationOverlayView(this)
        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            overlayWindowType(),
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        )
        windowManager.addView(view, params)
        overlayView = view
    }

    private fun showFloatingBubble() {
        val bubble = TextView(this).apply {
            text = "ES"
            setTextColor(Color.WHITE)
            textSize = 15f
            gravity = Gravity.CENTER
            background = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(Color.argb(230, 30, 136, 229))
                setStroke(3, Color.WHITE)
            }
        }

        val sizePx = (52 * resources.displayMetrics.density).toInt()
        val params = WindowManager.LayoutParams(
            sizePx,
            sizePx,
            overlayWindowType(),
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = 20
            y = 400
        }

        var initialX = 0
        var initialY = 0
        var touchStartX = 0f
        var touchStartY = 0f

        bubble.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    initialX = params.x
                    initialY = params.y
                    touchStartX = event.rawX
                    touchStartY = event.rawY
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    params.x = initialX + (event.rawX - touchStartX).toInt()
                    params.y = initialY + (event.rawY - touchStartY).toInt()
                    windowManager.updateViewLayout(bubble, params)
                    true
                }
                MotionEvent.ACTION_UP -> {
                    val movedX = abs(event.rawX - touchStartX)
                    val movedY = abs(event.rawY - touchStartY)
                    if (movedX < 12 && movedY < 12) {
                        onBubbleClicked()
                    }
                    true
                }
                else -> false
            }
        }

        windowManager.addView(bubble, params)
        bubbleView = bubble
    }

    private fun onBubbleClicked() {
        if (isBusy) return

        // Si ya hay traducción visible, tocar la burbuja la limpia para seguir jugando limpio
        if (isTranslatedVisible) {
            overlayView?.clearTranslations()
            isTranslatedVisible = false
            bubbleView?.text = "ES"
            return
        }

        val capturer = captureHelper ?: return
        isBusy = true
        bubbleView?.text = "..."

        // Ocultamos brevemente la capa para capturar el juego limpio
        overlayView?.visibility = View.INVISIBLE

        capturer.captureOnce { bitmap ->
            overlayView?.visibility = View.VISIBLE

            if (bitmap == null) {
                isBusy = false
                bubbleView?.text = "ES"
                return@captureOnce
            }

            ocrHelper.recognizeFromBitmap(
                bitmap = bitmap,
                onResult = { blocks ->
                    bitmap.recycle()
                    if (blocks.isEmpty()) {
                        isBusy = false
                        bubbleView?.text = "ES"
                        return@recognizeFromBitmap
                    }

                    val results = arrayOfNulls<TranslatedBox>(blocks.size)
                    val remaining = AtomicInteger(blocks.size)

                    blocks.forEachIndexed { index, block ->
                        translatorHelper.translateText(
                            sourceText = block.text,
                            onResult = { translated ->
                                results[index] = TranslatedBox(
                                    bounds = block.bounds,
                                    translatedText = if (translated.isNotEmpty()) translated else block.text
                                )
                                if (remaining.decrementAndGet() == 0) {
                                    overlayView?.updateTranslations(results.filterNotNull())
                                    isTranslatedVisible = true
                                    isBusy = false
                                    bubbleView?.text = "X"
                                }
                            },
                            onError = {
                                results[index] = TranslatedBox(
                                    bounds = block.bounds,
                                    translatedText = block.text
                                )
                                if (remaining.decrementAndGet() == 0) {
                                    overlayView?.updateTranslations(results.filterNotNull())
                                    isTranslatedVisible = true
                                    isBusy = false
                                    bubbleView?.text = "X"
                                }
                            }
                        )
                    }
                },
                onError = {
                    bitmap.recycle()
                    isBusy = false
                    bubbleView?.text = "ES"
                }
            )
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        captureHelper?.release()
        ocrHelper.close()
        translatorHelper.close()
        bubbleView?.let { windowManager.removeView(it) }
        overlayView?.let { windowManager.removeView(it) }
    }
}
