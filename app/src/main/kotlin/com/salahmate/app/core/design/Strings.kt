package com.salahmate.app.core.design

/**
 * Bilingual helper. The app ships Bengali and English UI text side by side in code so switching
 * language is instant and needs no configuration change or locale hack, while Arabic religious
 * content always stays Arabic and right-to-left.
 */
fun t(bangla: Boolean, bn: String, en: String): String = if (bangla) bn else en

/**
 * Three-language string picker. Arabic chrome is translated for the navigation and module titles;
 * anything not yet translated falls back to English rather than to an empty or machine-generated
 * string. See docs/TESTING.md for the current Arabic coverage.
 */
fun t3(bangla: Boolean, arabic: Boolean, bn: String, en: String, ar: String): String = when {
    arabic && ar.isNotBlank() -> ar
    bangla -> bn
    else -> en
}
