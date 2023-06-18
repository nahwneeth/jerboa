
package com.jerboa.ui.components.post.edit

import android.util.Log
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.TextFieldValue
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.jerboa.api.ApiState
import com.jerboa.api.uploadPictrsImage
import com.jerboa.datatypes.types.EditPost
import com.jerboa.datatypes.types.PostView
import com.jerboa.db.AccountViewModel
import com.jerboa.imageInputStreamFromUri
import com.jerboa.nav.initializeOnce
import com.jerboa.ui.components.common.LoadingBar
import com.jerboa.ui.components.common.getCurrentAccount
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PostEditActivity(
    accountViewModel: AccountViewModel,
    navController: PostEditNavController,
    postView: PostView,
    onPostEdit: OnPostEdit?,
) {
    Log.d("jerboa", "got to post edit activity")

    val ctx = LocalContext.current
    val account = getCurrentAccount(accountViewModel = accountViewModel)
    val scope = rememberCoroutineScope()

    val postEditViewModel: PostEditViewModel = viewModel()
    initializeOnce(postEditViewModel) {
        initialize(postView)
    }

    val pv = postView
    var name by rememberSaveable { mutableStateOf(pv.post.name.orEmpty()) }
    var url by rememberSaveable { mutableStateOf(pv.post.url.orEmpty()) }
    var body by rememberSaveable(stateSaver = TextFieldValue.Saver) {
        mutableStateOf(
            TextFieldValue(
                pv.post.body.orEmpty(),
            ),
        )
    }
    var formValid by rememberSaveable { mutableStateOf(true) }

    val onSuccess : OnPostEdit = { it ->
        onPostEdit?.invoke(it)
        navController.navigateUp()
    }

    Scaffold(
        topBar = {
            Column {
                val loading = when (postEditViewModel.editPostRes) {
                    ApiState.Loading -> true
                    else -> false
                }
                EditPostHeader(
                    navController = navController,
                    formValid = formValid,
                    loading = loading,
                    onEditPostClick = {
                        account?.also { acct ->
                            // Clean up that data
                            val nameOut = name.trim()
                            val bodyOut = body.text.trim().ifEmpty { null }
                            val urlOut = url.trim().ifEmpty { null }

                            postEditViewModel.editPost(
                                form = EditPost(
                                    post_id = pv.post.id,
                                    name = nameOut,
                                    url = urlOut,
                                    body = bodyOut,
                                    auth = acct.jwt,
                                ),
                                onSuccess = onSuccess,
                            )
                        }
                    },
                )
                if (loading) {
                    LoadingBar()
                }
            }
        },
        content = { padding ->
            EditPostBody(
                name = name,
                onNameChange = { name = it },
                body = body,
                onBodyChange = { body = it },
                url = url,
                onUrlChange = { url = it },
                formValid = { formValid = it },
                onPickedImage = { uri ->
                    val imageIs = imageInputStreamFromUri(ctx, uri)
                    scope.launch {
                        account?.also { acct ->
                            url = uploadPictrsImage(acct, imageIs, ctx).orEmpty()
                        }
                    }
                },
                account = account,
                modifier = Modifier
                    .padding(padding)
                    .imePadding(),
            )
        },
    )
}
