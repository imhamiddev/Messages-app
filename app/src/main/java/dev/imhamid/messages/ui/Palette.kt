package dev.imhamid.messages.ui

import androidx.compose.ui.graphics.Color

/**
 * Bubble styles offered in Settings. One colour = solid bubble, two colours = gradient.
 * The app's accent colour is derived from the chosen style, so there is no separate accent setting.
 */
val BubbleStyles: List<List<Color>> = listOf(
    listOf(Color(0xFF5B5FEF)), listOf(Color(0xFF2F7DD1)), listOf(Color(0xFF0EA5A4)), listOf(Color(0xFF3C9A55)),
    listOf(Color(0xFFD9822B)), listOf(Color(0xFFE0457B)), listOf(Color(0xFFD8434B)), listOf(Color(0xFF8B5CF6)),
    listOf(Color(0xFF5B6B86)), listOf(Color(0xFF8D6E63)),
    listOf(Color(0xFF6366F1), Color(0xFF06B6D4)),
    listOf(Color(0xFFF43F5E), Color(0xFFF97316)),
    listOf(Color(0xFF1D4ED8), Color(0xFF0891B2)),
    listOf(Color(0xFF16A34A), Color(0xFF0D9488)),
    listOf(Color(0xFFEC4899), Color(0xFF8B5CF6)),
    listOf(Color(0xFF7C3AED), Color(0xFF2563EB)),
    listOf(Color(0xFFDC2626), Color(0xFFF59E0B)),
    listOf(Color(0xFF1E293B), Color(0xFF475569))
)

/** Number of single-colour styles at the start of [BubbleStyles] (used to migrate the old settings). */
const val SOLID_STYLE_COUNT = 10
