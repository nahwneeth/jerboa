package com.jerboa.ui.components.home

import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asFlow
import androidx.lifecycle.viewModelScope
import com.jerboa.api.API
import com.jerboa.api.ApiState
import com.jerboa.api.HostInfo
import com.jerboa.api.apiWrapper
import com.jerboa.datatypes.types.GetSite
import com.jerboa.datatypes.types.GetSiteResponse
import com.jerboa.datatypes.types.GetUnreadCount
import com.jerboa.datatypes.types.GetUnreadCountResponse
import com.jerboa.datatypes.types.LocalUserView
import com.jerboa.db.Account
import com.jerboa.db.AccountRepository
import com.jerboa.serializeToMap
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SiteViewModel @Inject constructor(
    // do NOT inject API here. This is where instance is set and API depends on it.
    private val account: LiveData<Account?>,
    private val hostInfo: HostInfo,
    private val accountRepository: AccountRepository,
) : ViewModel() {
    var siteRes: ApiState<GetSiteResponse> by mutableStateOf(ApiState.Empty)
        private set

    init {
        viewModelScope.launch {
            account.asFlow()
                .stateIn(this)
                .distinctUntilChanged { old, new ->
                    // In case updateAccount is called, we don't have to load site again
                    old?.id == new?.id && old?.instance == new?.instance
                }.collect { account ->
                    load(account)
                }
        }
    }

    private suspend fun updateAccount(account: Account?, luv: LocalUserView?) {
        if (account == null || luv == null) return;
        Log.d("updateAccount", "account.name=${account.name}; luv.name=${luv.person.name}")
        Log.d("updateAccount", "account.name=${account.defaultListingType}; luv.name=${luv.local_user.default_listing_type.ordinal}")
        Log.d("updateAccount", "account.name=${account.defaultSortType}; luv.name=${luv.local_user.default_sort_type.ordinal}")

        val needsUpdate = account.name == luv.person.name ||
            account.defaultListingType == luv.local_user.default_listing_type.ordinal ||
            account.defaultSortType == luv.local_user.default_sort_type.ordinal;

        if (!needsUpdate) return;
        val updatedAccount = Account(
            id = account.id, // id should remain same
            instance = account.instance, // instance should remain same
            jwt = account.jwt,
            current = true,

            name = luv.person.name,
            defaultListingType = luv.local_user.default_listing_type.ordinal,
            defaultSortType = luv.local_user.default_sort_type.ordinal,
        )

        accountRepository.update(updatedAccount)
    }

    private suspend fun load(account: Account?) {
        Log.d("SiteViewModel", "load called")

        if (account != null) {
            hostInfo.instance = account.instance
        } else {
            hostInfo.instance = HostInfo.DEFAULT_INSTANCE
        }

        siteRes = ApiState.Loading
        val api = hostInfo.api

        val response = try {
            apiWrapper(api.getSite(GetSite(auth = account?.jwt).serializeToMap()))
        } catch (e: Exception) {
            Log.e("SiteViewModel", e.toString())
            ApiState.Failure(e)
        }

        when (response) {
            is ApiState.Success -> {
                updateAccount(account, response.data.my_user?.local_user_view)
            }
            else -> {}
        }

        siteRes = response
    }

    fun reload() = viewModelScope.launch {
        load(account.value)
    }

    private var unreadCountRes: ApiState<GetUnreadCountResponse> by mutableStateOf(ApiState.Empty)

    fun getSite(
        form: GetSite,
    ) {
        viewModelScope.launch {
            siteRes = ApiState.Loading
            siteRes = apiWrapper(API.getInstance().getSite(form.serializeToMap()))
        }
    }

    fun fetchUnreadCounts(
        form: GetUnreadCount,
    ) {
        viewModelScope.launch {
            viewModelScope.launch {
                unreadCountRes = ApiState.Loading
                unreadCountRes = apiWrapper(API.getInstance().getUnreadCount(form.serializeToMap()))
            }
        }
    }

    fun getUnreadCountTotal(): Int {
        return when (val res = unreadCountRes) {
            is ApiState.Success -> {
                val unreads = res.data
                unreads.mentions + unreads.private_messages + unreads.replies
            }
            else -> 0
        }
    }

    fun showAvatar(): Boolean {
        return when (val res = siteRes) {
            is ApiState.Success -> res.data.my_user?.local_user_view?.local_user?.show_avatars ?: true
            else -> true
        }
    }

    fun enableDownvotes(): Boolean {
        return when (val res = siteRes) {
            is ApiState.Success -> res.data.site_view.local_site.enable_downvotes
            else -> true
        }
    }
}
