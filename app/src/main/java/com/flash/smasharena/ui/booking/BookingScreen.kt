package com.smasharena.ui.booking

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.smasharena.R
import com.smasharena.data.User
import com.smasharena.viewmodel.BookingViewModel
import java.time.LocalDate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BookingScreen(
    courtId: Long,
    currentUser: User,
    factory: BookingViewModel.Factory,
    onBooked: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val vm: BookingViewModel = viewModel(factory = factory)
    val state by vm.state.collectAsStateWithLifecycle()

    LaunchedEffect(courtId) { vm.load(courtId) }
    LaunchedEffect(state.confirmedBookingId) {
        if (state.confirmedBookingId != null) onBooked()
    }

    Scaffold(
        topBar = {
            TopAppBar(title = {
                Text(
                    text = state.court?.let { stringResource(R.string.booking_title, it.name) }
                        ?: stringResource(R.string.app_name),
                )
            })
        },
        modifier = modifier,
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(16.dp)
                .fillMaxWidth(),
        ) {
            // Date row — today + next 6 days. Keeps the prototype simple; a
            // real app would launch Material's DatePicker dialog instead.
            Text(stringResource(R.string.booking_pick_date), style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items((0..6).map { LocalDate.now().plusDays(it.toLong()) }) { date ->
                    FilterChip(
                        selected = state.date == date,
                        onClick = { vm.setDate(date) },
                        label = { Text(date.dayOfWeek.name.take(3) + " " + date.dayOfMonth) },
                    )
                }
            }

            Spacer(Modifier.height(16.dp))
            Text(stringResource(R.string.booking_pick_start), style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))
            // Hourly slots from 6am to 10pm in 30-minute increments.
            val slots = remember6To22HalfHours()
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(slots) { slot ->
                    val selected = state.startHour == slot.first && state.startMinute == slot.second
                    FilterChip(
                        selected = selected,
                        onClick = { vm.setStart(slot.first, slot.second) },
                        label = { Text(formatHourMinute(slot.first, slot.second)) },
                    )
                }
            }

            Spacer(Modifier.height(16.dp))
            Text(stringResource(R.string.booking_pick_duration), style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(30, 60, 90, 120).forEach { mins ->
                    FilterChip(
                        selected = state.durationMinutes == mins,
                        onClick = { vm.setDuration(mins) },
                        label = { Text("$mins min") },
                    )
                }
            }

            Spacer(Modifier.height(16.dp))
            AssistChip(onClick = {}, label = { Text(stringResource(R.string.booking_peak_hint)) })
            Spacer(Modifier.height(4.dp))
            AssistChip(onClick = {}, label = { Text(stringResource(R.string.booking_daily_cap_hint)) })

            state.errorRes?.let { res ->
                Spacer(Modifier.height(16.dp))
                Text(
                    text = stringResource(res),
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }

            Spacer(Modifier.height(24.dp))
            Button(
                onClick = { vm.confirm(currentUser) },
                enabled = !state.isSubmitting && state.court != null,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(stringResource(R.string.booking_confirm))
            }
        }
    }
}

private fun remember6To22HalfHours(): List<Pair<Int, Int>> = buildList {
    for (h in 6..22) {
        add(h to 0)
        if (h != 22) add(h to 30)
    }
}

private fun formatHourMinute(h: Int, m: Int): String {
    val hour12 = ((h + 11) % 12) + 1
    val ampm = if (h < 12) "AM" else "PM"
    val mm = m.toString().padStart(2, '0')
    return "$hour12:$mm $ampm"
}
