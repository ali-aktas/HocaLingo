package com.hocalingo.app.feature.selection

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hocalingo.app.R
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.absoluteValue
import kotlin.math.roundToInt

private val PoppinsFontFamily = FontFamily(
    Font(R.font.poppins_regular, FontWeight.Normal),
    Font(R.font.poppins_medium, FontWeight.Medium),
    Font(R.font.poppins_bold, FontWeight.Bold),
    Font(R.font.poppins_black, FontWeight.Black)
)


private val cardColors = listOf(
    Color(0xFF6366F1), Color(0xFF8B5CF6), Color(0xFFEC4899), Color(0xFFEF4444),
    Color(0xFFF97316), Color(0xFF10B981), Color(0xFF06B6D4), Color(0xFF3B82F6),
    Color(0xFF8B5A2B), Color(0xFF059669), Color(0xFF7C3AED), Color(0xFFDC2626),
    Color(0xFF0891B2), Color(0xFF065F46), Color(0xFF7C2D12), Color(0xFF1E40AF),
    Color(0xFF7E22CE), Color(0xFF0F766E), Color(0xFFA21CAF), Color(0xFF9A3412)
)

/**
 * SwipeableCard.kt
 *
 * Package: app/src/main/java/com/hocalingo/app/feature/selection/
 *
 * ✅ SORUNLAR ÇÖZÜLdü:
 * - Her kart için state reset ediliyor
 * - Direction yazıları düzgün kaybolup gösteriliyor
 * - Tüm kartların yazıları görünüyor
 * - Kaydırma her yönde düzgün çalışıyor
 */
