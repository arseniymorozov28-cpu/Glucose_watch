package com.weargluco.watch.ui.login

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.wear.compose.foundation.lazy.ScalingLazyColumn
import androidx.wear.compose.foundation.lazy.items
import androidx.wear.compose.foundation.lazy.rememberScalingLazyListState
import androidx.wear.compose.material.Button
import androidx.wear.compose.material.Chip
import androidx.wear.compose.material.ChipDefaults
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.Scaffold
import androidx.wear.compose.material.Text
import androidx.wear.compose.material.TimeText
import androidx.wear.compose.material.dialog.Dialog
import androidx.wear.compose.ui.tooling.preview.WearPreviewLargeRound

@Composable
fun LoginScreen(
    viewModel: LoginViewModel,
    onLoginSuccess: () -> Unit
) {
    val state by viewModel.uiState.collectAsState()
    var showRegionDialog by remember { mutableStateOf(false) }

    LaunchedEffect(state.isLoggedIn) {
        if (state.isLoggedIn) {
            onLoginSuccess()
        }
    }

    Dialog(
        showDialog = showRegionDialog,
        onDismissRequest = { showRegionDialog = false }
    ) {
        val listState = rememberScalingLazyListState()
        ScalingLazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            item {
                Text(text = "Select Region", style = MaterialTheme.typography.caption1)
            }
            items(listOf("eu", "us", "ae", "ap", "au", "fr", "jp")) { region ->
                Chip(
                    onClick = {
                        viewModel.updateRegion(region)
                        showRegionDialog = false
                    },
                    label = { Text(region.uppercase()) },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }

    Scaffold(
        timeText = { TimeText() }
    ) {
        ScalingLazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            item {
                Text(
                    text = "GlucoWatch",
                    style = MaterialTheme.typography.title2,
                    color = MaterialTheme.colors.primary
                )
            }

            item { Spacer(modifier = Modifier.height(12.dp)) }

            item {
                WearTextField(
                    value = state.email,
                    onValueChange = { viewModel.updateEmail(it) },
                    label = "Email",
                    keyboardType = KeyboardType.Email
                )
            }

            item { Spacer(modifier = Modifier.height(8.dp)) }

            item {
                WearTextField(
                    value = state.password,
                    onValueChange = { viewModel.updatePassword(it) },
                    label = "Password",
                    keyboardType = KeyboardType.Password,
                    isPassword = true
                )
            }

            item { Spacer(modifier = Modifier.height(8.dp)) }

            item {
                Chip(
                    onClick = { showRegionDialog = true },
                    label = { Text("Region: ${state.region.uppercase()}") },
                    colors = ChipDefaults.secondaryChipColors(),
                    modifier = Modifier.fillMaxWidth()
                )
            }

            item { Spacer(modifier = Modifier.height(8.dp)) }

            if (state.error != null) {
                item {
                    Text(
                        text = state.error!!,
                        color = MaterialTheme.colors.error,
                        fontSize = 12.sp
                    )
                }
                item { Spacer(modifier = Modifier.height(4.dp)) }
            }

            item {
                Button(
                    onClick = { viewModel.login() },
                    enabled = !state.isLoading,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = if (state.isLoading) "Connecting..." else "Connect",
                        fontSize = 12.sp
                    )
                }
            }
        }
    }
}

@Composable
fun WearTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    keyboardType: KeyboardType = KeyboardType.Text,
    isPassword: Boolean = false
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = label, style = MaterialTheme.typography.caption2)
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
            visualTransformation = if (isPassword) PasswordVisualTransformation() else androidx.compose.ui.text.input.VisualTransformation.None,
            singleLine = true,
            textStyle = TextStyle(
                color = MaterialTheme.colors.onSurface,
                fontSize = 14.sp
            ),
            cursorBrush = SolidColor(MaterialTheme.colors.primary),
            modifier = Modifier
                .fillMaxWidth()
                .padding(4.dp)
        )
    }
}
