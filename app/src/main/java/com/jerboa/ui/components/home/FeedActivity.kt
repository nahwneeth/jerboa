package com.jerboa.ui.components.home

import android.content.Context
import android.util.Log
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.pullrefresh.PullRefreshIndicator
import androidx.compose.material.pullrefresh.pullRefresh
import androidx.compose.material.pullrefresh.rememberPullRefreshState
import androidx.compose.material3.DrawerState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FabPosition
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import com.jerboa.CommunityBlockedSnackbarEffect
import com.jerboa.PersonBlockedSnackbarEffect
import com.jerboa.PostViewMode
import com.jerboa.R
import com.jerboa.VoteType
import com.jerboa.api.ApiState
import com.jerboa.closeDrawer
import com.jerboa.datatypes.types.BlockCommunity
import com.jerboa.datatypes.types.BlockPerson
import com.jerboa.datatypes.types.CreatePostLike
import com.jerboa.datatypes.types.DeletePost
import com.jerboa.datatypes.types.GetPosts
import com.jerboa.datatypes.types.GetSiteResponse
import com.jerboa.datatypes.types.ListingType
import com.jerboa.datatypes.types.SavePost
import com.jerboa.datatypes.types.SortType
import com.jerboa.db.Account
import com.jerboa.db.AccountViewModel
import com.jerboa.db.AppSettings
import com.jerboa.db.AppSettingsViewModel
import com.jerboa.newVote
import com.jerboa.scrollToTop
import com.jerboa.ui.components.common.ApiEmptyText
import com.jerboa.ui.components.common.ApiErrorText
import com.jerboa.ui.components.common.LoadingBar
import com.jerboa.ui.components.common.getCurrentAccount
import com.jerboa.ui.components.common.getPostViewMode
import com.jerboa.ui.components.common.postViewMode
import com.jerboa.ui.components.post.PostListings
import com.jerboa.ui.components.post.create.CreatePostDependencies
import com.jerboa.ui.components.post.edit.PostEditDependencies
import com.jerboa.ui.components.settings.account.AccountSettingsViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FeedActivity(
    navController: FeedNavController,
    homeViewModel: HomeViewModel,
    siteRes: GetSiteResponse,
    appSettingsViewModel: AppSettingsViewModel,
    drawerState: DrawerState,
) {
    Log.d("jerboa", "got to home activity")

    val postListState = rememberLazyListState()
    val snackbarHostState = remember { SnackbarHostState() }
    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior(rememberTopAppBarState())

    val appSettings by appSettingsViewModel.appSettings.observeAsState()
    val showVotingArrowsInListView = appSettings?.showVotingArrowsInListView ?: true

    val account by homeViewModel.account.observeAsState()

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            MainTopBar(
                postListState = postListState,
                drawerState = drawerState,
                homeViewModel = homeViewModel,
                navController = navController,
                scrollBehavior = scrollBehavior,
            )
        },
        content = { padding ->
            MainPostListingsContent(
                padding = padding,
                homeViewModel = homeViewModel,
                siteRes = siteRes,
                appSettingsViewModel = appSettingsViewModel,
                navController = navController,
                postListState = postListState,
                showVotingArrowsInListView = showVotingArrowsInListView,
            )
        },
        floatingActionButtonPosition = FabPosition.End,
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    account?.also {
                        navController.toCreatePost.navigate(
                            CreatePostDependencies(selectedCommunity = null),
                        )
                    } ?: run {
                        navController.toLogin.navigate()
                    }
                },
            ) {
                Icon(
                    imageVector = Icons.Outlined.Add,
                    contentDescription = stringResource(R.string.floating_createPost),
                )
            }
        },
    )
}

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun MainPostListingsContent(
    siteRes: GetSiteResponse,
    homeViewModel: HomeViewModel,
    navController: FeedNavController,
    padding: PaddingValues,
    postListState: LazyListState,
    appSettingsViewModel: AppSettingsViewModel,
    showVotingArrowsInListView: Boolean,
) {
    // TODO can be removed with 0.18.0 release
    if (siteRes.taglines !== null) {
        Taglines(siteRes.taglines)
    }

    val loading = homeViewModel.fetchingMore is ApiState.Loading
    val pullRefreshState = rememberPullRefreshState(
        refreshing = loading,
        onRefresh = homeViewModel::reload,
    )

    val account by homeViewModel.account.observeAsState()

    Box(modifier = Modifier.pullRefresh(pullRefreshState)) {
        PullRefreshIndicator(loading, pullRefreshState, Modifier.align(Alignment.TopCenter))
        // Can't be in ApiState.Loading, because of infinite scrolling
        if (loading) {
            LoadingBar(padding = padding)
        }
        PostListings(
            listState = postListState,
            padding = padding,
            posts = homeViewModel.postsRes,
            postViewMode = getPostViewMode(appSettingsViewModel),
            onUpvoteClick = { postView ->
                Log.d("HomeViewModel", "onUpvoteClick")
                val loggedIn = homeViewModel.likePost(
                    postView= postView,
                    voteType = VoteType.Upvote
                )
                if (!loggedIn) navController.toLogin.navigate()
            },
            onDownvoteClick = { postView ->
                val loggedIn = homeViewModel.likePost(
                    postView = postView,
                    voteType = VoteType.Downvote
                )
                if (!loggedIn) navController.toLogin.navigate()
            },
            onPostClick = { postView ->
                navController.toPost.navigate(postView.post.id)
            },
            onSaveClick = { postView ->
                val loggedIn = homeViewModel.savePost(postView)
                if (!loggedIn) navController.toLogin.navigate()
            },
            onBlockCommunityClick = { community ->
                val loggedIn = homeViewModel.blockCommunity(community)
                if (!loggedIn) navController.toLogin.navigate()
            },
            onBlockCreatorClick = { creator ->
                val loggedIn = homeViewModel.blockPerson(creator)
                if (!loggedIn) navController.toLogin.navigate()
            },
            onEditPostClick = { postView ->
                navController.toPostEdit.navigate(
                    PostEditDependencies(
                        postView = postView,
                        onPostEdit = homeViewModel::updatePost,
                    )
                )
            },
            onDeletePostClick = { postView ->
                val loggedIn = homeViewModel.deletePost(postView)
                if (!loggedIn) navController.toLogin.navigate()
            },
            onReportClick = { postView ->
                navController.toPostReport.navigate(postView.post.id)
            },
            onCommunityClick = { community ->
                navController.toCommunity.navigate(community.id)
            },
            onPersonClick = { personId ->
                navController.toProfile.navigate(personId)
            },
            isScrolledToEnd = homeViewModel::nextPage,
            account = account,
            enableDownVotes = siteRes.enableDownvotes(),
            showAvatar = siteRes.showAvatar(),
            showVotingArrowsInListView = showVotingArrowsInListView,
        )
    }
}

