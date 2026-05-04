package com.smasharena.ui.courts

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
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
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.smasharena.R
import com.smasharena.data.Court
import com.smasharena.viewmodel.CourtsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CourtsScreen(
    factory: CourtsViewModel.Factory,
    onCourtSelected: (courtId: Long) -> Unit,
    onMyBookings: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val vm: CourtsViewModel = viewModel(factory = factory)
    val courts by vm.courts.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.courts_title)) },
                actions = {
                    TextButton(onClick = onMyBookings) {
                        Text(stringResource(R.string.courts_my_bookings))
                    }
                },
            )
        },
        modifier = modifier,
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
        ) {
            items(courts, key = { it.id }) { court ->
                CourtCard(court = court, onClick = { onCourtSelected(court.id) })
                Spacer(Modifier.height(12.dp))
            }
        }
    }
}

@Composable
private fun CourtCard(court: Court, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(text = court.name, style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(4.dp))
            Text(text = court.description, style = MaterialTheme.typography.bodyMedium)
        }
    }
}
