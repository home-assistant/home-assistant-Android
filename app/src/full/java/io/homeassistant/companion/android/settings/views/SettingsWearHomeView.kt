package io.homeassistant.companion.android.settings.views

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.google.android.material.composethemeadapter.MdcTheme
import io.homeassistant.companion.android.settings.SettingsWearViewModel

@Composable
fun LoadSettingsHomeView(
    settingsWearViewModel: SettingsWearViewModel,
    deviceName: String,
    loginWearOs: ()->Unit
) {
    MdcTheme {
        val navController = rememberNavController()
        NavHost(navController = navController, startDestination = SettingsWearMainView.LANDING) {
            composable(SettingsWearMainView.FAVORITES) {
                LoadWearFavoritesSettings(
                    settingsWearViewModel
                )
            }
            composable(SettingsWearMainView.LANDING) {
                SettingWearLandingView(
                    deviceName = deviceName,
                    isAuthed = settingsWearViewModel.isAuthenticated.value,
                    navigateFavorites = { navController.navigate(SettingsWearMainView.FAVORITES) },
                    loginWearOs = loginWearOs
                )
            }
        }
    }
}
