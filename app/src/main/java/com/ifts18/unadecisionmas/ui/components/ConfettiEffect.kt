package com.ifts18.unadecisionmas.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.rotate
import com.ifts18.unadecisionmas.ui.theme.BordeCyan
import com.ifts18.unadecisionmas.ui.theme.BordeMagenta
import com.ifts18.unadecisionmas.ui.theme.GlowCyan
import com.ifts18.unadecisionmas.ui.theme.VerdeExito

data class Particle(
    var x: Float,
    var y: Float,
    var speedY: Float,
    var speedX: Float,
    var rotation: Float,
    var rotationSpeed: Float,
    val width: Float,
    val height: Float,
    val color: Color,
    val isCircle: Boolean
)

@Composable
fun ConfettiEffect(
    modifier: Modifier = Modifier,
    particleCount: Int = 50
) {
    val colors = remember {
        listOf(
            VerdeExito,
            BordeCyan,
            BordeMagenta,
            GlowCyan,
            Color(0xFFFFD54F),
            Color(0xFFFF4081),
            Color.White
        )
    }

    val particles = remember {
        val random = kotlin.random.Random(1337)
        List(particleCount) {
            Particle(
                x = random.nextFloat(),
                y = -random.nextFloat() * 0.9f,
                speedY = 0.0022f + random.nextFloat() * 0.0045f,
                speedX = (random.nextFloat() - 0.5f) * 0.002f,
                rotation = random.nextFloat() * 360f,
                rotationSpeed = (random.nextFloat() - 0.5f) * 6f,
                width = 8f + random.nextFloat() * 8f,
                height = 5f + random.nextFloat() * 7f,
                color = colors[random.nextInt(colors.size)],
                isCircle = random.nextFloat() > 0.65f
            )
        }
    }

    var frameTime by remember { mutableLongStateOf(0L) }

    LaunchedEffect(Unit) {
        while (true) {
            withFrameNanos { time ->
                frameTime = time
                for (i in particles.indices) {
                    val p = particles[i]
                    p.y += p.speedY
                    p.x += p.speedX
                    p.rotation += p.rotationSpeed
                    if (p.y > 1.05f) {
                        p.y = -0.05f
                        p.x = kotlin.random.Random.nextFloat()
                    }
                    if (p.x < 0f) p.x = 1f
                    else if (p.x > 1f) p.x = 0f
                }
            }
        }
    }

    Canvas(modifier = modifier.fillMaxSize()) {
        @Suppress("UNUSED_VARIABLE")
        val tick = frameTime
        val w = size.width
        val h = size.height

        for (i in particles.indices) {
            val p = particles[i]
            val px = p.x * w
            val py = p.y * h

            if (py >= -20 && py <= h + 20) {
                rotate(degrees = p.rotation, pivot = Offset(px, py)) {
                    if (p.isCircle) {
                        drawCircle(
                            color = p.color,
                            radius = p.width / 2f,
                            center = Offset(px, py)
                        )
                    } else {
                        drawRect(
                            color = p.color,
                            topLeft = Offset(px - p.width / 2f, py - p.height / 2f),
                            size = Size(p.width, p.height)
                        )
                    }
                }
            }
        }
    }
}
