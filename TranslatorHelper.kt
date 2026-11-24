package com.cuentalmt17.gametraduccin.translate

import com.google.mlkit.common.model.DownloadConditions
import com.google.mlkit.nl.translate.TranslateLanguage
import com.google.mlkit.nl.translate.Translation
import com.google.mlkit.nl.translate.TranslatorOptions

class TranslatorHelper(
    private val cacheHelper: TranslationCacheHelper = TranslationCacheHelper()
) {
    private val options = TranslatorOptions.Builder()
        .setSourceLanguage(TranslateLanguage.ENGLISH)
        .setTargetLanguage(TranslateLanguage.SPANISH)
        .build()

    private val translator = Translation.getClient(options)
    private var modelReady = false

    fun prepareModel(onReady: () -> Unit = {}, onError: (Exception) -> Unit = {}) {
        val conditions = DownloadConditions.Builder().build()
        translator.downloadModelIfNeeded(conditions)
            .addOnSuccessListener {
                modelReady = true
                onReady()
            }
            .addOnFailureListener { e ->
                onError(e)
            }
    }

    fun translateText(
        sourceText: String,
        onResult: (String) -> Unit,
        onError: (Exception) -> Unit = {}
    ) {
        val clean = sourceText.trim()
        if (clean.isEmpty()) {
            onResult("")
            return
        }

        // 1. Revisa caché y diccionario rápido primero (0 ms de espera)
        val cachedOrQuick = cacheHelper.getOrTranslate(clean) { "" }
        if (cachedOrQuick.isNotEmpty()) {
            onResult(cachedOrQuick)
            return
        }

        // 2. Si no estaba en caché, traduce y guarda
        val translateAction: () -> Unit = {
            translator.translate(clean)
                .addOnSuccessListener { spanish ->
                    val finalText = spanish.trim()
                    cacheHelper.getOrTranslate(clean) { finalText }
                    onResult(finalText)
                }
                .addOnFailureListener { e ->
                    onError(e)
                }
            Unit
        }

        if (modelReady) {
            translateAction()
        } else {
            prepareModel(onReady = translateAction, onError = onError)
        }
    }

    fun close() {
        translator.close()
    }
}