@Composable
fun SwipeableCard(
    word: String,
    translation: String,
    example: String? = null,
    onSwipeLeft: () -> Unit,
    onSwipeRight: () -> Unit,
    modifier: Modifier = Modifier,
    nextWord: String? = null,
    nextTranslation: String? = null
) {
    // ✅ Random color based on word
    val cardColor = remember(word, translation) {
        cardColors[(word + translation).hashCode().absoluteValue % cardColors.size]
    }

    val configuration = LocalConfiguration.current
    val screenWidth = configuration.screenWidthDp.dp
    val density = LocalDensity.current

    val swipeThreshold = with(density) { (screenWidth.toPx() * 0.15f) }

    // ✅ FIX: Her kart için benzersiz key
    val cardKey = remember(word, translation) { "$word-$translation-${System.currentTimeMillis()}" }

    // ✅ FIX: State'ler her kart için sıfırlanıyor
    val offsetX = remember(cardKey) { Animatable(0f) }
    val offsetY = remember(cardKey) { Animatable(0f) }
    val rotation = remember(cardKey) { Animatable(0f) }

    val scope = rememberCoroutineScope()

    // ✅ FIX: Her kart için sıfırlanan flag
    var isSwipeTriggered by remember(cardKey) { mutableStateOf(false) }
    var isDragging by remember(cardKey) { mutableStateOf(false) }

    // ✅ FIX: Alpha değerleri basitleştirildi - derivedStateOf kaldırıldı
    val leftIndicatorAlpha: Float = if (offsetX.value < 0 && !isSwipeTriggered) {
        (abs(offsetX.value) / swipeThreshold).coerceIn(0f, 1f)
    } else 0f

    val rightIndicatorAlpha: Float = if (offsetX.value > 0 && !isSwipeTriggered) {
        (offsetX.value / swipeThreshold).coerceIn(0f, 1f)
    } else 0f

    val cardScale: Float = 1f - (abs(offsetX.value) / with(density) { screenWidth.toPx() } * 0.05f).coerceIn(0f, 0.1f)

    // ✅ FIX: Kart değişince tüm state'ler sıfırlanıyor
    LaunchedEffect(cardKey) {
        offsetX.snapTo(0f)
        offsetY.snapTo(0f)
        rotation.snapTo(0f)
        isSwipeTriggered = false
        isDragging = false
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(420.dp)
    ) {

        // Main card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight()
                .padding(horizontal = 24.dp, vertical = 16.dp)
                .offset {
                    IntOffset(
                        offsetX.value.roundToInt(),
                        offsetY.value.roundToInt()
                    )
                }
                .graphicsLayer {
                    rotationZ = rotation.value
                    scaleX = cardScale
                    scaleY = cardScale
                }
                .shadow(
                    elevation = 16.dp,
                    shape = RoundedCornerShape(24.dp)
                )
                .pointerInput(cardKey) {
                    detectDragGestures(
                        onDragStart = {
                            isDragging = true
                        },
                        onDragEnd = {
                            isDragging = false

                            // ✅ FIX: Threshold kontrolü basitleştirildi
                            if (!isSwipeTriggered) {
                                if (abs(offsetX.value) >= swipeThreshold) {
                                    // Swipe completed
                                    isSwipeTriggered = true

                                    scope.launch {
                                        // Animate off screen
                                        val targetX = if (offsetX.value > 0) {
                                            with(density) { screenWidth.toPx() * 1.5f }
                                        } else {
                                            with(density) { -screenWidth.toPx() * 1.5f }
                                        }

                                        launch {
                                            offsetX.animateTo(
                                                targetValue = targetX,
                                                animationSpec = tween(800)
                                            )
                                        }

                                        launch {
                                            val targetRotation = if (offsetX.value > 0) 25f else -25f
                                            rotation.animateTo(
                                                targetValue = targetRotation,
                                                animationSpec = tween(800)
                                            )
                                        }

                                        // ✅ FIX: Callback doğru yönde çağrılıyor
                                        if (offsetX.value > 0) {
                                            onSwipeRight()
                                        } else {
                                            onSwipeLeft()
                                        }
                                    }
                                } else {
                                    // Return to center
                                    scope.launch {
                                        launch {
                                            offsetX.animateTo(0f, animationSpec = tween(400))
                                        }
                                        launch {
                                            offsetY.animateTo(0f, animationSpec = tween(400))
                                        }
                                        launch {
                                            rotation.animateTo(0f, animationSpec = tween(400))
                                        }
                                    }
                                }
                            }
                        },
                        onDrag = { change, dragAmount ->
                            change.consume()

                            if (!isSwipeTriggered) {
                                scope.launch {
                                    offsetX.snapTo(offsetX.value + dragAmount.x)
                                    offsetY.snapTo(offsetY.value + dragAmount.y * 0.3f)

                                    // Smooth rotation
                                    val targetRotation = (offsetX.value / swipeThreshold * 15f).coerceIn(-15f, 15f)
                                    rotation.snapTo(targetRotation)
                                }
                            }
                        }
                    )
                },
            colors = CardDefaults.cardColors(
                containerColor = cardColor
            ),
            shape = RoundedCornerShape(24.dp),
            elevation = CardDefaults.cardElevation(
                defaultElevation = if (isDragging) 20.dp else 16.dp
            )
        ) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                // ✅ FIX: Direction indicators - alpha dinamik
                Text(
                    text = "Geç",
                    fontFamily = PoppinsFontFamily,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 32.sp,
                    color = Color(0xFFDC2C29),
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(top = 24.dp)
                        .padding(end = 42.dp)
                        .alpha(leftIndicatorAlpha)
                        .graphicsLayer {
                            rotationZ = -10f
                        }
                )

                Text(
                    text = "Öğren",
                    fontFamily = PoppinsFontFamily,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 32.sp,
                    color = Color(0xFF14EA1F),
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(top = 24.dp)
                        .padding(start = 42.dp)
                        .alpha(rightIndicatorAlpha)
                        .graphicsLayer {
                            rotationZ = 10f
                        }
                )

                // ✅ FIX: Content her zaman görünür
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier.padding(32.dp)
                ) {
                    Text(
                        text = word,
                        fontFamily = PoppinsFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 40.sp,
                        color = Color.White,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = translation,
                        fontFamily = PoppinsFontFamily,
                        fontWeight = FontWeight.Medium,
                        fontSize = 24.sp,
                        color = Color.White.copy(alpha = 0.9f),
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}