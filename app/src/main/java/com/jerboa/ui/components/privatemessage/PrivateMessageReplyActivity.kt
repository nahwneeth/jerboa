package com.jerboa.ui.components.privatemessage

import android.util.Log
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.TextFieldValue
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.jerboa.api.ApiState
import com.jerboa.datatypes.types.PrivateMessageView
import com.jerboa.db.AccountViewModel
import com.jerboa.nav.initializeOnce
import com.jerboa.ui.components.common.LoadingBar
import com.jerboa.ui.components.common.getCurrentAccount
import com.jerboa.ui.components.home.SiteViewModel

@Composable
fun PrivateMessageReplyActivity(
    privateMessageView: PrivateMessageView,
    accountViewModel: AccountViewModel,
    siteViewModel: SiteViewModel,
    navController: PrivateMessageReplyNavController,
) {
    Log.d("jerboa", "got to private message reply activity")

    val account = getCurrentAccount(accountViewModel = accountViewModel)

    var reply by rememberSaveable(stateSaver = TextFieldValue.Saver) { mutableStateOf(TextFieldValue("")) }

    val focusManager = LocalFocusManager.current

    val privateMessageReplyViewModel : PrivateMessageReplyViewModel = viewModel()
    initializeOnce(privateMessageReplyViewModel) {
        initialize(privateMessageView)
    }

    val loading = when (privateMessageReplyViewModel.createMessageRes) {
        ApiState.Loading -> true
        else -> false
    }

    Surface(color = MaterialTheme.colorScheme.background) {
        Scaffold(
            topBar = {
                PrivateMessageReplyHeader(
                    navController = navController,
                    loading = loading,
                    onSendClick = {
                        account?.also { acct ->
                            privateMessageReplyViewModel.createPrivateMessage(
                                content = reply.text,
                                account = acct,
                            ) {
                                focusManager.clearFocus()
                                navController.navigateUp()
                            }
                        }
                    },
                )
            },
            content = { padding ->
                if (loading) {
                    LoadingBar(padding)
                } else {
                    privateMessageReplyViewModel.replyItem?.also { pmv ->
                        PrivateMessageReply(
                            privateMessageView = pmv,
                            account = account,
                            reply = reply,
                            onReplyChange = { reply = it },
                            onPersonClick = { personId ->
                                navController.toProfile.navigate(personId)
                            },
                            modifier = Modifier
                                .padding(padding)
                                .imePadding(),
                            showAvatar = siteViewModel.showAvatar(),
                        )
                    }
                }
            },
        )
    }
}
