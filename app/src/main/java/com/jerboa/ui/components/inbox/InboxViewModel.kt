package com.jerboa.ui.components.inbox

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asFlow
import androidx.lifecycle.viewModelScope
import com.jerboa.VoteType
import com.jerboa.api.API
import com.jerboa.api.ApiState
import com.jerboa.api.apiWrapper
import com.jerboa.appendData
import com.jerboa.datatypes.types.BlockCommunity
import com.jerboa.datatypes.types.BlockCommunityResponse
import com.jerboa.datatypes.types.BlockPerson
import com.jerboa.datatypes.types.BlockPersonResponse
import com.jerboa.datatypes.types.CommentReply
import com.jerboa.datatypes.types.CommentReplyView
import com.jerboa.datatypes.types.CommentResponse
import com.jerboa.datatypes.types.CommentSortType
import com.jerboa.datatypes.types.CreateCommentLike
import com.jerboa.datatypes.types.GetPersonMentions
import com.jerboa.datatypes.types.GetPersonMentionsResponse
import com.jerboa.datatypes.types.GetPrivateMessages
import com.jerboa.datatypes.types.GetReplies
import com.jerboa.datatypes.types.GetRepliesResponse
import com.jerboa.datatypes.types.MarkAllAsRead
import com.jerboa.datatypes.types.MarkCommentReplyAsRead
import com.jerboa.datatypes.types.MarkPersonMentionAsRead
import com.jerboa.datatypes.types.MarkPrivateMessageAsRead
import com.jerboa.datatypes.types.Person
import com.jerboa.datatypes.types.PersonMentionResponse
import com.jerboa.datatypes.types.PersonMentionView
import com.jerboa.datatypes.types.PrivateMessageResponse
import com.jerboa.datatypes.types.PrivateMessageView
import com.jerboa.datatypes.types.PrivateMessagesResponse
import com.jerboa.datatypes.types.SaveComment
import com.jerboa.db.Account
import com.jerboa.findAndUpdateCommentReply
import com.jerboa.findAndUpdateMention
import com.jerboa.findAndUpdatePersonMention
import com.jerboa.findAndUpdatePrivateMessage
import com.jerboa.nav.Initializable
import com.jerboa.newVote
import com.jerboa.serializeToMap
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.cancellable
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.launch
import javax.inject.Inject

data class InboxFilter(
    val unreadOnly: Boolean = true,
    val repliesPage: Int = 1,
    val mentionsPage: Int = 1,
    val messagesPage: Int = 1,
)