@Composable
fun MainDrawer(
    siteRes: GetSiteResponse,
    navController: FeedNavController,
    bottomNavController: BottomNavController,
    homeViewModel: HomeViewModel,
    drawerState: DrawerState,
) {
    val accountViewModel: AccountViewModel = hiltViewModel()
    val account = getCurrentAccount(accountViewModel)

    val scope = rememberCoroutineScope()

    Drawer(
        siteRes = siteRes,
        unreadCount = homeViewModel.unreadCountRes.totalUnreadCount(),
        accountViewModel = accountViewModel,
        isOpen = drawerState.isOpen,
        onAddAccountClick = { navController.toLogin.navigate() },
        onSwitchAccountClick = { acct ->
            accountViewModel.removeCurrent()
            accountViewModel.setCurrent(acct.id)
            closeDrawer(scope, drawerState)
        },
        onSignOutClick = {
            account?.let(accountViewModel::delete)
            closeDrawer(scope, drawerState)
        },
        onClickListingType = { listingType ->
            homeViewModel.withType(listingType)
            closeDrawer(scope, drawerState)
        },
        onCommunityClick = { community ->
            navController.toCommunity.navigate(community.id)
            closeDrawer(scope, drawerState)
        },
        onClickProfile = {
            account?.id?.also {
                bottomNavController.toProfile.navigate()
                closeDrawer(scope, drawerState)
            }
        },
        onClickSaved = {
            account?.id?.also {
                bottomNavController.toSaved.navigate()
                closeDrawer(scope, drawerState)
            }
        },
        onClickInbox = {
            account?.also {
                bottomNavController.toInbox.navigate()
            } ?: run {
                navController.toLogin.navigate()
            }
            closeDrawer(scope, drawerState)
        },
        onClickSettings = {
            navController.toSettings.navigate()
            closeDrawer(scope, drawerState)
        },
        onClickCommunities = {
            bottomNavController.toSearch.navigate()
            closeDrawer(scope, drawerState)
        },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainTopBar(
    postListState: LazyListState,
    drawerState: DrawerState,
    homeViewModel: HomeViewModel,
    navController: FeedNavController,
    scrollBehavior: TopAppBarScrollBehavior,
) {
    val appSettingsViewModel: AppSettingsViewModel = hiltViewModel()
    val appSettings by appSettingsViewModel.appSettings.observeAsState()

    val filter by homeViewModel.filter.collectAsState()
    val scope = rememberCoroutineScope()

    Column {
        HomeHeader(
            scrollBehavior = scrollBehavior,
            drawerState = drawerState,
            selectedSortType = filter.sort,
            selectedListingType = filter.type,
            selectedPostViewMode = appSettings.postViewMode(),
            onClickSortType = { sortType ->
                scrollToTop(scope, postListState)
                homeViewModel.withSort(sortType)
            },
            onClickListingType = { listingType ->
                scrollToTop(scope, postListState)
                homeViewModel.withType(listingType)
            },
            onClickPostViewMode = {
                appSettingsViewModel.updatedPostViewMode(it.ordinal)
            },
            onClickRefresh = {
                scrollToTop(scope, postListState)
                homeViewModel.reload()
            },
            onClickShowSiteInfo = { navController.toSiteSideBar.navigate() }
        )
    }
}
