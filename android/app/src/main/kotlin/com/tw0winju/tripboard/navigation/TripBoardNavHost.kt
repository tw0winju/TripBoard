package com.tw0winju.tripboard.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.rememberNavController
import com.tw0winju.tripboard.feature.trips.navigation.TripsDestination
import com.tw0winju.tripboard.feature.trips.navigation.tripsScreen

@Composable
fun TripBoardNavHost(
    navController: NavHostController = rememberNavController(),
) {
    NavHost(navController = navController, startDestination = TripsDestination) {
        tripsScreen()
    }
}
