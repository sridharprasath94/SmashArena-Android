package com.flash.smasharena

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.smasharena.AppContainer
import com.smasharena.SmashArenaApplication
import com.smasharena.ui.booking.BookingScreen
import com.smasharena.ui.courts.CourtsScreen
import com.smasharena.ui.login.LoginScreen
import com.smasharena.ui.mybookings.MyBookingsScreen
import com.smasharena.ui.theme.SmashArenaTheme
import com.smasharena.viewmodel.BookingViewModel
import com.smasharena.viewmodel.CourtsViewModel
import com.smasharena.viewmodel.MyBookingsViewModel
import com.smasharena.viewmodel.SessionViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val container = (application as SmashArenaApplication).container
        setContent {
            SmashArenaTheme {
                AppNavHost(container)
            }
        }
    }
}

private object Routes {
    const val LOGIN = "login"
    const val COURTS = "courts"
    const val BOOK = "book/{courtId}"
    const val MY_BOOKINGS = "my-bookings"
    fun book(courtId: Long) = "book/$courtId"
}

@Composable
private fun AppNavHost(container: AppContainer) {
    val nav: NavHostController = rememberNavController()
    val sessionVm: SessionViewModel = viewModel(
        factory = SessionViewModel.Factory(container.bookingRepository),
    )
    val user by sessionVm.currentUser.collectAsStateWithLifecycle()

    NavHost(
        navController = nav,
        startDestination = if (user == null) Routes.LOGIN else Routes.COURTS,
    ) {
        composable(Routes.LOGIN) {
            LoginScreen(onSignIn = { name, premium ->
                sessionVm.signIn(name, premium)
                nav.navigate(Routes.COURTS) {
                    popUpTo(Routes.LOGIN) { inclusive = true }
                }
            })
        }

        composable(Routes.COURTS) {
            CourtsScreen(
                factory = CourtsViewModel.Factory(container.bookingRepository),
                onCourtSelected = { id -> nav.navigate(Routes.book(id)) },
                onMyBookings = { nav.navigate(Routes.MY_BOOKINGS) },
            )
        }

        composable(Routes.BOOK) { backStack ->
            val id = backStack.arguments?.getString("courtId")?.toLongOrNull() ?: return@composable
            val u = user ?: return@composable
            BookingScreen(
                courtId = id,
                currentUser = u,
                factory = BookingViewModel.Factory(container.bookingRepository),
                onBooked = {
                    nav.navigate(Routes.MY_BOOKINGS) {
                        popUpTo(Routes.COURTS)
                    }
                },
            )
        }

        composable(Routes.MY_BOOKINGS) {
            val u = user ?: return@composable
            MyBookingsScreen(
                userId = u.id,
                factory = MyBookingsViewModel.Factory(container.bookingRepository),
            )
        }
    }
}
