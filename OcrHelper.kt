package com.cuentalmt17.gametraduccin.ocr

import android.graphics.Bitmap
import android.graphics.Rect
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions

data class DetectedTextBlock(
    val text: String,
    val bounds: Rect
)

class OcrHelper {

    private val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

    fun recognizeFromBitmap(
        bitmap: Bitmap,
        onResult: (List<DetectedTextBlock>) -> Unit,
        onError: (Exception) -> Unit = {}
    ) {
        val image = InputImage.fromBitmap(bitmap, 0)
        recognizer.process(image)
            .addOnSuccessListener { visionText ->
                val blocks = visionText.textBlocks.mapNotNull { block ->
                    val box = block.boundingBox
                    val txt = block.text.trim()
                    if (box != null && txt.isNotEmpty()) {
                        DetectedTextBlock(text = txt, bounds = box)
                    } else {
                        null
                    }
                }
                onResult(blocks)
            }
            .addOnFailureListener { exception ->
                onError(exception)
            }
    }

    fun close() {
        recognizer.close()
    }
}
