package com.jerboa.ui.components.home

import android.app.Activity
import android.util.Log
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmarks
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.Bookmarks
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDrawerState
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import arrow.core.Either
import com.jerboa.CommunityBlockedSnackbarEffect
import com.jerboa.PersonBlockedSnackbarEffect
import com.jerboa.R
import com.jerboa.api.ApiState
import com.jerboa.datatypes.types.GetSiteResponse
import com.jerboa.datatypes.types.GetUnreadCountResponse
import com.jerboa.db.AppSettingsViewModel
import com.jerboa.nav.Route
import com.jerboa.ui.components.common.InboxIconAndBadge
import com.jerboa.ui.components.community.list.CommunityListActivity
import com.jerboa.ui.components.community.list.CommunityListNavController
import com.jerboa.ui.components.community.list.FollowedCommunitiesViewModel
import com.jerboa.ui.components.inbox.InboxActivity
import com.jerboa.ui.components.inbox.InboxNavController
import com.jerboa.ui.components.person.PersonProfileActivity
import com.jerboa.ui.components.person.PersonProfileNavController

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeActivity(
    selectTabArg: HomeTab,
    siteRes: GetSiteResponse,
    followedCommunitiesViewModel: FollowedCommunitiesViewModel,
    appSettingsViewModel: AppSettingsViewModel,
    feedNavController: FeedNavController,
    communityListNavController: CommunityListNavController,
    inboxNavController: InboxNavController,
    savedAndProfileNavController: PersonProfileNavController,
) {
    Log.d("jerboa", "got to home activity")

    val snackbarHostState = remember { SnackbarHostState() }
    val drawerState = rememberDrawerState(DrawerValue.Closed)

    val homeViewModel: HomeViewModel = hiltViewModel()
    CommunityBlockedSnackbarEffect(homeViewModel.blockCommunityRes, snackbarHostState)
    PersonBlockedSnackbarEffect(homeViewModel.blockPersonRes, snackbarHostState)
    LaunchedEffect(homeViewModel.unreadCountRes) {
        when (homeViewModel.unreadCountRes) {
            is ApiState.Failure -> homeViewModel.reloadUnreadCounts()
            else -> {}
        }
    }

    val account by homeViewModel.account.observeAsState()

    val bottomNavController = rememberNavController()
    var selectedTab by rememberSaveable {
        mutableStateOf(
            if (selectTabArg.needsLogin() && account == null) {
                Route.HomeArgs.TAB_DEFAULT
            } else {
                selectTabArg
            }
        )
    }

    val onSelectTab = { tab: HomeTab ->
        if (tab.needsLogin() && account == null) {
            feedNavController.toLogin.navigate()
        } else {
            selectedTab = tab
            bottomNavController.navigate(tab.name) {
                popUpTo(0)
            }
        }
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(
                content = {
                    MainDrawer(
                        siteRes = siteRes,
                        homeViewModel = homeViewModel,
                        drawerState = drawerState,
                        navController = feedNavController,
                        bottomNavController = BottomNavController(
                            toFeed = BottomNavigation { onSelectTab(HomeTab.Feed) },
                            toSearch = BottomNavigation { onSelectTab(HomeTab.Search) },
                            toInbox = BottomNavigation { onSelectTab(HomeTab.Inbox) },
                            toSaved = BottomNavigation { onSelectTab(HomeTab.Saved) },
                            toProfile = BottomNavigation { onSelectTab(HomeTab.Profile) },
                        )
                    )
                },
            )
        },
        content = {
            Scaffold(
                snackbarHost = { SnackbarHost(snackbarHostState) },
                content = { padding ->
                    NavHost(
                        navController = bottomNavController,
                        startDestination = selectedTab.name,
                        modifier = Modifier.padding(bottom = padding.calculateBottomPadding()),
                    ) {
                        composable(HomeTab.Feed.name) {
                            FeedActivity(
                                homeViewModel = homeViewModel,
                                siteRes = siteRes,
                                appSettingsViewModel = appSettingsViewModel,
                                drawerState = drawerState,
                                navController = feedNavController,
                            )
                        }

                        composable(HomeTab.Search.name) {
                            CommunityListActivity(
                                followedCommunitiesViewModel = followedCommunitiesViewModel,
                                onSelectCommunity = null,
                                navController = communityListNavController,
                            )
                        }

                        composable(HomeTab.Inbox.name) {
                            InboxActivity(
                                siteResponse = siteRes,
                                homeViewModel = homeViewModel,
                                navController = inboxNavController,
                            )
                        }

                        composable(HomeTab.Saved.name) {
                            PersonProfileActivity(
                                personArg = Either.Left(account!!.id),
                                savedMode = true,
                                accountViewModel = hiltViewModel(),
                                siteViewModel = hiltViewModel(),
                                appSettingsViewModel = appSettingsViewModel,
                                navController = savedAndProfileNavController,
                            )
                        }

                        composable(HomeTab.Profile.name) {
                            PersonProfileActivity(
                                personArg = Either.Left(account!!.id),
                                savedMode = false,
                                accountViewModel = hiltViewModel(),
                                siteViewModel = hiltViewModel(),
                                appSettingsViewModel = appSettingsViewModel,
                                navController = savedAndProfileNavController,
                            )
                        }
                    }
                },
                bottomBar = {
                    BottomNavBar(
                        showBottomNav = appSettingsViewModel.appSettings.value?.showBottomNav,
                        selectedTab = selectedTab,
                        unreadCounts = homeViewModel.unreadCountRes.totalUnreadCount(),
                        onSelect = onSelectTab,
                    )
                },
            )
        },
    )
}

