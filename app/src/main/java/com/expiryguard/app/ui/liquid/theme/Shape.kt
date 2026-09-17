package com.expiryguard.app.ui.liquid.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

/**
 * Design tokens — shape.
 *
 * Generous, quiet radii. The reference UI uses 22dp for cards and 17dp for
 * controls; these five Material slots cover every component in the template.
 */
val Shapes = Shapes(
    extraSmall = RoundedCornerShape(10.dp),
    small = RoundedCornerShape(14.dp),
    medium = RoundedCornerShape(18.dp),
    large = RoundedCornerShape(22.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

/** Corner radii that sit outside the Material five-slot scale. */
object AppShape {
    val card = RoundedCornerShape(22.dp)
    val cardLarge = RoundedCornerShape(28.dp)
    val control = RoundedCornerShape(17.dp)
    val pill = RoundedCornerShape(percent = 50)
    val sheetTop = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
}
