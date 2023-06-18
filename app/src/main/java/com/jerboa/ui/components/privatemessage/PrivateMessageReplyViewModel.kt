package com.jerboa.ui.components.privatemessage

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.focus.FocusManager
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavController
import com.jerboa.api.API
import com.jerboa.api.ApiState
import com.jerboa.api.apiWrapper
import com.jerboa.datatypes.types.CreatePrivateMessage
import com.jerboa.datatypes.types.PrivateMessageResponse
import com.jerboa.datatypes.types.PrivateMessageView
import com.jerboa.db.Account
import com.jerboa.nav.Initializable
import kotlinx.coroutines.launch

class PrivateMessageReplyViewModel : ViewModel(), Initializable {
    override var initialized = false

    var createMessageRes: ApiState<PrivateMessageResponse> by mutableStateOf(ApiState.Empty)
        private set
    var replyItem by mutableStateOf<PrivateMessageView?>(null)
        private set

    fun initialize(
        newReplyItem: PrivateMessageView,
    ) {
        replyItem = newReplyItem
        initialized = true
    }

    fun createPrivateMessage(
        content: String,
        account: Account,
        onFinish: () -> Unit,
    ) {
        viewModelScope.launch {
            val form = CreatePrivateMessage(
                content = content,
                recipient_id = replyItem!!.recipient.id,
                auth = account.jwt,

            )
            createMessageRes = ApiState.Loading
            createMessageRes = apiWrapper(API.getInstance().createPrivateMessage(form))

//            focusManager.clearFocus()
//            navController.navigateUp()
            onFinish()
        }
    }
}
