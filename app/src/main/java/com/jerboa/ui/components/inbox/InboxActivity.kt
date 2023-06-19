package com.jerboa.ui.components.inbox

import android.content.Context
import android.util.Log
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.pullrefresh.PullRefreshIndicator
import androidx.compose.material.pullrefresh.pullRefresh
import androidx.compose.material.pullrefresh.rememberPullRefreshState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.jerboa.*
import com.jerboa.api.ApiState
import com.jerboa.datatypes.types.BlockPerson
import com.jerboa.datatypes.types.CommentSortType
import com.jerboa.datatypes.types.CreateCommentLike
import com.jerboa.datatypes.types.GetPersonMentions
import com.jerboa.datatypes.types.GetPrivateMessages
import com.jerboa.datatypes.types.GetReplies
import com.jerboa.datatypes.types.GetSiteResponse
import com.jerboa.datatypes.types.GetUnreadCount
import com.jerboa.datatypes.types.MarkAllAsRead
import com.jerboa.datatypes.types.MarkCommentReplyAsRead
import com.jerboa.datatypes.types.MarkPersonMentionAsRead
import com.jerboa.datatypes.types.MarkPrivateMessageAsRead
import com.jerboa.datatypes.types.SaveComment
import com.jerboa.db.Account
import com.jerboa.db.AccountViewModel
import com.jerboa.db.AppSettingsViewModel
import com.jerboa.nav.NavControllerWrapper
import com.jerboa.nav.initializeOnce
import com.jerboa.ui.components.comment.mentionnode.CommentMentionNode
import com.jerboa.ui.components.comment.reply.CommentReplyDependencies
import com.jerboa.ui.components.comment.reply.CommentReplyViewModel
import com.jerboa.ui.components.comment.reply.ReplyItem
import com.jerboa.ui.components.comment.reply.ToCommentReply
import com.jerboa.ui.components.comment.replynode.CommentReplyNode
import com.jerboa.ui.components.common.ApiEmptyText
import com.jerboa.ui.components.common.ApiErrorText
import com.jerboa.ui.components.common.BottomAppBarAll
import com.jerboa.ui.components.common.LoadingBar
import com.jerboa.ui.components.common.getCurrentAccount
import com.jerboa.ui.components.common.simpleVerticalScrollbar
import com.jerboa.ui.components.home.HomeViewModel
import com.jerboa.ui.components.home.SiteViewModel
import com.jerboa.ui.components.home.showAvatar
import com.jerboa.ui.components.home.totalUnreadCount
import com.jerboa.ui.components.privatemessage.PrivateMessage
import com.jerboa.ui.components.privatemessage.PrivateMessageReplyDependencies
import com.jerboa.ui.components.privatemessage.PrivateMessageReplyViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InboxActivity(
    navController: InboxNavController,
    siteResponse: GetSiteResponse,
    homeViewModel: HomeViewModel,
) {
    Log.d("jerboa", "got to inbox activity")

    val snackbarHostState = remember { SnackbarHostState() }

    val unreadCount = homeViewModel.unreadCountRes.totalUnreadCount()
    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior(rememberTopAppBarState())

    val inboxViewModel: InboxViewModel = hiltViewModel()
    ApiSuccessEffect(inboxViewModel.markAllAsReadRes) {
        homeViewModel.reloadUnreadCounts()
    }
    ApiSuccessEffect(inboxViewModel.markReplyAsReadRes) {
        homeViewModel.reloadUnreadCounts()
    }
    ApiSuccessEffect(inboxViewModel.markMentionAsReadRes) {
        homeViewModel.reloadUnreadCounts()
    }
    ApiSuccessEffect(inboxViewModel.markMessageAsReadRes) {
        homeViewModel.reloadUnreadCounts()
    }
    PersonBlockedSnackbarEffect(inboxViewModel.blockPersonRes, snackbarHostState)

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            val filter by inboxViewModel.filter.collectAsState()
            InboxHeader(
                scrollBehavior = scrollBehavior,
                unreadCount = unreadCount,
                navController = navController,
                selectedUnreadOrAll = unreadOrAllFromBool(filter.unreadOnly),
                onClickUnreadOrAll = { unreadOrAll ->
                    inboxViewModel.updateUnreadOnly(unreadOrAll == UnreadOrAll.Unread)
                },
                onClickMarkAllAsRead = {
                    inboxViewModel.markAllAsRead()
                },
            )
        },
        content = {
            InboxTabs(
                padding = it,
                navController = navController,
                inboxViewModel = inboxViewModel,
                siteResponse = siteResponse,
            )
        },
    )
}

