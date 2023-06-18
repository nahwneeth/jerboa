package com.jerboa.ui.components.login

import android.content.Context
import android.util.Log
import android.widget.Toast
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jerboa.api.ApiState
import com.jerboa.api.HostInfo
import com.jerboa.api.apiWrapper
import com.jerboa.api.retrofitErrorHandler
import com.jerboa.datatypes.types.GetPosts
import com.jerboa.datatypes.types.GetSite
import com.jerboa.datatypes.types.ListingType
import com.jerboa.datatypes.types.Login
import com.jerboa.datatypes.types.SortType
import com.jerboa.db.Account
import com.jerboa.db.AccountRepository
import com.jerboa.db.AccountViewModel
import com.jerboa.serializeToMap
import com.jerboa.ui.components.home.HomeViewModel
import com.jerboa.ui.components.home.SiteViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import javax.inject.Inject

interface LoginError {
    class NotALemmyInstance(val instance: String): LoginError

    class IncorrectLogin: LoginError

    class FailedLoadingUserData: LoginError
}

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val accountRepository: AccountRepository,
) : ViewModel() {
    val accounts = accountRepository.allAccounts

    var loading by mutableStateOf(false)
        private set

    var error by mutableStateOf<LoginError?>(null)
        private set

    fun login(instance: String, form: Login) {
        val api = HostInfo(instance).api

        loading = true
        error = null

        viewModelScope.launch {
            val jwt = try {
                // TODO this needs to be checked,
                retrofitErrorHandler(api.login(form = form)).jwt!!
            } catch (e: Exception) {
                Log.e("login", e.toString())
                error = when (e) {
                    is java.net.UnknownHostException -> LoginError.NotALemmyInstance(instance)
                    else -> LoginError.IncorrectLogin()
                }
                return@launch
            }

            // Fetch the site to get your name and id
            // Can't do a co-routine within a co-routine
            when (val siteRes = apiWrapper(api.getSite(GetSite(auth = jwt).serializeToMap()))) {
                is ApiState.Failure -> LoginError.FailedLoadingUserData()
                is ApiState.Success -> {
                    val luv = siteRes.data.my_user!!.local_user_view
                    val account = Account(
                        // TODO(nahwneeth): Check if the id is unique across instances.
                        id = luv.person.id,
                        name = luv.person.name,
                        current = true,
                        instance = instance,
                        jwt = jwt,
                        defaultListingType = luv.local_user.default_listing_type.ordinal,
                        defaultSortType = luv.local_user.default_sort_type.ordinal,
                    )

                    // Remove the default account
                    accountRepository.removeCurrent()

                    // Save that info in the DB
                    accountRepository.insert(account)
                }
                else -> {}
            }
        }

        loading = false
    }
}
