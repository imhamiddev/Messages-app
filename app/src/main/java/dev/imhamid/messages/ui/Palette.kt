package dev.imhamid.messages.ui

import androidx.compose.ui.graphics.Color

/** Accent colours offered in Settings. */
val AccentColors = listOf(
    Color(0xFF5B5FEF), Color(0xFF2F7DD1), Color(0xFF0EA5A4), Color(0xFF3C9A55), Color(0xFFD9822B),
    Color(0xFFE0457B), Color(0xFFD8434B), Color(0xFF8B5CF6), Color(0xFF5B6B86), Color(0xFF8D6E63)
)

/** Gradients for outgoing bubbles, buttons and the FAB. Index 0 = plain accent colour. */
val BubbleGradients: List<List<Color>?> = listOf(
    null,
    listOf(Color(0xFF6366F1), Color(0xFF06B6D4)),
    listOf(Color(0xFFF43F5E), Color(0xFFF97316)),
    listOf(Color(0xFF1D4ED8), Color(0xFF0891B2)),
    listOf(Color(0xFF16A34A), Color(0xFF0D9488)),
    listOf(Color(0xFFEC4899), Color(0xFF8B5CF6)),
    listOf(Color(0xFF7C3AED), Color(0xFF2563EB)),
    listOf(Color(0xFFDC2626), Color(0xFFF59E0B)),
    listOf(Color(0xFF1E293B), Color(0xFF475569))
)

/** Soft chat backgrounds. Index 0 = none. */
val Wallpapers: List<List<Color>?> = listOf(
    null,
    listOf(Color(0xFFFF9A9E), Color(0xFFFAD0C4)),
    listOf(Color(0xFF89F7FE), Color(0xFF66A6FF)),
    listOf(Color(0xFF84FAB0), Color(0xFF8FD3F4)),
    listOf(Color(0xFFA18CD1), Color(0xFFFBC2EB)),
    listOf(Color(0xFF30CFD0), Color(0xFF330867)),
    listOf(Color(0xFFF6D365), Color(0xFFFDA085))
)
