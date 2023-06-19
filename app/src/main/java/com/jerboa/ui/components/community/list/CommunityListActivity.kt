package com.jerboa.ui.components.community.list

import android.util.Log
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import com.jerboa.DEBOUNCE_DELAY
import com.jerboa.api.ApiState
import com.jerboa.datatypes.types.GetSiteResponse
import com.jerboa.datatypes.types.Search
import com.jerboa.datatypes.types.SearchType
import com.jerboa.datatypes.types.SortType
import com.jerboa.db.AccountViewModel
import com.jerboa.db.AppSettingsViewModel
import com.jerboa.nav.initializeOnce
import com.jerboa.ui.components.common.ApiEmptyText
import com.jerboa.ui.components.common.ApiErrorText
import com.jerboa.ui.components.common.LoadingBar
import com.jerboa.ui.components.common.getCurrentAccount
import com.jerboa.ui.components.home.SiteViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun CommunityListActivity(
    navController: CommunityListNavController,
    followedCommunitiesViewModel: FollowedCommunitiesViewModel,
    onSelectCommunity: OnSelectCommunity?
) {
    Log.d("jerboa", "got to community list activity")

    val communityListViewModel: CommunityListViewModel = hiltViewModel()
    LaunchedEffect(followedCommunitiesViewModel) {
        communityListViewModel.followedCommunitiesViewModel = followedCommunitiesViewModel
    }

    Surface(color = MaterialTheme.colorScheme.background) {
        Scaffold(
            topBar = {
                CommunityListHeader(
                    navController = navController,
                    searchFlow = communityListViewModel.searchFlow,
                )
            },
            content = { padding ->
                when (val communitiesRes = communityListViewModel.searchRes) {
                    ApiState.Empty -> ApiEmptyText()
                    is ApiState.Failure -> ApiErrorText(communitiesRes.msg)
                    ApiState.Loading -> {
                        LoadingBar(padding)
                    }

                    is ApiState.Success -> {
                        CommunityListings(
                            communities = communitiesRes.data.communities,
                            onClickCommunity = onSelectCommunity ?: { cs ->
                                navController.toCommunity.navigate(cs.id)
                            },
                            modifier = Modifier
                                .padding(padding)
                                .imePadding(),
                        )
                    }
                }
            }
        )
    }
}
