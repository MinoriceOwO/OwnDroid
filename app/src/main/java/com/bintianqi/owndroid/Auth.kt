package com.bintianqi.owndroid

import android.content.Context
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.biometric.BiometricPrompt.AuthenticationCallback
import androidx.biometric.BiometricPrompt.PromptInfo.Builder
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import com.bintianqi.owndroid.ui.Animations
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun AuthScreen(activity: FragmentActivity, showAuth: MutableState<Boolean>) {
    val context = activity.applicationContext
    val coroutineScope = rememberCoroutineScope()
    var canStartAuth by remember { mutableStateOf(true) }
    var fallback by remember { mutableStateOf(false) }
    var startFade by remember { mutableStateOf(false) }
    val alpha by animateFloatAsState(
        targetValue = if(startFade) 0F else 1F,
        label = "AuthScreenFade",
        animationSpec = Animations.authScreenFade
    )
    val onAuthSucceed = {
        startFade = true
        coroutineScope.launch {
            delay(300)
            showAuth.value = false
        }
    }
    val promptInfo = Builder()
        .setTitle(context.getText(R.string.authenticate))
        .setSubtitle(context.getText(R.string.auth_with_bio))
        .setConfirmationRequired(true)
    val callback = object: AuthenticationCallback() {
        override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
            super.onAuthenticationSucceeded(result)
            onAuthSucceed()
        }
        override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
            super.onAuthenticationError(errorCode, errString)
            when(errorCode) {
                BiometricPrompt.ERROR_NO_DEVICE_CREDENTIAL -> onAuthSucceed()
                BiometricPrompt.ERROR_NEGATIVE_BUTTON -> fallback = true
                else -> canStartAuth = true
            }
        }
    }
    LaunchedEffect(fallback) {
        if(fallback) {
            val fallbackPromptInfo = promptInfo
                .setAllowedAuthenticators(BiometricManager.Authenticators.DEVICE_CREDENTIAL)
                .setSubtitle(context.getText(R.string.auth_with_password))
                .build()
            val executor = ContextCompat.getMainExecutor(context)
            val biometricPrompt = BiometricPrompt(activity, executor, callback)
            biometricPrompt.authenticate(fallbackPromptInfo)
        }
    }
    Surface(
        modifier = Modifier
            .fillMaxSize()
            .alpha(alpha)
            .background(if(isSystemInDarkTheme()) Color.Black else Color.White)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)
        ) {
            LaunchedEffect(Unit) {
                delay(300)
                startAuth(activity, promptInfo, callback)
                canStartAuth = false
            }
            Text(
                text = stringResource(R.string.authenticate),
                style = MaterialTheme.typography.headlineLarge,
                color = MaterialTheme.colorScheme.onBackground
            )
            Button(
                onClick = {
                    startAuth(activity, promptInfo, callback)
                    canStartAuth = false
                },
                enabled = canStartAuth
            ) {
                Text(text = stringResource(R.string.start))
            }
        }
    }
}

private fun startAuth(activity: FragmentActivity, basicPromptInfo: Builder, callback: AuthenticationCallback) {
    val context = activity.applicationContext
    val promptInfo = basicPromptInfo
    val bioManager = BiometricManager.from(context)
    val sharedPref = context.getSharedPreferences("data", Context.MODE_PRIVATE)
    if(sharedPref.getBoolean("bio_auth", false)) {
        when(BiometricManager.BIOMETRIC_SUCCESS) {
            bioManager.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG) ->
                promptInfo
                    .setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_STRONG)
                    .setNegativeButtonText(context.getText(R.string.use_password))
            bioManager.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_WEAK) ->
                promptInfo
                    .setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_WEAK)
                    .setNegativeButtonText(context.getText(R.string.use_password))
            else -> promptInfo
                .setAllowedAuthenticators(BiometricManager.Authenticators.DEVICE_CREDENTIAL)
                .setSubtitle(context.getText(R.string.auth_with_password))
        }
    }else{
        promptInfo
            .setAllowedAuthenticators(BiometricManager.Authenticators.DEVICE_CREDENTIAL)
            .setSubtitle(context.getText(R.string.auth_with_password))
    }
    val executor = ContextCompat.getMainExecutor(context)
    val biometricPrompt = BiometricPrompt(activity, executor, callback)
    biometricPrompt.authenticate(promptInfo.build())
}