@HiltViewModel
class InboxViewModel @Inject constructor(
    val account: LiveData<Account?>,
    private val api: API,
): ViewModel() {
    val filter = MutableStateFlow(InboxFilter())

    fun updateUnreadOnly(unreadOnly: Boolean) {
        viewModelScope.launch {
            filter.emit(InboxFilter().copy(unreadOnly = unreadOnly))
        }
    }

    var repliesRes: List<CommentReplyView> by mutableStateOf(listOf())
        private set

    var fetchingMoreReplies: ApiState<GetRepliesResponse> by mutableStateOf(ApiState.Empty)
        private set

    init {
        viewModelScope.launch {
            combine(
                account.asFlow().filterNotNull(),
                filter.distinctUntilChanged { a, b ->
                    a.unreadOnly == b.unreadOnly && a.repliesPage == b.repliesPage
                },
            ) { account, filter ->
                loadReplies(account, filter)
            }.collect()
        }
    }

    private suspend fun loadReplies(account: Account, filter: InboxFilter) {
        val form = GetReplies(
            unread_only = filter.unreadOnly,
            sort = CommentSortType.New,
            page = filter.repliesPage,
            auth = account.jwt,
        );

        fetchingMoreReplies = ApiState.Loading

        val response = apiWrapper(api.getReplies(form.serializeToMap()))
        fetchingMoreReplies = response

        when (response) {
            is ApiState.Success -> {
                repliesRes = appendData(repliesRes, response.data.replies)
            }
            else -> {}
        }
    }

    fun reloadReplies() {
        viewModelScope.launch {
            filter.emit(filter.value.copy(repliesPage = 1))
        }
    }

    fun nextRepliesPage() {
        viewModelScope.launch {
            when (fetchingMoreReplies) {
                is ApiState.Failure -> {
                    account.value?.let {
                        loadReplies(account = it, filter = filter.value)
                    }
                }
                is ApiState.Loading -> {}
                else -> filter.emit(
                    filter.value.let {
                        it.copy(repliesPage = it.repliesPage + 1)
                    }
                )
            }
        }
    }

    var mentionsRes: List<PersonMentionView> by mutableStateOf(listOf())
        private set

    var fetchingMoreMentions: ApiState<GetPersonMentionsResponse> by mutableStateOf(ApiState.Empty)
        private set

    init {
        viewModelScope.launch {
            combine(
                account.asFlow().filterNotNull(),
                filter.distinctUntilChanged { a, b ->
                    a.unreadOnly == b.unreadOnly && a.mentionsPage == b.mentionsPage
                },
            ) { account, filter ->
                loadMentions(account, filter)
            }.collect()
        }
    }

    private suspend fun loadMentions(account: Account, filter: InboxFilter) {
        val form = GetReplies(
            unread_only = filter.unreadOnly,
            sort = CommentSortType.New,
            page = filter.mentionsPage,
            auth = account.jwt,
        );

        fetchingMoreMentions = ApiState.Loading

        val response = apiWrapper(api.getPersonMentions(form.serializeToMap()))
        fetchingMoreMentions = response

        when (response) {
            is ApiState.Success -> {
                mentionsRes = appendData(mentionsRes, response.data.mentions)
            }
            else -> {}
        }
    }

    fun reloadMentions() {
        viewModelScope.launch {
            filter.emit(filter.value.copy(mentionsPage = 1))
        }
    }

    fun nextMentionsPage() {
        viewModelScope.launch {
            when (fetchingMoreMentions) {
                is ApiState.Failure -> {
                    account.value?.let {
                        loadMentions(account = it, filter = filter.value)
                    }
                }
                is ApiState.Loading -> {}
                else -> filter.emit(
                    filter.value.let {
                        it.copy(mentionsPage = it.mentionsPage + 1)
                    }
                )
            }
        }
    }

    var messagesRes: List<PrivateMessageView> by mutableStateOf(listOf())
        private set

    var fetchingMoreMessages: ApiState<PrivateMessagesResponse> by mutableStateOf(ApiState.Empty)
        private set

    init {
        viewModelScope.launch {
            combine(
                account.asFlow().filterNotNull(),
                filter.distinctUntilChanged { a, b ->
                    a.unreadOnly == b.unreadOnly && a.messagesPage == b.messagesPage
                },
            ) { account, filter ->
                loadMessages(account, filter)
            }.collect()
        }
    }

    private suspend fun loadMessages(account: Account, filter: InboxFilter) {
        val form = GetReplies(
            unread_only = filter.unreadOnly,
            sort = CommentSortType.New,
            page = filter.messagesPage,
            auth = account.jwt,
        );

        fetchingMoreMessages = ApiState.Loading

        val response = apiWrapper(api.getPrivateMessages(form.serializeToMap()))
        fetchingMoreMessages = response

        when (response) {
            is ApiState.Success -> {
                messagesRes = appendData(messagesRes, response.data.private_messages)
            }
            else -> {}
        }
    }

    fun reloadMessages() {
        viewModelScope.launch {
            filter.emit(filter.value.copy(messagesPage = 1))
        }
    }

    fun nextMessagesPage() {
        viewModelScope.launch {
            when (fetchingMoreMessages) {
                is ApiState.Failure -> {
                    account.value?.let {
                        loadMentions(account = it, filter = filter.value)
                    }
                }
                is ApiState.Loading -> {}
                else -> filter.emit(
                    filter.value.let {
                        it.copy(messagesPage = it.messagesPage + 1)
                    }
                )
            }
        }
    }

    private var likeReplyRes: ApiState<CommentResponse> by mutableStateOf(ApiState.Empty)

    private var saveReplyRes: ApiState<CommentResponse> by mutableStateOf(ApiState.Empty)

    private var likeMentionRes: ApiState<CommentResponse> by mutableStateOf(ApiState.Empty)

    private var saveMentionRes: ApiState<CommentResponse> by mutableStateOf(ApiState.Empty)

    var markReplyAsReadRes: ApiState<CommentResponse> by mutableStateOf(ApiState.Empty)

    var markMentionAsReadRes: ApiState<PersonMentionResponse> by mutableStateOf(ApiState.Empty)

    var markMessageAsReadRes: ApiState<PrivateMessageResponse> by mutableStateOf(ApiState.Empty)

    var markAllAsReadRes: ApiState<GetRepliesResponse> by mutableStateOf(ApiState.Empty)

    var blockCommunityRes: ApiState<BlockCommunityResponse> by
        mutableStateOf(ApiState.Empty)

    var blockPersonRes: ApiState<BlockPersonResponse> by
        mutableStateOf(ApiState.Empty)

    fun likeReply(commentReplyView: CommentReplyView, voteType: VoteType): Boolean {
        val jwt = account.value?.jwt ?: return false
        viewModelScope.launch {
            val form = CreateCommentLike(
                comment_id = commentReplyView.comment.id,
                score = newVote(commentReplyView.my_vote, voteType),
                auth = jwt,
            )
            likeReplyRes = ApiState.Loading
            likeReplyRes = apiWrapper(api.likeComment(form))
            when (val likeRes = likeReplyRes) {
                is ApiState.Success -> {
                    repliesRes = findAndUpdateCommentReply(repliesRes, likeRes.data.comment_view)
                }
                else -> {}
            }
        }
        return true
    }

    fun saveReply(commentReplyView: CommentReplyView): Boolean {
        val jwt = account.value?.jwt ?: return false
        viewModelScope.launch {
            val form = SaveComment(
                comment_id = commentReplyView.comment.id,
                save = !commentReplyView.saved,
                auth = jwt,
            )
            saveReplyRes = ApiState.Loading
            saveReplyRes = apiWrapper(api.saveComment(form))
            when (val saveRes = saveReplyRes) {
                is ApiState.Success -> {
                    repliesRes = findAndUpdateCommentReply(repliesRes, saveRes.data.comment_view)
                }
                else -> {}
            }
        }
        return true
    }

    fun likeMention(personMentionView: PersonMentionView, voteType: VoteType): Boolean {
        val jwt = account.value?.jwt ?: return false
        viewModelScope.launch {
            val form = CreateCommentLike(
                comment_id = personMentionView.comment.id,
                score = newVote(personMentionView.my_vote, voteType),
                auth = jwt,
            )
            likeMentionRes = ApiState.Loading
            likeMentionRes = apiWrapper(api.likeComment(form))
            when (val likeRes = likeMentionRes) {
                is ApiState.Success -> {
                    mentionsRes = findAndUpdatePersonMention(
                        mentionsRes,
                        likeRes.data.comment_view,
                    )
                }
                else -> {}
            }
        }
        return true
    }

    fun saveMention(personMentionView: PersonMentionView): Boolean {
        val jwt = account.value?.jwt ?: return false
        viewModelScope.launch {
            val form = SaveComment(
                comment_id = personMentionView.comment.id,
                save = !personMentionView.saved,
                auth = jwt,
            )
            saveMentionRes = ApiState.Loading
            saveMentionRes = apiWrapper(api.saveComment(form))
            when (val saveRes = saveMentionRes) {
                is ApiState.Success -> {
                    mentionsRes = findAndUpdatePersonMention(
                        mentionsRes,
                        saveRes.data.comment_view,
                    )
                }
                else -> {}
            }
        }
        return true
    }

    fun markReplyAsRead(commentReplyView: CommentReplyView): Boolean {
        val jwt = account.value?.jwt ?: return false
        viewModelScope.launch {
            val form = MarkCommentReplyAsRead(
                comment_reply_id = commentReplyView.comment_reply.id,
                read = !commentReplyView.comment_reply.read,
                auth = jwt,
            )

            markReplyAsReadRes = ApiState.Loading
            markReplyAsReadRes = apiWrapper(api.markCommentReplyAsRead(form))

            when (val readRes = markReplyAsReadRes) {
                is ApiState.Success -> {
                    val mutable = repliesRes.toMutableList()
                    val foundIndex = mutable.indexOfFirst {
                        it.comment_reply.comment_id == readRes.data.comment_view.comment.id
                    }
                    if (foundIndex != -1) {
                        val cr = mutable[foundIndex].comment_reply
                        val newCr = cr.copy(read = !cr.read)
                        mutable[foundIndex] = mutable[foundIndex].copy(comment_reply = newCr)
                        repliesRes = mutable.toList()
                    }
                }
                else -> {}
            }
        }
        return true
    }

    fun markPersonMentionAsRead(personMentionView: PersonMentionView): Boolean {
        val jwt = account.value?.jwt ?: return false
        viewModelScope.launch {
            val form = MarkPersonMentionAsRead(
                person_mention_id = personMentionView.person_mention.id,
                read = !personMentionView.person_mention.read,
                auth = jwt,
            )

            markMentionAsReadRes = ApiState.Loading
            markMentionAsReadRes = apiWrapper(api.markPersonMentionAsRead(form))

            when (val readRes = markMentionAsReadRes) {
                is ApiState.Success -> {
                    mentionsRes = findAndUpdateMention(
                        mentionsRes,
                        readRes.data.person_mention_view,
                    )
                }
                else -> {}
            }
        }
        return true
    }

    fun markPrivateMessageAsRead(privateMessageView: PrivateMessageView): Boolean {
        val jwt = account.value?.jwt ?: return false
        viewModelScope.launch {
            val form = MarkPrivateMessageAsRead(
                private_message_id = privateMessageView.private_message.id,
                read = !privateMessageView.private_message.read,
                auth = jwt,
            )

            markMessageAsReadRes = ApiState.Loading
            markMessageAsReadRes = apiWrapper(api.markPrivateMessageAsRead(form))

            when (val readRes = markMessageAsReadRes) {
                is ApiState.Success -> {
                    messagesRes =findAndUpdatePrivateMessage(
                        messagesRes,
                        readRes.data.private_message_view,
                    )
                }
                else -> {}
            }
        }
        return false
    }

    fun blockCommunity(form: BlockCommunity, ctx: Context) {
        viewModelScope.launch {
            blockCommunityRes = ApiState.Loading
            blockCommunityRes = apiWrapper(api.blockCommunity(form))
        }
    }

    fun blockPerson(person: Person): Boolean {
        val jwt = account.value?.jwt ?: return false
        viewModelScope.launch {
            val form = BlockPerson(
                person_id = person.id,
                block = true,
                auth = jwt,
            )

            blockPersonRes = ApiState.Loading
            blockPersonRes = apiWrapper(API.getInstance().blockPerson(form))
        }
        return true
    }

    fun markAllAsRead(): Boolean {
        val jwt = account.value?.jwt ?: return false
        viewModelScope.launch {
            val form = MarkAllAsRead(auth = jwt)

            markAllAsReadRes = ApiState.Loading
            markAllAsReadRes = apiWrapper(api.markAllAsRead(form))

            when (markAllAsReadRes) {
                is ApiState.Success -> {
                    repliesRes = repliesRes.let { replies ->
                        val mutable = replies.toMutableList()
                        mutable.replaceAll {
                            it.copy(comment_reply = it.comment_reply.copy(read = true))
                        }
                        mutable.toList()
                    }

                    mentionsRes = mentionsRes.let { mentions ->
                        val mutable = mentions.toMutableList()
                        mutable.replaceAll {
                            it.copy(person_mention = it.person_mention.copy(read = true))
                        }
                        mutable.toList()
                    }

                    messagesRes = messagesRes.let { messages ->
                        val mutable = messages.toMutableList()
                        mutable.replaceAll {
                            it.copy(private_message = it.private_message.copy(read = true))
                        }
                        mutable.toList()
                    }
                }
                else -> {}
            }
        }
        return true
    }
}
