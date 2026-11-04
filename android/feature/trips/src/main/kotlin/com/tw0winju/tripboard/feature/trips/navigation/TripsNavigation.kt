package com.tw0winju.tripboard.feature.trips.navigation

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.tw0winju.tripboard.feature.trips.TripsRoute
import kotlinx.serialization.Serializable

/** Navigation 2.8+ 타입 세이프 라우트 (문자열 route 대신 @Serializable 객체) */
@Serializable
data object TripsDestination

fun NavGraphBuilder.tripsScreen() {
    composable<TripsDestination> {
        TripsRoute()
    }
}
