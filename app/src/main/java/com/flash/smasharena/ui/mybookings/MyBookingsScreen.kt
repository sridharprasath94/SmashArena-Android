package com.smasharena.ui.mybookings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.smasharena.R
import com.smasharena.data.Booking
import com.smasharena.util.TimeFormat
import com.smasharena.viewmodel.MyBookingsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyBookingsScreen(
    userId: Long,
    factory: MyBookingsViewModel.Factory,
    modifier: Modifier = Modifier,
) {
    val vm: MyBookingsViewModel = viewModel(factory = factory)
    val bookings by vm.bookings.collectAsStateWithLifecycle()

    LaunchedEffect(userId) { vm.setUser(userId) }

    Scaffold(
        topBar = { TopAppBar(title = { Text(stringResource(R.string.courts_my_bookings)) }) },
        modifier = modifier,
    ) { padding ->
        if (bookings.isEmpty()) {
            Box(
                Modifier
                    .padding(padding)
                    .fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                Text(stringResource(R.string.my_bookings_empty))
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .padding(padding)
                    .padding(16.dp)
                    .fillMaxSize(),
            ) {
                items(bookings, key = { it.id }) { booking ->
                    BookingRow(booking = booking, onCancel = { vm.cancel(booking) })
                    Spacer(Modifier.height(12.dp))
                }
            }
        }
    }
}

@Composable
private fun BookingRow(booking: Booking, onCancel: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.padding(end = 16.dp)) {
                Text(
                    text = TimeFormat.formatDate(booking.startEpochMs),
                    style = MaterialTheme.typography.titleMedium,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = TimeFormat.formatTime(booking.startEpochMs) + " — " +
                        TimeFormat.formatTime(booking.endEpochMs),
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
            TextButton(onClick = onCancel) {
                Text(stringResource(R.string.my_bookings_cancel))
            }
        }
    }
}
