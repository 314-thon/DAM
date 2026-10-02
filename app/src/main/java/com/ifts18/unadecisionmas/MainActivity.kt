package com.ifts18.unadecisionmas

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.ifts18.unadecisionmas.navigation.AppNavigation
import com.ifts18.unadecisionmas.ui.theme.UnaDecisionMasTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            UnaDecisionMasTheme {
                AppNavigation()
            }
        }
    }
}
