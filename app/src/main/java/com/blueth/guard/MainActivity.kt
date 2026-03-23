package com.blueth.guard

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.blueth.guard.data.prefs.ThemeMode
import com.blueth.guard.data.prefs.UserPreferences
import com.blueth.guard.ui.navigation.MainNavGraph
import com.blueth.guard.ui.screens.OnboardingScreen
import com.blueth.guard.ui.theme.BluethGuardTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var userPreferences: UserPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val themeMode by userPreferences.themeMode.collectAsState(initial = ThemeMode.DARK)
            val onboardingCompleted by userPreferences.onboardingCompleted.collectAsState(initial = true)

            val darkTheme = when (themeMode) {
                ThemeMode.DARK -> true
                ThemeMode.AMOLED -> true
                ThemeMode.LIGHT -> false
                ThemeMode.SYSTEM -> isSystemInDarkTheme()
            }

            BluethGuardTheme(darkTheme = darkTheme, amoledTheme = themeMode == ThemeMode.AMOLED) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    if (onboardingCompleted) {
                        MainNavGraph()
                    } else {
                        OnboardingScreen(
                            onComplete = {
                                // Onboarding screen handles marking complete internally
                            }
                        )
                    }
                }
            }
        }
    }
}