enum class InboxTab {
    Replies,
    Mentions,
    Messages,
}

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterialApi::class)
@Composable
fun InboxTabs(
    navController: InboxNavController,
    inboxViewModel: InboxViewModel,
    siteResponse: GetSiteResponse,
    padding: PaddingValues,
) {
    val account by inboxViewModel.account.observeAsState()

    val tabTitles = InboxTab.values().map { it.toString() }
    val pagerState = rememberPagerState()
    val scope = rememberCoroutineScope()

    Column(
        modifier = Modifier.padding(padding),
    ) {
        TabRow(
            selectedTabIndex = pagerState.currentPage,
            indicator = { tabPositions ->
                TabRowDefaults.Indicator(
                    Modifier.pagerTabIndicatorOffset2(
                        pagerState,
                        tabPositions,
                    ),
                )
            },
            tabs = {
                tabTitles.forEachIndexed { index, title ->
                    Tab(
                        selected = pagerState.currentPage == index,
                        onClick = {
                            scope.launch {
                                pagerState.animateScrollToPage(index)
                            }
                        },
                        text = { Text(text = title) },
                    )
                }
            },
        )
        HorizontalPager(
            pageCount = tabTitles.size,
            state = pagerState,
            verticalAlignment = Alignment.Top,
            modifier = Modifier.fillMaxSize(),
        ) { tabIndex ->
            when (tabIndex) {
                InboxTab.Replies.ordinal -> {
                    val listState = rememberLazyListState()

                    // observer when reached end of list
                    val endOfListReached by remember {
                        derivedStateOf {
                            listState.isScrolledToEnd()
                        }
                    }

                    LaunchedEffect(endOfListReached) {
                        if (endOfListReached) {
                            inboxViewModel.nextRepliesPage()
                        }
                    }

                    val fetchState = inboxViewModel.fetchingMoreMentions
                    val loading = fetchState is ApiState.Loading

                    val refreshState = rememberPullRefreshState(
                        refreshing = loading,
                        onRefresh = inboxViewModel::reloadReplies,
                    )
                    Box(modifier = Modifier.pullRefresh(refreshState)) {
                        PullRefreshIndicator(loading, refreshState, Modifier.align(Alignment.TopCenter))
                        if (loading) LoadingBar()

                        val mentions = inboxViewModel.mentionsRes
                        if (mentions.isEmpty()) {
                            when (fetchState) {
                                is ApiState.Empty -> ApiEmptyText()
                                is ApiState.Failure -> ApiErrorText(fetchState.msg)
                                else -> {}
                            }
                        } else {
                            val replies = inboxViewModel.repliesRes
                            LazyColumn(
                                state = listState,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .simpleVerticalScrollbar(listState),
                            ) {
                                items(
                                    replies,
                                    key = { reply -> reply.comment_reply.id },
                                ) { crv ->
                                    CommentReplyNode(
                                        commentReplyView = crv,
                                        onUpvoteClick = { cr ->
                                            inboxViewModel.likeReply(cr, VoteType.Upvote)
                                        },
                                        onDownvoteClick = { cr ->
                                            inboxViewModel.likeReply(cr, VoteType.Downvote)
                                        },
                                        onReplyClick = { cr ->
                                            navController.toCommentReply.navigate(
                                                CommentReplyDependencies(
                                                    ReplyItem.CommentReplyItem(cr),
                                                    // TODO(nahwneeth), how to know if mod or not
                                                    isModerator = false,
                                                    onCommentReply = null,
                                                )
                                            )
                                        },
                                        onSaveClick = inboxViewModel::saveReply,
                                        onMarkAsReadClick = inboxViewModel::markReplyAsRead,
                                        onReportClick = { cv ->
                                            navController.toCommentReport.navigate(cv.comment.id)
                                        },
                                        onCommentLinkClick = { cv ->
                                            // Go to the parent comment or post instead for context
                                            val parent = getCommentParentId(cv.comment)
                                            if (parent != null) {
                                                navController.toComment.navigate(parent)
                                            } else {
                                                navController.toPost.navigate(cv.post.id)
                                            }
                                        },
                                        onPersonClick = { personId ->
                                            navController.toProfile.navigate(personId)
                                        },
                                        onCommunityClick = { community ->
                                            navController.toCommunity.navigate(community.id)
                                        },
                                        onBlockCreatorClick = inboxViewModel::blockPerson,
                                        onPostClick = { postId ->
                                            navController.toPost.navigate(postId)
                                        },
                                        account = account,
                                        showAvatar = siteResponse.showAvatar(),
                                    )
                                }
                            }
                        }
                    }
                }

                InboxTab.Mentions.ordinal -> {
                    val listState = rememberLazyListState()

                    // observer when reached end of list
                    val endOfListReached by remember {
                        derivedStateOf {
                            listState.isScrolledToEnd()
                        }
                    }

                    LaunchedEffect(endOfListReached) {
                        if (endOfListReached) {
                            inboxViewModel.nextMentionsPage()
                        }
                    }

                    val fetchState = inboxViewModel.fetchingMoreMentions
                    val loading = fetchState is ApiState.Loading

                    val refreshState = rememberPullRefreshState(
                        refreshing = loading,
                        onRefresh = inboxViewModel::reloadMentions,
                    )
                    Box(modifier = Modifier.pullRefresh(refreshState)) {
                        PullRefreshIndicator(loading, refreshState, Modifier.align(Alignment.TopCenter))
                        if (loading) LoadingBar()

                        val mentions = inboxViewModel.mentionsRes
                        if (mentions.isEmpty()) {
                            when (fetchState) {
                                is ApiState.Empty -> ApiEmptyText()
                                is ApiState.Failure -> ApiErrorText(fetchState.msg)
                                else -> {}
                            }
                        } else {
                            LazyColumn(
                                state = listState,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .simpleVerticalScrollbar(listState),
                            ) {
                                items(
                                    mentions,
                                    key = { mention -> mention.person_mention.id },
                                ) { pmv ->
                                    CommentMentionNode(
                                        personMentionView = pmv,
                                        onUpvoteClick = { pm ->
                                            inboxViewModel.likeMention(pm, VoteType.Upvote)
                                        },
                                        onDownvoteClick = { pm ->
                                            inboxViewModel.likeMention(pm, VoteType.Downvote)
                                        },
                                        onReplyClick = { pm ->
                                            navController.toCommentReply.navigate(
                                                CommentReplyDependencies(
                                                    ReplyItem.MentionReplyItem(pm),
                                                    // TODO(nahwneeth), how to know if mod or not
                                                    isModerator = false,
                                                    onCommentReply = null,
                                                )
                                            )
                                        },
                                        onSaveClick = inboxViewModel::saveMention,
                                        onMarkAsReadClick = inboxViewModel::markPersonMentionAsRead,
                                        onReportClick = { pm ->
                                            navController.toComment.navigate(pm.comment.id)
                                        },
                                        onLinkClick = { pm ->
                                            // Go to the parent comment or post instead for context
                                            val parent = getCommentParentId(pm.comment)
                                            if (parent != null) {
                                                navController.toComment.navigate(parent)
                                            } else {
                                                navController.toPost.navigate(pm.post.id)
                                            }
                                        },
                                        onPersonClick = { personId ->
                                            navController.toProfile.navigate(personId)
                                        },
                                        onCommunityClick = { community ->
                                            navController.toCommunity.navigate(community.id)
                                        },
                                        onBlockCreatorClick = inboxViewModel::blockPerson,
                                        onPostClick = { postId ->
                                            navController.toPost.navigate(postId)
                                        },
                                        account = account,
                                        showAvatar = siteResponse.showAvatar(),
                                    )
                                }
                            }
                        }
                    }
                }

                InboxTab.Messages.ordinal -> {
                    val listState = rememberLazyListState()

                    // observer when reached end of list
                    val endOfListReached by remember {
                        derivedStateOf {
                            listState.isScrolledToEnd()
                        }
                    }

                    // act when end of list reached
                    LaunchedEffect(endOfListReached) {
                        inboxViewModel.nextMessagesPage()
                    }

                    val fetchState = inboxViewModel.fetchingMoreMentions
                    val loading = fetchState is ApiState.Loading

                    val refreshState = rememberPullRefreshState(
                        refreshing = loading,
                        onRefresh = inboxViewModel::reloadMessages,
                    )
                    Box(modifier = Modifier.pullRefresh(refreshState)) {
                        PullRefreshIndicator(loading, refreshState, Modifier.align(Alignment.TopCenter))
                        if (loading) LoadingBar()

                        val mentions = inboxViewModel.mentionsRes
                        if (mentions.isEmpty()) {
                            when (fetchState) {
                                is ApiState.Empty -> ApiEmptyText()
                                is ApiState.Failure -> ApiErrorText(fetchState.msg)
                                else -> {}
                            }
                        } else {
                            val messages = inboxViewModel.messagesRes
                            LazyColumn(
                                state = listState,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .simpleVerticalScrollbar(listState),
                            ) {
                                items(
                                    messages,
                                    key = { message -> message.private_message.id },
                                ) { message ->
                                    account?.also { acct ->
                                        PrivateMessage(
                                            myPersonId = acct.id,
                                            privateMessageView = message,
                                            onReplyClick = { privateMessageView ->
                                                navController.toPrivateMessageReply.navigate(
                                                    PrivateMessageReplyDependencies(privateMessageView)
                                                )
                                            },
                                            onMarkAsReadClick = inboxViewModel::markPrivateMessageAsRead,
                                            onPersonClick = { personId ->
                                                navController.toProfile.navigate(personId)
                                            },
                                            account = acct,
                                            showAvatar = siteResponse.showAvatar(),
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
