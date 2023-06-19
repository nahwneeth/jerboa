package com.jerboa.ui.components.common

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import com.jerboa.PostViewMode
import com.jerboa.db.Account
import com.jerboa.db.AccountViewModel
import com.jerboa.db.AppSettings
import com.jerboa.db.AppSettingsViewModel

@Composable
fun getCurrentAccount(accountViewModel: AccountViewModel): Account? {
    val accounts by accountViewModel.allAccounts.observeAsState()
    return getCurrentAccount(accounts)
}

fun getCurrentAccountSync(accountViewModel: AccountViewModel): Account? {
    val accounts = accountViewModel.allAccountSync
    return getCurrentAccount(accounts)
}

fun getCurrentAccount(accounts: List<Account>?): Account? {
    return accounts?.firstOrNull { it.current }
}

fun getPostViewMode(appSettingsViewModel: AppSettingsViewModel): PostViewMode {
    return PostViewMode.values()[appSettingsViewModel.appSettings.value?.postViewMode ?: 0]
}

fun AppSettings?.postViewMode() = PostViewMode.values()[this?.postViewMode ?: 0]
