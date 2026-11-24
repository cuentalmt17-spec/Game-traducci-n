package com.cuentalmt17.gametraduccin.overlay

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.view.View

data class TranslatedBox(
    val bounds: Rect,
    val translatedText: String
)

class TranslationOverlayView(context: Context) : View(context) {

    private val items = mutableListOf<TranslatedBox>()

    private val boxPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.argb(215, 18, 20, 26)
        style = Paint.Style.FILL
    }

    private val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.argb(120, 255, 255, 255)
        style = Paint.Style.STROKE
        strokeWidth = 2f
    }

    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        textAlign = Paint.Align.CENTER
    }

    fun updateTranslations(newItems: List<TranslatedBox>) {
        items.clear()
        items.addAll(newItems)
        invalidate()
    }

    fun clearTranslations() {
        items.clear()
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        for (item in items) {
            val rectF = RectF(item.bounds)
            val radius = 12f

            canvas.drawRoundRect(rectF, radius, radius, boxPaint)
            canvas.drawRoundRect(rectF, radius, radius, borderPaint)

            // Ajusta el tamaño de letra según el alto del bloque detectado
            val targetSize = (item.bounds.height() * 0.55f).coerceIn(22f, 54f)
            textPaint.textSize = targetSize

            val centerX = rectF.centerX()
            val centerY = rectF.centerY() - ((textPaint.descent() + textPaint.ascent()) / 2f)

            canvas.drawText(item.translatedText, centerX, centerY, textPaint)
        }
    }
}
