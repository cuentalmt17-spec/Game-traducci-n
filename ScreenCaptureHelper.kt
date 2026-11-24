package com.cuentalmt17.gametraduccin.capture

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.PixelFormat
import android.hardware.display.DisplayManager
import android.media.ImageReader
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.Handler
import android.os.Looper
import android.util.DisplayMetrics
import android.view.WindowManager

class ScreenCaptureHelper(
    private val context: Context,
    resultCode: Int,
    data: Intent
) {
    private val projectionManager =
        context.getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
    private val mediaProjection: MediaProjection? =
        projectionManager.getMediaProjection(resultCode, data)

    private val handler = Handler(Looper.getMainLooper())

    fun captureOnce(onCaptured: (Bitmap?) -> Unit) {
        val projection = mediaProjection
        if (projection == null) {
            onCaptured(null)
            return
        }

        val wm = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        val metrics = DisplayMetrics()
        @Suppress("DEPRECATION")
        wm.defaultDisplay.getRealMetrics(metrics)

        // Escalamos a 0.75x para que el OCR sea más rápido y liviano
        val width = (metrics.widthPixels * 0.75f).toInt().coerceAtLeast(360)
        val height = (metrics.heightPixels * 0.75f).toInt().coerceAtLeast(640)
        val density = metrics.densityDpi

        val imageReader = ImageReader.newInstance(width, height, PixelFormat.RGBA_8888, 2)
        val virtualDisplay = projection.createVirtualDisplay(
            "GameTranslatorCapture",
            width,
            height,
            density,
            DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
            imageReader.surface,
            null,
            handler
        )

        var handled = false
        imageReader.setOnImageAvailableListener({ reader ->
            if (handled) return@setOnImageAvailableListener
            val image = reader.acquireLatestImage() ?: return@setOnImageAvailableListener
            handled = true

            try {
                val plane = image.planes[0]
                val buffer = plane.buffer
                val pixelStride = plane.pixelStride
                val rowStride = plane.rowStride
                val rowPadding = rowStride - pixelStride * width

                val rawBitmap = Bitmap.createBitmap(
                    width + rowPadding / pixelStride,
                    height,
                    Bitmap.Config.ARGB_8888
                )
                rawBitmap.copyPixelsFromBuffer(buffer)

                val cropped = Bitmap.createBitmap(rawBitmap, 0, 0, width, height)
                if (cropped != rawBitmap) {
                    rawBitmap.recycle()
                }

                // Escalamos de vuelta al tamaño real de pantalla para que las coordenadas encajen exacto
                val realBitmap = Bitmap.createScaledBitmap(
                    cropped,
                    metrics.widthPixels,
                    metrics.heightPixels,
                    true
                )
                if (realBitmap != cropped) {
                    cropped.recycle()
                }

                virtualDisplay.release()
                reader.setOnImageAvailableListener(null, null)
                reader.close()

                handler.post { onCaptured(realBitmap) }
            } catch (_: Exception) {
                virtualDisplay.release()
                reader.close()
                handler.post { onCaptured(null) }
            } finally {
                image.close()
            }
        }, handler)
    }

    fun release() {
        mediaProjection?.stop()
    }

    companion object {
        fun createPermissionIntent(context: Context): Intent {
            val mgr = context.getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
            return mgr.createScreenCaptureIntent()
        }

        fun isValidResult(resultCode: Int): Boolean = resultCode == Activity.RESULT_OK
    }
}
