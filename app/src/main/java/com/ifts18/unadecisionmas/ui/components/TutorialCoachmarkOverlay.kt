package com.ifts18.unadecisionmas.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ifts18.unadecisionmas.ui.theme.BordeCyan
import com.ifts18.unadecisionmas.ui.theme.BordeMagenta
import com.ifts18.unadecisionmas.ui.theme.SpaceGrotesk
import com.ifts18.unadecisionmas.ui.theme.TextoSecundario
import com.ifts18.unadecisionmas.ui.theme.VerdeExito
import kotlin.math.roundToInt

enum class TutorialStep(
    val stepNumber: Int,
    val title: String,
    val description: String
) {
    NICKNAME(
        stepNumber = 1,
        title = "Tu Nombre o Apodo",
        description = "Escribí tu nombre acá para identificarte en las partidas y en la tabla de posiciones."
    ),
    AVATAR(
        stepNumber = 2,
        title = "Tu Foto de Perfil",
        description = "Tocá tu foto de perfil para entrar al selector de avatares y confirmar el que más te guste."
    ),
    MUTE_BUTTON(
        stepNumber = 3,
        title = "Control de Sonido",
        description = "Clickeá acá para silenciar el sonido del juego cuando lo necesites."
    ),
    GAMES_SECTION(
        stepNumber = 4,
        title = "Sección de Juegos",
        description = "Esta es la sección de juegos, te invitamos a pasarlos con el puntaje más alto que puedas conseguir."
    ),
    RANKING_SECTION(
        stepNumber = 5,
        title = "Ranking y Medallas",
        description = "Compite con tus amigos para ver quién puede llegar más lejos y compara los resultados aquí."
    )
}

