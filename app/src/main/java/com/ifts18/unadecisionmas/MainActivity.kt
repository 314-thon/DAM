package com.ifts18.unadecisionmas

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.ifts18.unadecisionmas.audio.SoundManager
import com.ifts18.unadecisionmas.navigation.AppNavigation
import com.ifts18.unadecisionmas.ui.theme.JuegoLimpioTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Inicializar gestor multimedia global y arrancar música de fondo
        SoundManager.init(this)
        SoundManager.startBgMusic()

        setContent {
            JuegoLimpioTheme {
                AppNavigation()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // Reanudar sonido si la partida estaba activa
        SoundManager.resumeBgMusic()
    }

    override fun onPause() {
        super.onPause()
        // Pausar audio para respetar el ciclo de vida y no consumir batería en segundo plano
        SoundManager.pauseBgMusic()
    }

    override fun onDestroy() {
        super.onDestroy()
        // Liberar MediaPlayer y SoundPool
        SoundManager.release()
    }
}
