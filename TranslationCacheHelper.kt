package com.cuentalmt17.gametraduccin.translate

class TranslationCacheHelper {

    // Diccionario rápido en memoria: "texto en inglés" -> "texto en español"
    private val cache = HashMap<String, String>(256)

    // Diccionario base de términos comunes de juegos para respuesta instantánea
    private val quickGameDictionary = mapOf(
        "play" to "Jugar",
        "start" to "Iniciar",
        "new game" to "Nueva partida",
        "continue" to "Continuar",
        "settings" to "Ajustes",
        "options" to "Opciones",
        "inventory" to "Inventario",
        "map" to "Mapa",
        "quest" to "Misión",
        "quests" to "Misiones",
        "shop" to "Tienda",
        "exit" to "Salir",
        "back" to "Volver",
        "save" to "Guardar",
        "load" to "Cargar",
        "attack" to "Atacar",
        "defend" to "Defender",
        "skip" to "Saltar",
        "next" to "Siguiente",
        "confirm" to "Confirmar",
        "cancel" to "Cancelar"
    )

    fun getOrTranslate(sourceText: String, translator: (String) -> String): String {
        val clean = sourceText.trim()
        if (clean.isEmpty()) return ""

        val key = clean.lowercase()
        cache[key]?.let { return it }
        quickGameDictionary[key]?.let {
            cache[key] = it
            return it
        }

        val translated = translator(clean).trim()
        cache[key] = translated
        return translated
    }

    fun clearCache() {
        cache.clear()
    }
}