enum class HomeTab {
    Feed, Search, Inbox, Saved, Profile;

    fun needsLogin() = this == Inbox || this == Saved || this == Profile
}

@Composable
fun BottomNavBar(
    selectedTab: HomeTab,
    onSelect: (HomeTab) -> Unit,
    unreadCounts: Int?,
    showBottomNav: Boolean? = true,
) {
    if (showBottomNav == true) {
        // Check for preview mode
        if (LocalContext.current is Activity) {
            val window = (LocalContext.current as Activity).window
            val colorScheme = MaterialTheme.colorScheme

            DisposableEffect(Unit) {
                window.navigationBarColor = colorScheme.surfaceColorAtElevation(3.dp).toArgb()

                onDispose {
                    window.navigationBarColor = colorScheme.background.toArgb()
                }
            }
        }

        NavigationBar {
            for (tab in HomeTab.values()) {
                val selected = tab == selectedTab
                NavigationBarItem(
                    icon = {
                        InboxIconAndBadge(
                            iconBadgeCount = if (tab == HomeTab.Inbox) unreadCounts else null,
                            icon = if (selected) {
                                when (tab) {
                                    HomeTab.Feed -> Icons.Filled.Home
                                    HomeTab.Search -> Icons.Filled.Search
                                    HomeTab.Inbox -> Icons.Filled.Email
                                    HomeTab.Saved -> Icons.Filled.Bookmarks
                                    HomeTab.Profile -> Icons.Filled.Person
                                }
                            } else {
                                when (tab) {
                                    HomeTab.Feed -> Icons.Outlined.Home
                                    HomeTab.Search -> Icons.Outlined.Search
                                    HomeTab.Inbox -> Icons.Outlined.Email
                                    HomeTab.Saved -> Icons.Outlined.Bookmarks
                                    HomeTab.Profile -> Icons.Outlined.Person
                                }
                            },
                            contentDescription = stringResource(
                                when (tab) {
                                    HomeTab.Feed -> R.string.bottomBar_home
                                    HomeTab.Search -> R.string.bottomBar_search
                                    HomeTab.Inbox -> R.string.bottomBar_inbox
                                    HomeTab.Saved -> R.string.bottomBar_bookmarks
                                    HomeTab.Profile -> R.string.bottomBar_profile
                                },
                            ),
                        )
                    },
                    label = {
                        Text(
                            text = stringResource(
                                when (tab) {
                                    HomeTab.Feed -> R.string.bottomBar_label_home
                                    HomeTab.Search -> R.string.bottomBar_label_search
                                    HomeTab.Inbox -> R.string.bottomBar_label_inbox
                                    HomeTab.Saved -> R.string.bottomBar_label_bookmarks
                                    HomeTab.Profile -> R.string.bottomBar_label_profile
                                },
                            ),
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                    },
                    selected = selected,
                    onClick = {
                        onSelect(tab)
                    },
                )
            }
        }
    }
}

fun ApiState<GetUnreadCountResponse>.totalUnreadCount(): Int? {
    return when (this) {
        is ApiState.Success -> data.mentions + data.replies + data.private_messages
        else -> null
    }
}
