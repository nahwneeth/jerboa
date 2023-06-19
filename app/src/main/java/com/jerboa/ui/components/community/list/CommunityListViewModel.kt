package com.jerboa.ui.components.community.list

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jerboa.DEBOUNCE_DELAY
import com.jerboa.api.API
import com.jerboa.api.ApiState
import com.jerboa.api.apiWrapper
import com.jerboa.datatypes.types.Community
import com.jerboa.datatypes.types.CommunityAggregates
import com.jerboa.datatypes.types.CommunityView
import com.jerboa.datatypes.types.GetSiteResponse
import com.jerboa.datatypes.types.Search
import com.jerboa.datatypes.types.SearchResponse
import com.jerboa.datatypes.types.SearchType
import com.jerboa.datatypes.types.SortType
import com.jerboa.datatypes.types.SubscribedType
import com.jerboa.db.Account
import com.jerboa.serializeToMap
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

class FollowedCommunitiesViewModel: ViewModel() {
    var communities: Set<Community> = setOf()
        private set

    fun addFollowing(siteRes: GetSiteResponse) {
        siteRes.my_user?.follows?.also {
            communities.union(it)
        }
    }
}

@HiltViewModel
class CommunityListViewModel @Inject constructor(
    private val api: API,
    private val account: LiveData<Account?>,
) : ViewModel() {
    var searchRes: ApiState<SearchResponse> by mutableStateOf(ApiState.Empty)
        private set

    var followedCommunitiesViewModel: FollowedCommunitiesViewModel? = null
        set(value) {
            field = value
            if (searchFlow.value?.isEmpty() == true) {
                searchRes = followedCommunitiesSearchRes(value?.communities)
            }
        }

    val searchFlow = MutableStateFlow("")

    init {
        viewModelScope.launch {
            searchFlow.collect { search ->
                searchCommunities(search)
            }
        }
    }

    private var fetchCommunitiesJob: Job? = null

    private fun searchCommunities(search: String) {
        fetchCommunitiesJob?.cancel()

        if (search.isEmpty()) {
            val communities = followedCommunitiesViewModel?.communities
            searchRes = followedCommunitiesSearchRes(communities)
            return
        }

        fetchCommunitiesJob = viewModelScope.launch {
            delay(DEBOUNCE_DELAY)
            searchRes = ApiState.Loading
            searchRes = apiWrapper(
                api.search(
                    Search(
                        q = search,
                        type_ = SearchType.Communities,
                        sort = SortType.TopAll,
                        auth = account.value?.jwt,
                    ).serializeToMap()
                )
            )
        }
    }

    private fun followedCommunitiesSearchRes(communities: Set<Community>?): ApiState<SearchResponse> {
        val followsIntoCommunityViews = (communities ?: setOf())
            .sortedBy { it.name }
            .map { community ->
                CommunityView(
                    community = community,
                    subscribed = SubscribedType.Subscribed,
                    blocked = false,
                    counts = CommunityAggregates(
                        id = 0,
                        community_id = community.id,
                        subscribers = 0,
                        posts = 0,
                        comments = 0,
                        published = "",
                        users_active_day = 0,
                        users_active_week = 0,
                        users_active_month = 0,
                        users_active_half_year = 0,
                    ),
                )
            }

        return ApiState.Success(
            SearchResponse(
                type_ = SearchType.Communities,
                communities = followsIntoCommunityViews,
                comments = emptyList(),
                posts = emptyList(),
                users = emptyList(),
            ),
        )
    }
}
