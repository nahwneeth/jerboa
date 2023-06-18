package com.jerboa.ui.components.common

import androidx.compose.material.IconButton
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import com.jerboa.R
import com.jerboa.nav.NavControllerWrapper

@Composable
fun DefaultBackButton(navController: NavControllerWrapper) {
    val canPop = navController.canPop.collectAsState(false)
    var clicked by remember { mutableStateOf(false) }

    if (canPop.value || clicked) {
        IconButton(onClick = {
            if (!clicked) navController.navigateUp()
            clicked = true
        }) {
            Icon(
                imageVector = Icons.Outlined.ArrowBack,
                contentDescription = stringResource(R.string.topAppBar_back),
            )
        }
    }
}
