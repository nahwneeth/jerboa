package com.jerboa.ui.components.settings.account

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.jerboa.api.API
import com.jerboa.api.ApiState
import com.jerboa.api.apiWrapper
import com.jerboa.datatypes.types.GetSite
import com.jerboa.datatypes.types.ListingType
import com.jerboa.datatypes.types.LoginResponse
import com.jerboa.datatypes.types.SaveUserSettings
import com.jerboa.datatypes.types.SortType
import com.jerboa.db.Account
import com.jerboa.db.AccountRepository
import com.jerboa.ui.components.home.SiteViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AccountSettingsViewModel @Inject constructor(
    private val accountRepository: AccountRepository,
) : ViewModel() {
    var saveUserSettingsRes: ApiState<LoginResponse> by mutableStateOf(ApiState.Empty)
        private set

    fun saveSettings(
        form: SaveUserSettings,
        siteViewModel: SiteViewModel,
        account: Account,
    ) {
        viewModelScope.launch {
            saveUserSettingsRes = ApiState.Loading
            saveUserSettingsRes = apiWrapper(API.getInstance().saveUserSettings(form))

            siteViewModel.getSite(
                GetSite(auth = account.jwt),
            )

//            val newAccount = async { maybeUpdateAccountSettings(account, form) }.await()

//            siteViewModel.updateFromAccount(newAccount)
        }
    }

//     TODO Where is this used??
    private suspend fun maybeUpdateAccountSettings(account: Account, form: SaveUserSettings): Account {
        val newAccount = account.copy(
            defaultListingType = form.default_listing_type?.ordinal ?: account.defaultListingType,
            defaultSortType = form.default_sort_type?.ordinal ?: account.defaultSortType,
        )
        if (newAccount != account) {
            accountRepository.update(newAccount)
        }
        return newAccount
    }
}

fun Account?.defaultSortType(): SortType {
    return this?.defaultSortType?.let(SortType.values()::getOrNull) ?: SortType.Active
}

fun Account?.defaultListingType(): ListingType {
    return this?.defaultSortType?.let(ListingType.values()::getOrNull) ?: ListingType.Local
}
