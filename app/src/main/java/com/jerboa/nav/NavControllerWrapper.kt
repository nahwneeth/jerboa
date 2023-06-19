package com.jerboa.nav

import androidx.navigation.NavController
import kotlinx.coroutines.flow.map

abstract class NavControllerWrapper {
    protected abstract val navController: NavController

    val canPop by lazy {
        navController.currentBackStackEntryFlow.map {
            it.destination.parent?.route != null
        }
    }

    fun navigateUp() = navController.navigateUp()
}
