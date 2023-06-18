package com.jerboa

import android.app.Application
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.util.Patterns
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.navigation
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import androidx.navigation.navDeepLink
import arrow.core.Either
import com.jerboa.datatypes.types.GetSite
import com.jerboa.datatypes.types.GetUnreadCount
import com.jerboa.db.AccountRepository
import com.jerboa.db.AccountViewModel
import com.jerboa.db.AccountViewModelFactory
import com.jerboa.db.AppDB
import com.jerboa.db.AppSettingsRepository
import com.jerboa.db.AppSettingsViewModel
import com.jerboa.db.AppSettingsViewModelFactory
import com.jerboa.nav.Route
import com.jerboa.nav.dependencyContainer
import com.jerboa.nav.toAbout
import com.jerboa.nav.toAccountSettings
import com.jerboa.nav.toComment
import com.jerboa.nav.toCommentEdit
import com.jerboa.nav.toCommentReply
import com.jerboa.nav.toCommentReport
import com.jerboa.nav.toCommunity
import com.jerboa.nav.toCommunityList
import com.jerboa.nav.toCommunitySideBar
import com.jerboa.nav.toCreatePost
import com.jerboa.nav.toHome
import com.jerboa.nav.toLogin
import com.jerboa.nav.toLookAndFeel
import com.jerboa.nav.toPost
import com.jerboa.nav.toPostEdit
import com.jerboa.nav.toPostReport
import com.jerboa.nav.toPrivateMessageReply
import com.jerboa.nav.toProfile
import com.jerboa.nav.toSettings
import com.jerboa.nav.toSiteSideBar
import com.jerboa.ui.components.comment.edit.CommentEditActivity
import com.jerboa.ui.components.comment.edit.CommentEditDependencies
import com.jerboa.ui.components.comment.edit.CommentEditNavController
import com.jerboa.ui.components.comment.reply.CommentReplyActivity
import com.jerboa.ui.components.comment.reply.CommentReplyDependencies
import com.jerboa.ui.components.comment.reply.CommentReplyNavController
import com.jerboa.ui.components.common.MarkdownHelper
import com.jerboa.ui.components.common.ShowChangelog
import com.jerboa.ui.components.common.getCurrentAccount
import com.jerboa.ui.components.common.getCurrentAccountSync
import com.jerboa.ui.components.community.CommunityActivity
import com.jerboa.ui.components.community.CommunityNavController
import com.jerboa.ui.components.community.CommunityViewModel
import com.jerboa.ui.components.community.list.CommunityListActivity
import com.jerboa.ui.components.community.list.CommunityListDependencies
import com.jerboa.ui.components.community.list.CommunityListNavController
import com.jerboa.ui.components.community.list.ToCommunityList
import com.jerboa.ui.components.community.sidebar.CommunitySideBarNavController
import com.jerboa.ui.components.community.sidebar.CommunitySidebarActivity
import com.jerboa.ui.components.home.*
import com.jerboa.ui.components.home.sidebar.SiteSideBarNavController
import com.jerboa.ui.components.home.sidebar.SiteSidebarActivity
import com.jerboa.ui.components.inbox.InboxNavController
import com.jerboa.ui.components.login.LoginActivity
import com.jerboa.ui.components.login.LoginNavController
import com.jerboa.ui.components.person.PersonProfileActivity
import com.jerboa.ui.components.person.PersonProfileNavController
import com.jerboa.ui.components.post.PostActivity
import com.jerboa.ui.components.post.PostNavController
import com.jerboa.ui.components.post.ToPost
import com.jerboa.ui.components.post.create.CreatePostActivity
import com.jerboa.ui.components.post.create.CreatePostDependencies
import com.jerboa.ui.components.post.create.CreatePostNavController
import com.jerboa.ui.components.post.create.CreatePostViewModel
import com.jerboa.ui.components.post.edit.PostEditActivity
import com.jerboa.ui.components.post.edit.PostEditDependencies
import com.jerboa.ui.components.post.edit.PostEditNavController
import com.jerboa.ui.components.privatemessage.PrivateMessageReplyActivity
import com.jerboa.ui.components.privatemessage.PrivateMessageReplyDependencies
import com.jerboa.ui.components.privatemessage.PrivateMessageReplyNavController
import com.jerboa.ui.components.report.CreateReportNavController
import com.jerboa.ui.components.report.comment.CreateCommentReportActivity
import com.jerboa.ui.components.report.post.CreatePostReportActivity
import com.jerboa.ui.components.settings.AccountSettingsNavController
import com.jerboa.ui.components.settings.LookAndFeelNavController
import com.jerboa.ui.components.settings.SettingsActivity
import com.jerboa.ui.components.settings.SettingsNavController
import com.jerboa.ui.components.settings.about.AboutActivity
import com.jerboa.ui.components.settings.account.AccountSettingsActivity
import com.jerboa.ui.components.settings.account.AccountSettingsViewModel
import com.jerboa.ui.components.settings.account.AccountSettingsViewModelFactory
import com.jerboa.ui.components.settings.lookandfeel.LookAndFeelActivity
import com.jerboa.ui.theme.JerboaTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.delay

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val siteViewModel by viewModels<SiteViewModel>()

    @OptIn(ExperimentalAnimationApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        window.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

        setContent {
            val accountSettingsViewModel: AccountSettingsViewModel = hiltViewModel()
            val accountViewModel: AccountViewModel = hiltViewModel()
            val appSettingsViewModel: AppSettingsViewModel = hiltViewModel()

            LaunchedEffect(Unit) {
                getCurrentAccountSync(accountViewModel).let { account ->
                    siteViewModel.getSite(GetSite(auth = account?.jwt))
                    if (account != null) siteViewModel.fetchUnreadCounts(GetUnreadCount(auth = account.jwt))
                }
            }

            val account = getCurrentAccount(accountViewModel)
            val appSettings by appSettingsViewModel.appSettings.observeAsState()

            JerboaTheme(
                appSettings = appSettings,
            ) {
                val navController = rememberNavController()
                val ctx = LocalContext.current

                MarkdownHelper.init(
                    navController,
                    appSettingsViewModel.appSettings.value?.useCustomTabs ?: true,
                    appSettingsViewModel.appSettings.value?.usePrivateTabs ?: false,
                )

                ShowChangelog(appSettingsViewModel = appSettingsViewModel)

                val commentEditDependencyContainer = dependencyContainer<CommentEditDependencies>()
                val commentReplyDependencyContainer =
                    dependencyContainer<CommentReplyDependencies>()
                val postEditDependencyContainer = dependencyContainer<PostEditDependencies>()
                val createPostDependencyContainer = dependencyContainer<CreatePostDependencies>()
                val communityListDependencyContainer =
                    dependencyContainer<CommunityListDependencies>()
                val privateMessageReplyDependencyContainer =
                    dependencyContainer<PrivateMessageReplyDependencies>()

                NavHost(
                    navController = navController,
                    startDestination = Route.HOME,
                ) {
                    composable(
                        route = Route.LOGIN,
                        deepLinks = DEFAULT_LEMMY_INSTANCES.map { instance ->
                            navDeepLink { uriPattern = "$instance/login" }
                        },
                    ) {
                        LoginActivity(
                            accountViewModel = accountViewModel,
                            siteViewModel = siteViewModel,
                            navController = LoginNavController(
                                navController,
                                toHome = navController.toHome(),
                            )
                        )
                    }

                    composable(
                        route = Route.HOME,
                        arguments = listOf(
                            navArgument(Route.HomeArgs.TAB) {
                                type = Route.HomeArgs.TAB_TYPE
                                defaultValue = Route.HomeArgs.TAB_DEFAULT.name
                            }
                        ),
                        deepLinks = DEFAULT_LEMMY_INSTANCES.map { instance ->
                            navDeepLink {
                                uriPattern = "$instance/inbox"
                                action = Route.HomeArgs.makeRoute(tab = HomeTab.Inbox.name)
                            }
                        },
                    ) {
                        val args = Route.HomeArgs(it)
                        HomeActivity(
                            accountViewModel = accountViewModel,
                            siteViewModel = siteViewModel,
                            appSettingsViewModel = appSettingsViewModel,
                            selectTabArg = args.tab,
                            feedNavController = FeedNavController(
                                navController,
                                toPostEdit = navController.toPostEdit(postEditDependencyContainer),
                                toCommunity = navController.toCommunity(),
                                toProfile = navController.toProfile(),
                                toSettings = navController.toSettings(),
                                toCreatePost = navController.toCreatePost(
                                    createPostDependencyContainer
                                ),
                                toPost = navController.toPost(),
                                toPostReport = navController.toPostReport(),
                                toLogin = navController.toLogin(),
                                toSiteSideBar = navController.toSiteSideBar(),
                            ),
                            communityListNavController = CommunityListNavController(
                                navController,
                                toCommunity = navController.toCommunity(),
                            ),
                            inboxNavController = InboxNavController(
                                navController,
                                toCommentReply = navController.toCommentReply(
                                    commentReplyDependencyContainer
                                ),
                                toPrivateMessageReply = navController.toPrivateMessageReply(
                                    privateMessageReplyDependencyContainer
                                ),
                                toProfile = navController.toProfile(),
                                toCommentReport = navController.toCommentReport(),
                                toComment = navController.toComment(),
                                toPost = navController.toPost(),
                                toCommunity = navController.toCommunity(),
                            ),
                            savedAndProfileNavController = PersonProfileNavController(
                                navController,
                                toCommentEdit = navController.toCommentEdit(
                                    commentEditDependencyContainer
                                ),
                                toCommentReply = navController.toCommentReply(
                                    commentReplyDependencyContainer
                                ),
                                toPostEdit = navController.toPostEdit(postEditDependencyContainer),
                                toCommentReport = navController.toCommentReport(),
                                toPostReport = navController.toPostReport(),
                                toCommunity = navController.toCommunity(),
                                toPost = navController.toPost(),
                                toProfile = navController.toProfile(),
                                toComment = navController.toComment(),
                            )
                        )
                    }

                    val communityGraph = "COMMUNITY_GRAPH"
                    navigation(route = communityGraph, startDestination = Route.COMMUNITY_FROM_ID) {
                        composable(
                            route = Route.COMMUNITY_FROM_ID,
                            arguments = listOf(
                                navArgument(Route.CommunityFromIdArgs.ID) {
                                    type = Route.CommunityFromIdArgs.ID_TYPE
                                },
                            ),
                        ) {
                            val communityViewModel: CommunityViewModel = viewModel(
                                remember(it) { navController.getBackStackEntry(communityGraph) }
                            )

                            val args = Route.CommunityFromIdArgs(it)
                            CommunityActivity(
                                communityArg = Either.Left(args.id),
                                communityViewModel = communityViewModel,
                                accountViewModel = accountViewModel,
                                appSettingsViewModel = appSettingsViewModel,
                                siteViewModel = siteViewModel,
                                navController = CommunityNavController(
                                    navController,
                                    toPostEdit = navController.toPostEdit(
                                        postEditDependencyContainer
                                    ),
                                    toCreatePost = navController.toCreatePost(
                                        createPostDependencyContainer
                                    ),
                                    toCommunitySideBar = navController.toCommunitySideBar(),
                                    toPost = navController.toPost(),
                                    toPostReport = navController.toPostReport(),
                                    toCommunity = navController.toCommunity(),
                                    toProfile = navController.toProfile(),
                                ),
                            )
                        }

                        // Only necessary for community deeplinks
                        composable(
                            route = Route.COMMUNITY_FROM_URL,
                            deepLinks = listOf(
                                navDeepLink { uriPattern = Route.COMMUNITY_FROM_URL },
                            ),
                            arguments = listOf(
                                navArgument(Route.CommunityFromUrlArgs.NAME) {
                                    type = Route.CommunityFromUrlArgs.NAME_TYPE
                                },
                                navArgument(Route.CommunityFromUrlArgs.INSTANCE) {
                                    type = Route.CommunityFromUrlArgs.INSTANCE_TYPE
                                },
                            ),
                        ) {
                            val communityViewModel: CommunityViewModel = viewModel(
                                remember(it) { navController.getBackStackEntry(communityGraph) }
                            )

                            val args = Route.CommunityFromUrlArgs(it)
                            val qualifiedName = "${args.name}@{$args.instance}"
                            CommunityActivity(
                                communityArg = Either.Right(qualifiedName),
                                communityViewModel = communityViewModel,
                                accountViewModel = accountViewModel,
                                appSettingsViewModel = appSettingsViewModel,
                                siteViewModel = siteViewModel,
                                navController = CommunityNavController(
                                    navController,
                                    toPostEdit = navController.toPostEdit(
                                        postEditDependencyContainer
                                    ),
                                    toCreatePost = navController.toCreatePost(
                                        createPostDependencyContainer
                                    ),
                                    toCommunitySideBar = navController.toCommunitySideBar(),
                                    toPost = navController.toPost(),
                                    toPostReport = navController.toPostReport(),
                                    toCommunity = navController.toCommunity(),
                                    toProfile = navController.toProfile(),
                                ),
                            )
                        }

                        composable(route = Route.COMMUNITY_SIDEBAR) {
                            val communityViewModel: CommunityViewModel = viewModel(
                                remember(it) { navController.getBackStackEntry(communityGraph) }
                            )

                            CommunitySidebarActivity(
                                communityViewModel = communityViewModel,
                                navController = CommunitySideBarNavController(navController),
                            )
                        }
                    }

                    composable(
                        route = Route.PROFILE_FROM_ID,
                        arguments = listOf(
                            navArgument(Route.ProfileFromIdArgs.ID) {
                                type = Route.ProfileFromIdArgs.ID_TYPE
                            },
                            navArgument(Route.ProfileFromIdArgs.SAVED) {
                                defaultValue = Route.ProfileFromIdArgs.SAVED_DEFAULT
                                type = Route.ProfileFromIdArgs.SAVED_TYPE
                            },
                        ),
                    ) {
                        val args = Route.ProfileFromIdArgs(it)
                        PersonProfileActivity(
                            personArg = Either.Left(args.id),
                            savedMode = args.saved,
                            accountViewModel = accountViewModel,
                            appSettingsViewModel = appSettingsViewModel,
                            siteViewModel = siteViewModel,
                            navController = PersonProfileNavController(
                                navController,
                                toCommentEdit = navController.toCommentEdit(
                                    commentEditDependencyContainer
                                ),
                                toCommentReply = navController.toCommentReply(
                                    commentReplyDependencyContainer
                                ),
                                toPostEdit = navController.toPostEdit(postEditDependencyContainer),
                                toCommentReport = navController.toCommentReport(),
                                toPostReport = navController.toPostReport(),
                                toCommunity = navController.toCommunity(),
                                toPost = navController.toPost(),
                                toProfile = navController.toProfile(),
                                toComment = navController.toComment(),
                            ),
                        )
                    }

                    // Necessary for deep links
                    composable(
                        route = Route.PROFILE_FROM_URL,
                        deepLinks = listOf(
                            navDeepLink { uriPattern = Route.PROFILE_FROM_URL },
                        ),
                        arguments = listOf(
                            navArgument(Route.ProfileFromUrlArgs.NAME) {
                                type = Route.ProfileFromUrlArgs.NAME_TYPE
                            },
                            navArgument(Route.ProfileFromUrlArgs.INSTANCE) {
                                type = Route.ProfileFromUrlArgs.INSTANCE_TYPE
                            },
                        ),
                    ) {
                        val args = Route.ProfileFromUrlArgs(it)
                        val qualifiedName = "${args.name}@${args.instance}"
                        PersonProfileActivity(
                            personArg = Either.Right(qualifiedName),
                            savedMode = false,
                            accountViewModel = accountViewModel,
                            appSettingsViewModel = appSettingsViewModel,
                            siteViewModel = siteViewModel,
                            navController = PersonProfileNavController(
                                navController,
                                toCommentEdit = navController.toCommentEdit(
                                    commentEditDependencyContainer,
                                ),
                                toCommentReply = navController.toCommentReply(
                                    commentReplyDependencyContainer,
                                ),
                                toPostEdit = navController.toPostEdit(postEditDependencyContainer),
                                toCommentReport = navController.toCommentReport(),
                                toPostReport = navController.toPostReport(),
                                toCommunity = navController.toCommunity(),
                                toPost = navController.toPost(),
                                toProfile = navController.toProfile(),
                                toComment = navController.toComment(),
                            ),
                        )
                    }

                    composable(route = Route.COMMUNITY_LIST) {
                        val dependencies: CommunityListDependencies =
                            viewModel(it, factory = communityListDependencyContainer)

                        CommunityListActivity(
                            accountViewModel = accountViewModel,
                            siteViewModel = siteViewModel,
                            onSelectCommunity = dependencies.onSelectCommunity,
                            navController = CommunityListNavController(
                                navController,
                                toCommunity = navController.toCommunity(),
                            )
                        )
                    }

                    composable(
                        route = Route.CREATE_POST,
                        deepLinks = listOf(
                            navDeepLink { mimeType = "text/plain" },
                            navDeepLink { mimeType = "image/*" },
                        ),
                    ) {
                        val dependencies: CreatePostDependencies =
                            viewModel(it, factory = createPostDependencyContainer)

                        val activity = ctx.findActivity()
                        val text = activity?.intent?.getStringExtra(Intent.EXTRA_TEXT) ?: ""
                        val image =
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                activity?.intent?.getParcelableExtra(
                                    Intent.EXTRA_STREAM,
                                    Uri::class.java,
                                )
                            } else {
                                @Suppress("DEPRECATION")
                                activity?.intent?.getParcelableExtra(Intent.EXTRA_STREAM) as? Uri
                            }
                        // url and body will be empty everytime except when there is EXTRA TEXT in the intent
                        var url = ""
                        var body = ""
                        if (Patterns.WEB_URL.matcher(text).matches()) {
                            url = text
                        } else {
                            body = text
                        }

                        val createPostViewModel: CreatePostViewModel = viewModel(it)

                        CreatePostActivity(
                            accountViewModel = accountViewModel,
                            createPostViewModel = createPostViewModel,
                            inCommunity = dependencies.selectedCommunity,
                            initialUrl = url,
                            initialBody = body,
                            initialImage = image,
                            navController = CreatePostNavController(
                                navController,
                                toPost = ToPost { postId ->
                                    navController.navigate("post/$postId") {
                                        popUpTo("createPost") { inclusive = true }
                                    }
                                },
                                toCommunityList = navController.toCommunityList(
                                    communityListDependencyContainer
                                )
                            ),
                        )
                        activity?.intent?.replaceExtras(Bundle())
                    }

                    composable(
                        route = Route.POST,
                        deepLinks = DEFAULT_LEMMY_INSTANCES.map { instance ->
                            navDeepLink { uriPattern = "$instance/post/{${Route.PostArgs.ID}}" }
                        },
                        arguments = listOf(
                            navArgument(Route.PostArgs.ID) {
                                type = Route.PostArgs.ID_TYPE
                            },
                        ),
                    ) {
                        var args by remember { mutableStateOf(Route.PostArgs(it)) }

                        LaunchedEffect(Unit) {
                            delay(5000)
                            args = Route.PostArgs(3)
                            Log.d("Flow Combine", "args updated to 3")
                        }

                        PostActivity(
                            postArg = Either.Left(args.id),
                            accountViewModel = accountViewModel,
                            siteViewModel = siteViewModel,
                            appSettingsViewModel = appSettingsViewModel,
                            navController = PostNavController(
                                navController,
                                toCommentEdit = navController.toCommentEdit(
                                    commentEditDependencyContainer,
                                ),
                                toCommentReply = navController.toCommentReply(
                                    commentReplyDependencyContainer,
                                ),
                                toPostEdit = navController.toPostEdit(
                                    postEditDependencyContainer,
                                ),
                                toCommunity = navController.toCommunity(),
                                toPostReport = navController.toPostReport(),
                                toProfile = navController.toProfile(),
                                toPost = navController.toPost(),
                                toComment = navController.toComment(),
                                toCommentReport = navController.toCommentReport(),
                            ),
                        )
                    }

                    composable(
                        route = Route.COMMENT,
                        deepLinks = DEFAULT_LEMMY_INSTANCES.map { instance ->
                            navDeepLink {
                                uriPattern = "$instance/comment/{${Route.CommentArgs.ID}}"
                            }
                        },
                        arguments = listOf(
                            navArgument(Route.CommentArgs.ID) {
                                type = Route.CommentArgs.ID_TYPE
                            },
                        ),
                    ) {
                        val args = Route.CommentArgs(it)
                        PostActivity(
                            postArg = Either.Right(args.id),
                            accountViewModel = accountViewModel,
                            siteViewModel = siteViewModel,
                            appSettingsViewModel = appSettingsViewModel,
                            navController = PostNavController(
                                navController,
                                toCommentEdit = navController.toCommentEdit(
                                    commentEditDependencyContainer,
                                ),
                                toCommentReply = navController.toCommentReply(
                                    commentReplyDependencyContainer,
                                ),
                                toPostEdit = navController.toPostEdit(
                                    postEditDependencyContainer,
                                ),
                                toCommunity = navController.toCommunity(),
                                toPostReport = navController.toPostReport(),
                                toProfile = navController.toProfile(),
                                toPost = navController.toPost(),
                                toComment = navController.toComment(),
                                toCommentReport = navController.toCommentReport(),
                            ),
                        )
                    }

                    composable(route = Route.COMMENT_REPLY) {
                        val dependencies: CommentReplyDependencies =
                            viewModel(it, factory = commentReplyDependencyContainer)

                        CommentReplyActivity(
                            replyItem = dependencies.replyItem,
                            isModerator = dependencies.isModerator,
                            accountViewModel = accountViewModel,
                            siteViewModel = siteViewModel,
                            onCommentReply = dependencies.onCommentReply,
                            navController = CommentReplyNavController(
                                navController,
                                toProfile = navController.toProfile(),
                            ),
                        )
                    }

                    composable(route = Route.SITE_SIDEBAR) {
                        SiteSidebarActivity(
                            siteViewModel = siteViewModel,
                            navController = SiteSideBarNavController(navController),
                        )
                    }

                    composable(route = Route.COMMENT_EDIT) {
                        val dependencies: CommentEditDependencies =
                            viewModel(it, factory = commentEditDependencyContainer)

                        CommentEditActivity(
                            commentView = dependencies.commentView,
                            accountViewModel = accountViewModel,
                            onCommentEdit = dependencies.onCommentEdit,
                            navController = CommentEditNavController(navController),
                        )
                    }

                    composable(route = Route.POST_EDIT) {
                        val dependencies: PostEditDependencies =
                            viewModel(it, factory = postEditDependencyContainer)

                        PostEditActivity(
                            accountViewModel = accountViewModel,
                            postView = dependencies.postView,
                            onPostEdit = dependencies.onPostEdit,
                            navController = PostEditNavController(navController),
                        )
                    }

                    composable(route = Route.PRIVATE_MESSAGE_REPLY) {
                        val dependencies: PrivateMessageReplyDependencies =
                            viewModel(it, factory = privateMessageReplyDependencyContainer)

                        PrivateMessageReplyActivity(
                            privateMessageView = dependencies.privateMessageView,
                            accountViewModel = accountViewModel,
                            siteViewModel = siteViewModel,
                            navController = PrivateMessageReplyNavController(
                                navController,
                                toProfile = navController.toProfile(),
                            )
                        )
                    }

                    composable(
                        route = Route.COMMENT_REPORT,
                        arguments = listOf(
                            navArgument(Route.CommentReportArgs.ID) {
                                type = Route.CommentReportArgs.ID_TYPE
                            },
                        ),
                    ) {
                        val args = Route.CommentReportArgs(it)
                        CreateCommentReportActivity(
                            commentId = args.id,
                            accountViewModel = accountViewModel,
                            navController = CreateReportNavController(navController),
                        )
                    }

                    composable(
                        route = Route.POST_REPORT,
                        arguments = listOf(
                            navArgument(Route.PostReportArgs.ID) {
                                type = Route.PostReportArgs.ID_TYPE
                            },
                        ),
                    ) {
                        val args = Route.PostReportArgs(it)
                        CreatePostReportActivity(
                            postId = args.id,
                            accountViewModel = accountViewModel,
                            navController = CreateReportNavController(navController),
                        )
                    }

                    composable(route = Route.SETTINGS) {
                        SettingsActivity(
                            accountViewModel = accountViewModel,
                            navController = SettingsNavController(
                                navController,
                                toLookAndFeel = navController.toLookAndFeel(),
                                toAccountSettings = navController.toAccountSettings(),
                                toAbout = navController.toAbout(),
                            ),
                        )
                    }

                    composable(route = Route.LOOK_AND_FEEL) {
                        LookAndFeelActivity(
                            appSettingsViewModel = appSettingsViewModel,
                            navController = LookAndFeelNavController(navController),
                        )
                    }

                    composable(
                        route = Route.ACCOUNT_SETTINGS,
                        deepLinks = DEFAULT_LEMMY_INSTANCES.map { instance ->
                            navDeepLink { uriPattern = "$instance/settings" }
                        },
                    ) {
                        AccountSettingsActivity(
                            accountViewModel = accountViewModel,
                            siteViewModel = siteViewModel,
                            accountSettingsViewModel = accountSettingsViewModel,
                            navController = AccountSettingsNavController(navController),
                        )
                    }

                    composable(route = "about") {
                        AboutActivity(
                            navController = navController,
                            useCustomTabs = appSettings?.useCustomTabs ?: true,
                            usePrivateTabs = appSettings?.usePrivateTabs ?: false,
                        )
                    }
                }
            }
        }
    }
}
