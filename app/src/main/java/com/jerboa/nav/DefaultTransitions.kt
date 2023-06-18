package com.jerboa.nav

import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally

val enterTransition = slideInHorizontally(tween(300)) { it }
val exitTransition = slideOutHorizontally(tween(300)) { -it }
val popEnterTransition = slideInHorizontally(tween(300)) { -it }
val popExitTransition = slideOutHorizontally(tween(300)) { it }
