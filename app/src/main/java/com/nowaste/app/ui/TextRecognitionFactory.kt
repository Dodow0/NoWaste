package com.nowaste.app.ui

import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.TextRecognizer
import com.google.mlkit.vision.text.chinese.ChineseTextRecognizerOptions
import com.google.mlkit.vision.text.latin.TextRecognizerOptions

internal fun createSafeTextRecognizer(): TextRecognizer? =
    runCatching {
        TextRecognition.getClient(ChineseTextRecognizerOptions.Builder().build())
    }.recoverCatching {
        TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
    }.getOrNull()
