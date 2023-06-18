@file:OptIn(ExperimentalMaterial3Api::class)

package com.jerboa.ui.components.login

import android.util.Log
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import com.jerboa.R
import com.jerboa.ui.theme.MEDIUM_PADDING

@Composable
fun LoginActivity(navController: LoginNavController) {
    Log.d("jerboa", "Got to login activity")

    val loginViewModel : LoginViewModel = hiltViewModel()
    val accounts by loginViewModel.accounts.observeAsState()

    val snackbarHostState = remember { SnackbarHostState() }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            LoginHeader(
                navController = navController,
                accounts = accounts,
            )
        },
        content = { padding ->
            Box(modifier = Modifier
                .padding(padding)
                .imePadding()) {
                Text(
                    text = when (val error = loginViewModel.error) {
                        is LoginError.NotALemmyInstance -> stringResource(
                            R.string.login_view_model_is_not_a_lemmy_instance,
                            error.instance,
                        )
                        is LoginError.IncorrectLogin -> stringResource(
                            R.string.login_view_model_incorrect_login
                        )
                        is LoginError.FailedLoadingUserData -> {
                            stringResource(R.string.error_retrieving_user_info)
                        }
                        else -> ""
                    },
                    modifier = Modifier.padding(MEDIUM_PADDING)
                )
                LoginForm(
                    loading = loginViewModel.loading,
                    modifier = Modifier,
                    onClickLogin = { form, instance ->
                        loginViewModel.login(
                            form = form,
                            instance = instance.trim(),
                        )
                    },
                )
            }
        },
    )
}