@Composable
fun TutorialCoachmarkOverlay(
    currentStep: TutorialStep,
    targetBoundsInRoot: Rect?,
    canAdvanceCurrentStep: Boolean,
    onTargetClicked: () -> Unit,
    onNextStep: () -> Unit,
    modifier: Modifier = Modifier
) {
    val density = LocalDensity.current
    val infiniteTransition = rememberInfiniteTransition(label = "coachmarkTransition")

    // Posición real en coordenadas Root del propio contenedor del overlay (después de statusBarsPadding).
    // Inicia en null para no dibujar el primer frame con Offset.Zero desfasado por la barra de estado.
    var overlayOriginInRoot by remember { mutableStateOf<Offset?>(null) }

    val pointerBouncePx by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = with(density) { 10.dp.toPx() },
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 560, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pointerBounce"
    )

    val spotlightPulse by infiniteTransition.animateFloat(
        initialValue = 0.70f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 750, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "spotlightPulse"
    )

    // Convertir las coordenadas globales (Root) del ítem a coordenadas locales exactas del Canvas del overlay
    val localTargetRect: Rect? = remember(targetBoundsInRoot, overlayOriginInRoot) {
        val origin = overlayOriginInRoot ?: return@remember null
        targetBoundsInRoot?.translate(-origin.x, -origin.y)
    }

    val isCircularTarget = currentStep == TutorialStep.AVATAR || currentStep == TutorialStep.MUTE_BUTTON
    val padPx = with(density) {
        when (currentStep) {
            TutorialStep.AVATAR -> 6.dp.toPx()
            TutorialStep.MUTE_BUTTON -> 5.dp.toPx()
            TutorialStep.NICKNAME, TutorialStep.RANKING_SECTION -> 6.dp.toPx()
            TutorialStep.GAMES_SECTION -> 8.dp.toPx()
        }
    }
    val cornerRadiusPx = with(density) {
        when (currentStep) {
            TutorialStep.NICKNAME -> 50.dp.toPx()
            TutorialStep.RANKING_SECTION -> 16.dp.toPx()
            else -> 18.dp.toPx()
        }
    }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .onGloballyPositioned { coords ->
                overlayOriginInRoot = coords.positionInRoot()
            }
    ) {
        val containerHeightPx = with(density) { maxHeight.toPx() }
        val containerWidthPx = with(density) { maxWidth.toPx() }

        val paddedRect: Rect? = remember(
            localTargetRect,
            padPx,
            isCircularTarget,
            containerWidthPx,
            containerHeightPx
        ) {
            localTargetRect?.let { rect ->
                if (isCircularTarget) {
                    val radius = (maxOf(rect.width, rect.height) / 2f) + padPx
                    val center = rect.center
                    val left = (center.x - radius).coerceAtLeast(2f)
                    val top = (center.y - radius).coerceAtLeast(2f)
                    Rect(
                        left = left,
                        top = top,
                        right = (center.x + radius).coerceAtMost((containerWidthPx - 2f).coerceAtLeast(left + 4f)),
                        bottom = (center.y + radius).coerceAtMost((containerHeightPx - 2f).coerceAtLeast(top + 4f))
                    )
                } else {
                    val left = (rect.left - padPx).coerceAtLeast(4f)
                    val top = (rect.top - padPx).coerceAtLeast(4f)
                    Rect(
                        left = left,
                        top = top,
                        right = (rect.right + padPx).coerceAtMost((containerWidthPx - 4f).coerceAtLeast(left + 4f)),
                        bottom = (rect.bottom + padPx).coerceAtMost((containerHeightPx - 4f).coerceAtLeast(top + 4f))
                    )
                }
            }
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(currentStep, paddedRect) {
                    detectTapGestures { tapOffset ->
                        val rect = paddedRect
                        if (rect != null && rect.contains(tapOffset)) {
                            onTargetClicked()
                        }
                    }
                }
        ) {

        // 1. Sombreado oscuro con recorte transparente perfectamente centrado sobre el objetivo activo
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    compositingStrategy = CompositingStrategy.Offscreen
                }
        ) {
            drawRect(color = Color.Black.copy(alpha = 0.84f))

            paddedRect?.let { rect ->
                if (isCircularTarget) {
                    val radius = minOf(rect.width, rect.height) / 2f
                    drawCircle(
                        color = Color.Transparent,
                        radius = radius,
                        center = rect.center,
                        blendMode = BlendMode.Clear
                    )
                    drawCircle(
                        brush = Brush.linearGradient(
                            colors = listOf(
                                BordeCyan.copy(alpha = spotlightPulse),
                                BordeMagenta.copy(alpha = spotlightPulse)
                            ),
                            start = Offset(rect.left, rect.top),
                            end = Offset(rect.right, rect.bottom)
                        ),
                        radius = radius,
                        center = rect.center,
                        style = Stroke(width = 3.dp.toPx())
                    )
                } else {
                    drawRoundRect(
                        color = Color.Transparent,
                        topLeft = Offset(rect.left, rect.top),
                        size = Size(rect.width, rect.height),
                        cornerRadius = CornerRadius(cornerRadiusPx, cornerRadiusPx),
                        blendMode = BlendMode.Clear
                    )

                    drawRoundRect(
                        brush = Brush.linearGradient(
                            colors = listOf(
                                BordeCyan.copy(alpha = spotlightPulse),
                                BordeMagenta.copy(alpha = spotlightPulse)
                            ),
                            start = Offset(rect.left, rect.top),
                            end = Offset(rect.right, rect.bottom)
                        ),
                        topLeft = Offset(rect.left, rect.top),
                        size = Size(rect.width, rect.height),
                        cornerRadius = CornerRadius(cornerRadiusPx, cornerRadiusPx),
                        style = Stroke(width = 3.dp.toPx())
                    )
                }
            }
        }

        // 2. Mano / Cursor animado apuntando directamente al elemento iluminado
        paddedRect?.let { rect ->
            val handSizePx = with(density) { 46.dp.toPx() }
            val placeHandAbove = rect.bottom + handSizePx + with(density) { 28.dp.toPx() } > containerHeightPx
            val handX = (rect.center.x - handSizePx / 2f)
                .coerceIn(12f, (containerWidthPx - handSizePx - 12f).coerceAtLeast(12f))
            val handY = if (placeHandAbove) {
                (rect.top - handSizePx - pointerBouncePx - 6f).coerceAtLeast(8f)
            } else {
                (rect.bottom + pointerBouncePx + 4f).coerceAtMost(containerHeightPx - handSizePx - 8f)
            }

            Box(
                modifier = Modifier
                    .offset { IntOffset(handX.roundToInt(), handY.roundToInt()) }
                    .size(46.dp)
                    .graphicsLayer {
                        rotationZ = if (placeHandAbove) 180f else 0f
                    }
                    .clip(CircleShape)
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                BordeCyan.copy(alpha = 0.38f),
                                Color(0xFF0A1626).copy(alpha = 0.92f)
                            )
                        )
                    )
                    .border(2.dp, BordeCyan, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.TouchApp,
                    contentDescription = "Apuntador del tutorial",
                    tint = Color.White,
                    modifier = Modifier.size(28.dp)
                )
            }
        }

        // 3. Tarjeta de explicación posicionada donde no tape al elemento ni a la mano
        val placeCardAtTop = remember(paddedRect, containerHeightPx) {
            val rect = paddedRect ?: return@remember false
            val spaceAbove = rect.top
            val spaceBelow = containerHeightPx - rect.bottom
            spaceAbove > spaceBelow
        }

        Column(
            modifier = Modifier
                .align(if (placeCardAtTop) Alignment.TopCenter else Alignment.BottomCenter)
                .padding(
                    top = if (placeCardAtTop) 22.dp else 0.dp,
                    bottom = if (!placeCardAtTop) 22.dp else 0.dp,
                    start = 18.dp,
                    end = 18.dp
                )
                .fillMaxWidth()
                .clip(RoundedCornerShape(22.dp))
                .background(Color(0xFF0D1628).copy(alpha = 0.97f))
                .border(
                    width = 1.8.dp,
                    brush = Brush.horizontalGradient(listOf(BordeCyan, BordeMagenta)),
                    shape = RoundedCornerShape(22.dp)
                )
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) {}
                .padding(horizontal = 18.dp, vertical = 15.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .background(BordeCyan.copy(alpha = 0.16f))
                        .border(1.dp, BordeCyan.copy(alpha = 0.6f), RoundedCornerShape(50))
                        .padding(horizontal = 10.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = "TUTORIAL • PASO ${currentStep.stepNumber}/5",
                        fontFamily = SpaceGrotesk,
                        fontWeight = FontWeight.Bold,
                        fontSize = 10.5.sp,
                        color = BordeCyan
                    )
                }

                // Indicadores de progreso de los 5 pasos
                Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                    TutorialStep.entries.forEach { step ->
                        val active = step.stepNumber <= currentStep.stepNumber
                        Box(
                            modifier = Modifier
                                .height(6.dp)
                                .width(if (step == currentStep) 18.dp else 7.dp)
                                .clip(RoundedCornerShape(50))
                                .background(
                                    if (step == currentStep) BordeCyan
                                    else if (active) VerdeExito
                                    else Color.White.copy(alpha = 0.2f)
                                )
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(9.dp))

            Text(
                text = currentStep.title,
                fontFamily = SpaceGrotesk,
                fontWeight = FontWeight.Bold,
                fontSize = 17.sp,
                color = Color.White
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = currentStep.description,
                fontFamily = SpaceGrotesk,
                fontSize = 13.5.sp,
                lineHeight = 18.5.sp,
                color = TextoSecundario
            )

            Spacer(modifier = Modifier.height(13.dp))

            if (currentStep == TutorialStep.AVATAR && !canAdvanceCurrentStep) {
                // En el paso 2 es obligatorio tocar el avatar y elegir otro para avanzar
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp)
                        .clip(RoundedCornerShape(13.dp))
                        .background(BordeCyan.copy(alpha = 0.18f))
                        .border(1.4.dp, BordeCyan, RoundedCornerShape(13.dp))
                        .clickable { onTargetClicked() },
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.TouchApp,
                            contentDescription = null,
                            tint = BordeCyan,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Tocá tu avatar para abrir el selector",
                            fontFamily = SpaceGrotesk,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.5.sp,
                            color = Color.White,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            } else {
                val isLastStep = currentStep == TutorialStep.RANKING_SECTION
                val buttonLabel = when (currentStep) {
                    TutorialStep.NICKNAME -> "Confirmar nombre y seguir"
                    TutorialStep.RANKING_SECTION -> "¡Empezar a jugar!"
                    else -> "Siguiente"
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp)
                        .clip(RoundedCornerShape(13.dp))
                        .background(
                            if (canAdvanceCurrentStep) {
                                Brush.horizontalGradient(listOf(VerdeExito, BordeCyan))
                            } else {
                                Brush.horizontalGradient(
                                    listOf(Color(0xFF253042), Color(0xFF253042))
                                )
                            }
                        )
                        .clickable(enabled = canAdvanceCurrentStep) { onNextStep() },
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = buttonLabel,
                            fontFamily = SpaceGrotesk,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = if (canAdvanceCurrentStep) Color(0xFF041019) else Color.White.copy(alpha = 0.5f)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Icon(
                            imageVector = if (isLastStep) Icons.Default.Check else Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = null,
                            tint = if (canAdvanceCurrentStep) Color(0xFF041019) else Color.White.copy(alpha = 0.5f),
                            modifier = Modifier.size(17.dp)
                        )
                    }
                }
            }
        }
        }
    }
}
