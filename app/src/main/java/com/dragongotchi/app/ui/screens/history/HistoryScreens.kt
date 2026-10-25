package com.dragongotchi.app.ui.screens.history

import androidx.compose.ui.res.stringResource
import com.dragongotchi.app.R

import com.dragongotchi.app.ui.theme.dragonKingdomBackground

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.dragongotchi.app.data.local.RoutePointEntity
import com.dragongotchi.app.data.local.TrainingSessionEntity
import com.dragongotchi.app.domain.ActivityType
import com.dragongotchi.app.domain.GeoPoint
import com.dragongotchi.app.domain.StatsCalculator
import com.dragongotchi.app.ui.components.BackTopBar
import com.dragongotchi.app.ui.components.RouteMap
import com.dragongotchi.app.ui.components.StatCard
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private fun historyDateFormat(locale: java.util.Locale) =
    SimpleDateFormat("dd MMM yyyy - HH:mm", locale)

@Composable
fun HistoryScreen(
    sessionsFlow: Flow<List<TrainingSessionEntity>>,
    onSessionClicked: (Long) -> Unit
) {
    val sessions by sessionsFlow.collectAsState(initial = emptyList())
    val dateFormat = historyDateFormat(
        androidx.compose.ui.platform.LocalConfiguration.current.locales.get(0) ?: Locale.getDefault()
    )
    Column(Modifier.fillMaxSize().dragonKingdomBackground().padding(16.dp)) {
        Text(stringResource(R.string.history_title), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(4.dp))
        Text(
            stringResource(R.string.history_summary, sessions.size, StatsCalculator.formatDistanceKm(sessions.sumOf { it.distanceM })),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(12.dp))
        if (sessions.isEmpty()) {
            Text(
                stringResource(R.string.history_empty),
                style = MaterialTheme.typography.bodyLarge
            )
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                items(sessions, key = { it.id }) { session ->
                    SessionCard(session = session, onClick = { onSessionClicked(session.id) })
                }
            }
        }
    }
}

