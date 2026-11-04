package com.tw0winju.tripboard.feature.trips

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tw0winju.tripboard.core.designsystem.theme.TripBoardTheme
import com.tw0winju.tripboard.core.model.Trip

/** ViewModel을 아는 쪽(stateful) */
@Composable
internal fun TripsRoute(
    viewModel: TripsViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    TripsScreen(uiState = uiState, onAddTripClick = viewModel::onAddTripClick)
}

/** 상태만 받아 그리는 쪽(stateless) → Preview·UI 테스트가 쉬워진다 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun TripsScreen(
    uiState: TripsUiState,
    onAddTripClick: () -> Unit,
) {
    Scaffold(
        topBar = { TopAppBar(title = { Text("트립보드") }) },
        floatingActionButton = {
            FloatingActionButton(onClick = onAddTripClick) { Text("+") }
        },
    ) { padding ->
        when (uiState) {
            TripsUiState.Loading -> Centered(padding) { CircularProgressIndicator() }
            TripsUiState.Empty -> Centered(padding) { Text("아직 여행이 없어요") }
            is TripsUiState.Success -> LazyColumn(contentPadding = padding) {
                items(uiState.trips, key = { it.id }) { trip ->
                    ListItem(headlineContent = { Text(trip.title) })
                }
            }
        }
    }
}

@Composable
private fun Centered(padding: PaddingValues, content: @Composable () -> Unit) {
    Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) { content() }
}

@Preview
@Composable
private fun TripsScreenPreview() {
    TripBoardTheme(dynamicColor = false) {
        TripsScreen(
            uiState = TripsUiState.Success(listOf(Trip("1", "도쿄 3박 4일", 0L))),
            onAddTripClick = {},
        )
    }
}
