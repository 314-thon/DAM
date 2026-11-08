package com.ifts18.unadecisionmas.sensor

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import kotlin.math.sqrt

data class AccelerometerReading(
    val x: Float = 0f,
    val y: Float = 0f,
    val z: Float = 0f,
    val gForce: Float = 1f,
    val isSensorAvailable: Boolean = true
)

/**
 * Gestor de sensores nativos (Acelerómetro) y vibración háptica.
 * Respeta estrictamente el ciclo de vida (se pausa en ON_PAUSE y se libera en onDispose).
 */
object HapticFeedbackHelper {
    fun vibrateShort(context: Context, durationMs: Long = 45L) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vm = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                vm?.defaultVibrator?.vibrate(
                    VibrationEffect.createOneShot(durationMs, VibrationEffect.DEFAULT_AMPLITUDE)
                )
            } else {
                @Suppress("DEPRECATION")
                val v = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    v?.vibrate(VibrationEffect.createOneShot(durationMs, VibrationEffect.DEFAULT_AMPLITUDE))
                } else {
                    @Suppress("DEPRECATION")
                    v?.vibrate(durationMs)
                }
            }
        } catch (_: Exception) {}
    }

    fun vibrateAlertPattern(context: Context) {
        try {
            val pattern = longArrayOf(0, 70, 50, 90)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vm = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                vm?.defaultVibrator?.vibrate(VibrationEffect.createWaveform(pattern, -1))
            } else {
                @Suppress("DEPRECATION")
                val v = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    v?.vibrate(VibrationEffect.createWaveform(pattern, -1))
                } else {
                    @Suppress("DEPRECATION")
                    v?.vibrate(140L)
                }
            }
        } catch (_: Exception) {}
    }
}

@Composable
fun rememberAccelerometerMonitor(
    enabled: Boolean = true,
    shakeThresholdG: Float = 1.45f,
    minIntervalMs: Long = 110L,
    onShakeDetected: (gForce: Float) -> Unit
): State<AccelerometerReading> {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val currentOnShake = rememberUpdatedState(onShakeDetected)
    val readingState = remember { mutableStateOf(AccelerometerReading()) }

    DisposableEffect(context, lifecycleOwner, enabled) {
        val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
        val accelerometer = sensorManager?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

        if (accelerometer == null) {
            readingState.value = AccelerometerReading(isSensorAvailable = false)
        }

        var lastShakeTimestamp = 0L

        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent?) {
                if (event == null || !enabled) return
                val x = event.values[0]
                val y = event.values[1]
                val z = event.values[2]

                val gX = x / SensorManager.GRAVITY_EARTH
                val gY = y / SensorManager.GRAVITY_EARTH
                val gZ = z / SensorManager.GRAVITY_EARTH
                val gForce = sqrt(gX * gX + gY * gY + gZ * gZ)

                readingState.value = AccelerometerReading(
                    x = x,
                    y = y,
                    z = z,
                    gForce = gForce,
                    isSensorAvailable = true
                )

                if (gForce >= shakeThresholdG) {
                    val now = System.currentTimeMillis()
                    if (now - lastShakeTimestamp >= minIntervalMs) {
                        lastShakeTimestamp = now
                        currentOnShake.value(gForce)
                    }
                }
            }

            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
        }

        fun register() {
            if (enabled && accelerometer != null) {
                sensorManager.registerListener(
                    listener,
                    accelerometer,
                    SensorManager.SENSOR_DELAY_GAME
                )
            }
        }

        fun unregister() {
            try {
                sensorManager?.unregisterListener(listener)
            } catch (_: Exception) {}
        }

        val lifecycleObserver = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> register()
                Lifecycle.Event.ON_PAUSE, Lifecycle.Event.ON_STOP -> unregister()
                else -> Unit
            }
        }

        lifecycleOwner.lifecycle.addObserver(lifecycleObserver)
        register()

        onDispose {
            lifecycleOwner.lifecycle.removeObserver(lifecycleObserver)
            unregister()
        }
    }

    return readingState
}
