@file:OptIn(ExperimentalMaterial3Api::class)

package com.jerboa.ui.components.login

import android.util.Log
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.jerboa.db.AccountViewModel
import com.jerboa.ui.components.home.HomeViewModel
import com.jerboa.ui.components.home.SiteViewModel

@Composable
fun LoginActivity(
    accountViewModel: AccountViewModel,
    siteViewModel: SiteViewModel,
    navController: LoginNavController,
) {
    Log.d("jerboa", "Got to login activity")

    val loginViewModel : LoginViewModel = viewModel()

    val snackbarHostState = remember { SnackbarHostState() }
    val accounts by accountViewModel.allAccounts.observeAsState()
    val ctx = LocalContext.current

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            LoginHeader(
                navController = navController,
                accounts = accounts,
            )
        },
        content = { padding ->
            LoginForm(
                loading = loginViewModel.loading,
                modifier = Modifier
                    .padding(padding)
                    .imePadding(),
                onClickLogin = { form, instance ->
                    loginViewModel.login(
                        form = form,
                        instance = instance.trim(),
                        ctx = ctx,
                        accountViewModel = accountViewModel,
                        siteViewModel = siteViewModel,
                    ) {
                        navController.toHome.navigate()
                    }
                },
            )
        },
    )
}