@Composable
private fun SessionCard(session: TrainingSessionEntity, onClick: () -> Unit) {
    val dateFormat = historyDateFormat(
        androidx.compose.ui.platform.LocalConfiguration.current.locales.get(0) ?: Locale.getDefault()
    )
    val type = runCatching { ActivityType.valueOf(session.activityType) }.getOrDefault(ActivityType.RUN)
    Card(Modifier.fillMaxWidth().clickable { onClick() }) {
        Column(Modifier.padding(14.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("${type.emoji} ${stringResource(type.nameRes)}", fontWeight = FontWeight.Bold)
                Text(dateFormat.format(Date(session.startTimeMillis)), style = MaterialTheme.typography.bodySmall)
            }
            Spacer(Modifier.height(6.dp))
            Text(
                "${StatsCalculator.formatDistanceKm(session.distanceM)} km - " +
                    "${StatsCalculator.formatDuration(session.durationSec)} - " +
                    "${StatsCalculator.formatPace(session.avgPaceSecPerKm)} /km - " +
                    "${StatsCalculator.formatKcal(session.kcal)} kcal - " +
                    stringResource(R.string.history_steps_count, session.steps) + " - +${session.expGained} EXP",
                style = MaterialTheme.typography.bodyMedium
            )
            if (session.isFlaggedSuspicious) {
                Text(
                    stringResource(R.string.history_card_suspicious),
                    color = Color(0xFFC62828),
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}

@Composable
fun SessionDetailScreen(
    sessionFlow: Flow<TrainingSessionEntity?>,
    pointsFlow: Flow<List<RoutePointEntity>>,
    onDelete: suspend (Long) -> Unit,
    onBack: () -> Unit
) {
    val session by sessionFlow.collectAsState(initial = null)
    val dateFormat = historyDateFormat(
        androidx.compose.ui.platform.LocalConfiguration.current.locales.get(0) ?: Locale.getDefault()
    )
    val points by pointsFlow.collectAsState(initial = emptyList())
    val scope = rememberCoroutineScope()
    var showDeleteDialog by remember { mutableStateOf(false) }

    val current = session
    if (current == null) {
        Column(Modifier.fillMaxSize().dragonKingdomBackground().padding(16.dp)) {
            BackTopBar(title = stringResource(R.string.history_detail_title), onBack = onBack)
            Text(stringResource(R.string.history_loading_session), style = MaterialTheme.typography.bodyLarge)
        }
        return
    }

    val type = runCatching { ActivityType.valueOf(current.activityType) }.getOrDefault(ActivityType.RUN)
    val geoPoints = points.map {
        GeoPoint(it.latitude, it.longitude, it.altitude, it.timeMillis, it.speedMps, it.accuracyM)
    }

    Column(Modifier.fillMaxSize().dragonKingdomBackground().padding(16.dp)) {
        BackTopBar(
            title = "${type.emoji} ${stringResource(type.nameRes)}",
            subtitle = dateFormat.format(Date(current.startTimeMillis)),
            onBack = onBack
        )
        if (current.isFlaggedSuspicious) {
            Text(
                stringResource(R.string.history_suspicious),
                color = Color(0xFFC62828),
                style = MaterialTheme.typography.bodySmall
            )
        }
        Spacer(Modifier.height(12.dp))

        // Mapa gratuito OpenStreetMap con la ruta completa encuadrada.
        // En el historial el fin se marca con el punto clasico, no con el
        // dragon (el dragon-marcador es para la posicion en vivo).
        RouteMap(
            points = geoPoints,
            modifier = Modifier.fillMaxWidth().height(300.dp),
            fitEntireRoute = true
        )
        Spacer(Modifier.height(12.dp))

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            StatCard(stringResource(R.string.stat_distance), StatsCalculator.formatDistanceKm(current.distanceM), "km", Modifier.weight(1f))
            StatCard(stringResource(R.string.stat_time), StatsCalculator.formatDuration(current.durationSec), modifier = Modifier.weight(1f))
            StatCard(stringResource(R.string.stat_avg_pace), StatsCalculator.formatPace(current.avgPaceSecPerKm), "/km", Modifier.weight(1f))
        }
        Spacer(Modifier.height(8.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            StatCard(stringResource(R.string.stat_calories), StatsCalculator.formatKcal(current.kcal), "kcal", Modifier.weight(1f))
            StatCard(stringResource(R.string.stat_steps), current.steps.toString() + if (current.stepsAreEstimated) stringResource(R.string.stat_steps_estimated_suffix) else "", modifier = Modifier.weight(1f))
            StatCard(stringResource(R.string.stat_exp_gained), "+${current.expGained}", modifier = Modifier.weight(1f))
        }
        Spacer(Modifier.height(8.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            StatCard(stringResource(R.string.stat_max_speed), "%.1f".format(current.maxSpeedKmh), "km/h", Modifier.weight(1f))
            StatCard(stringResource(R.string.stat_gps_points), current.acceptedGpsPoints.toString(), modifier = Modifier.weight(1f))
            StatCard(stringResource(R.string.stat_rejected), current.rejectedGpsPoints.toString(), modifier = Modifier.weight(1f))
        }

        Spacer(Modifier.height(20.dp))
        Button(
            onClick = { showDeleteDialog = true },
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFC62828)),
            modifier = Modifier.fillMaxWidth()
        ) { Text(stringResource(R.string.history_delete)) }
    }

    if (showDeleteDialog) {
        // Textos resueltos fuera del AlertDialog (ver LiveTrackingScreen:
        // dentro de un Dialog los recursos caen al idioma del sistema).
        val deleteDialogTitle = stringResource(R.string.dialog_delete_title)
        val deleteDialogText = stringResource(R.string.dialog_delete_text)
        val deleteDialogConfirm = stringResource(R.string.btn_delete)
        val deleteDialogCancel = stringResource(R.string.btn_cancel)
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text(deleteDialogTitle) },
            text = { Text(deleteDialogText) },
            confirmButton = {
                Button(onClick = {
                    showDeleteDialog = false
                    scope.launch { onDelete(current.id); onBack() }
                }) { Text(deleteDialogConfirm) }
            },
            dismissButton = { TextButton(onClick = { showDeleteDialog = false }) { Text(deleteDialogCancel) } }
        )
    }
}
