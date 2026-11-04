package com.tw0winju.tripboard.feature.trips

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tw0winju.tripboard.core.data.repository.TripRepository
import com.tw0winju.tripboard.core.model.Trip
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface TripsUiState {
    data object Loading : TripsUiState
    data object Empty : TripsUiState
    data class Success(val trips: List<Trip>) : TripsUiState
}

/**
 * MVP의 Presenter + View 인터페이스 대신, 상태(StateFlow) 하나를 노출하고 화면은 그걸 그리기만 한다.
 */
@HiltViewModel
class TripsViewModel @Inject constructor(
    private val tripRepository: TripRepository,
) : ViewModel() {

    val uiState: StateFlow<TripsUiState> = tripRepository.observeTrips()
        .map { trips -> if (trips.isEmpty()) TripsUiState.Empty else TripsUiState.Success(trips) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = TripsUiState.Loading,
        )

    fun onAddTripClick() {
        viewModelScope.launch {
            tripRepository.createTrip(title = "새 여행")
        }
    }
}
