package com.dragongotchi.app.ui.screens.onboarding

import androidx.compose.ui.res.stringResource
import com.dragongotchi.app.R
import com.dragongotchi.app.data.preferences.findActivity

import com.dragongotchi.app.ui.theme.dragonKingdomBackground

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner

/**
 * PANTALLA DE AVISO DE PRIMERA INSTALACION (requisito 1).
 * Sin estos permisos Android mataria el tracking en segundo plano:
 *  1. Ubicacion precisa.
 *  2. Ubicacion "Permitir siempre" (segundo plano, Android 10+).
 *  3. Reconocimiento de actividad (podometro).
 *  4. Notificaciones (la notificacion persistente mantiene vivo el servicio).
 *  5. Exencion de optimizacion de bateria (clave en Samsung/Xiaomi/Huawei).
 */
@Composable
fun PermissionsOnboardingScreen(onFinished: () -> Unit) {
    val context = LocalContext.current
    val activity = context.findActivity()
    // Contador para refrescar los checks al volver de Ajustes / conceder permisos
    var refreshTick by remember { mutableIntStateOf(0) }
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) refreshTick++
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val fineLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { refreshTick++ }

    val hasFine = remember(refreshTick) { context.hasPermission(Manifest.permission.ACCESS_FINE_LOCATION) }
    val hasBackground = remember(refreshTick) {
        Build.VERSION.SDK_INT < Build.VERSION_CODES.Q ||
            context.hasPermission(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
    }
    val hasActivityRecognition = remember(refreshTick) {
        Build.VERSION.SDK_INT < Build.VERSION_CODES.Q ||
            context.hasPermission(Manifest.permission.ACTIVITY_RECOGNITION)
    }
    val hasNotifications = remember(refreshTick) {
        Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            context.hasPermission(Manifest.permission.POST_NOTIFICATIONS)
    }
    val mandatoryReady = hasFine

    Column(
        modifier = Modifier.fillMaxSize().dragonKingdomBackground().verticalScroll(rememberScrollState()).padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text(stringResource(R.string.onb_welcome), style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Text(
            stringResource(R.string.onb_intro),
            style = MaterialTheme.typography.bodyMedium
        )

        PermissionRow(
            title = stringResource(R.string.onb_loc_title),
            description = stringResource(R.string.onb_loc_desc),
            granted = hasFine,
            buttonText = stringResource(R.string.btn_grant)
        ) {
            val perms = mutableListOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            )
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) perms.add(Manifest.permission.ACTIVITY_RECOGNITION)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) perms.add(Manifest.permission.POST_NOTIFICATIONS)
            fineLauncher.launch(perms.toTypedArray())
        }

        PermissionRow(
            title = stringResource(R.string.onb_bg_title),
            description = stringResource(R.string.onb_bg_desc),
            granted = hasBackground,
            buttonText = stringResource(R.string.btn_open_settings)
        ) {
            // Android 11+ no permite pedir background location con un dialogo normal:
            // hay que mandar al usuario a los ajustes de la app.
            activity?.let { openAppSettings(it) }
        }

        PermissionRow(
            title = stringResource(R.string.onb_steps_title),
            description = stringResource(R.string.onb_steps_desc),
            granted = hasActivityRecognition,
            buttonText = stringResource(R.string.btn_grant)
        ) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                fineLauncher.launch(arrayOf(Manifest.permission.ACTIVITY_RECOGNITION))
            }
        }

        PermissionRow(
            title = stringResource(R.string.onb_notif_title),
            description = stringResource(R.string.onb_notif_desc),
            granted = hasNotifications,
            buttonText = stringResource(R.string.btn_grant)
        ) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                fineLauncher.launch(arrayOf(Manifest.permission.POST_NOTIFICATIONS))
            }
        }

        // CONSEJO DE BATERIA (solo texto, una unica vez al instalar):
        // en Europa no se puede solicitar el permiso de exencion de
        // optimizacion de bateria (REQUEST_IGNORE_BATTERY_OPTIMIZATIONS)
        // ni llevar al usuario a ese ajuste desde la app. Se muestra
        // esta sugerencia informativa y nada mas: sin botones ni intents.
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Column(Modifier.padding(14.dp)) {
                Text(stringResource(R.string.onb_battery_title), fontWeight = FontWeight.Bold)
                Text(
                    stringResource(R.string.onb_battery_desc),
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }

        Spacer(Modifier.height(4.dp))
        Button(
            onClick = onFinished,
            enabled = mandatoryReady,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(if (mandatoryReady) stringResource(R.string.onb_start) else stringResource(R.string.onb_grant_at_least))
        }
        OutlinedButton(onClick = onFinished, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.onb_skip))
        }
        Text(
            stringResource(R.string.onb_footer),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun PermissionRow(
    title: String,
    description: String,
    granted: Boolean,
    buttonText: String,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (granted) MaterialTheme.colorScheme.primaryContainer
            else MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(Modifier.padding(14.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(title, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                Text(if (granted) "✅" else "⬜")
            }
            Text(description, style = MaterialTheme.typography.bodySmall)
            if (!granted) {
                Spacer(Modifier.height(8.dp))
                OutlinedButton(onClick = onClick) { Text(buttonText) }
            }
        }
    }
}

// ---------- Utilidades de permisos ----------
fun Context.hasPermission(permission: String): Boolean =
    ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED

fun openAppSettings(context: Context) {
    runCatching {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.parse("package:${context.packageName}")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }
}
