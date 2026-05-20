package com.weargluco.watch

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.wear.compose.material.MaterialTheme
import com.weargluco.watch.ui.login.LoginScreen
import com.weargluco.watch.ui.login.LoginViewModel
import com.weargluco.watch.ui.main.GlucoseScreen
import com.weargluco.watch.ui.main.GlucoseViewModel
import com.weargluco.watch.ui.theme.GlucoTheme

class MainActivity : ComponentActivity() {

    private lateinit var loginViewModel: LoginViewModel
    private lateinit var glucoseViewModel: GlucoseViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        loginViewModel = LoginViewModel(application)
        glucoseViewModel = GlucoseViewModel(application)

        setContent {
            GlucoTheme {
                var isLoggedIn by remember { mutableStateOf(false) }

                if (isLoggedIn) {
                    GlucoseScreen(
                        viewModel = glucoseViewModel,
                        onLogout = {
                            loginViewModel.logout()
                            isLoggedIn = false
                        }
                    )
                } else {
                    LoginScreen(
                        viewModel = loginViewModel,
                        onLoginSuccess = { isLoggedIn = true }
                    )
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
    }
}
