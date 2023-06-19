package com.jerboa.ui.components.home

import android.content.Context
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asFlow
import androidx.lifecycle.distinctUntilChanged
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
import com.jerboa.datatypes.types.Community
import com.jerboa.datatypes.types.CommunityView
import com.jerboa.datatypes.types.CreatePostLike
import com.jerboa.datatypes.types.DeletePost
import com.jerboa.datatypes.types.GetPost
import com.jerboa.datatypes.types.GetPosts
import com.jerboa.datatypes.types.GetPostsResponse
import com.jerboa.datatypes.types.GetUnreadCount
import com.jerboa.datatypes.types.GetUnreadCountResponse
import com.jerboa.datatypes.types.ListingType
import com.jerboa.datatypes.types.Person
import com.jerboa.datatypes.types.PostResponse
import com.jerboa.datatypes.types.PostView
import com.jerboa.datatypes.types.SavePost
import com.jerboa.datatypes.types.SortType
import com.jerboa.db.Account
import com.jerboa.dedupePosts
import com.jerboa.findAndUpdatePost
import com.jerboa.newVote
import com.jerboa.serializeToMap
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import javax.inject.Inject

val DEFAULT_SORT_TYPE = SortType.Active
val DEFAULT_LISTING_TYPE = ListingType.Local

data class PostFilter(
    val type: ListingType = DEFAULT_LISTING_TYPE,
    val sort: SortType = DEFAULT_SORT_TYPE,
    val page: Int = 1,
) {
    fun firstPage() = copy(page = 1)
    fun nextPage() = copy(page = page + 1)
    fun withSort(sort: SortType) = copy(sort = sort, page = 1)
    fun withType(type: ListingType) = copy(type = type, page = 1)
}

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val api: API,
    val account: LiveData<Account?>
) : ViewModel() {
    var unreadCountRes: ApiState<GetUnreadCountResponse> by mutableStateOf(ApiState.Empty)
        private set

    init {
        Log.d("HomeViewModel", "init")
        viewModelScope.launch {
            account.distinctUntilChanged().asFlow().collect { account ->
                loadUnreadCounts(account)
            }
        }
    }

    suspend fun reloadUnreadCounts() {
        loadUnreadCounts(account.value)
    }

    private suspend fun loadUnreadCounts(account: Account?) {
        if (account != null) {
            unreadCountRes = ApiState.Loading
            unreadCountRes = apiWrapper(
                api.getUnreadCount(
                    GetUnreadCount(auth = account.jwt).serializeToMap()
                )
            )
        } else {
            unreadCountRes = ApiState.Empty
        }
    }

    private val _filter = MutableStateFlow(
        PostFilter(
            type = account.value?.defaultListingType?.let {
                ListingType.values()[it]
            } ?: DEFAULT_LISTING_TYPE,
            sort = account.value?.defaultSortType?.let {
                SortType.values()[it]
            } ?: DEFAULT_SORT_TYPE,
        )
    )
    val filter: StateFlow<PostFilter> = _filter

    var postsRes: List<PostView> by mutableStateOf(listOf())
        private set

    var fetchingMore: ApiState<GetPostsResponse> by mutableStateOf(ApiState.Empty)
        private set

    init {
        viewModelScope.launch {
            filter.distinctUntilChanged { a, b -> a == b }
                .combine(account.asFlow()) { filter, account ->
                    Log.d("HomeViewModel", "filter & account combined flow")
                    Log.d("HomeViewModel", "filter = $filter")
                    load(filter, account?.jwt)
                }.collect()
        }
    }

    private suspend fun load(filter: PostFilter, auth: String?) {
        Log.d("HomeViewModel", "load")

        val form = GetPosts(
            type_ = filter.type,
            sort = filter.sort,
            page = filter.page,
            auth = auth
        )

        fetchingMore = ApiState.Loading

        val response = apiWrapper(api.getPosts(form.serializeToMap()))
        fetchingMore = response

        when (response) {
            is ApiState.Success -> {
                postsRes = if (filter.page == 1) {
                    response.data.posts
                } else {
                    appendData(
                        postsRes,
                        dedupePosts(
                            more = response.data.posts,
                            existing = postsRes
                        )
                    )
                }
            }
            else -> { }
        }
    }

    fun updateFilter(filter: PostFilter) {
        viewModelScope.launch { _filter.emit(filter) }
    }

    private var likePostRes: ApiState<PostResponse> by mutableStateOf(ApiState.Empty)

    private var savePostRes: ApiState<PostResponse> by mutableStateOf(ApiState.Empty)

    private var deletePostRes: ApiState<PostResponse> by mutableStateOf(ApiState.Empty)

    var blockCommunityRes: ApiState<BlockCommunityResponse> by mutableStateOf(ApiState.Empty)
        private set

    var blockPersonRes: ApiState<BlockPersonResponse> by mutableStateOf(ApiState.Empty)
        private set

    fun likePost(postView: PostView, voteType: VoteType): Boolean {
        val jwt = account.value?.jwt ?: return false
        viewModelScope.launch {
            val form = CreatePostLike(
                post_id = postView.post.id,
                score = newVote(
                    currentVote = postView.my_vote,
                    voteType = VoteType.Upvote,
                ),
                auth = jwt,
            )

            likePostRes = ApiState.Loading
            likePostRes = apiWrapper(api.likePost(form))

            when (val likeRes = likePostRes) {
                is ApiState.Success -> {
                    updatePost(likeRes.data.post_view)
                }

                else -> {}
            }
        }
        return true
    }

    fun savePost(postView: PostView): Boolean {
        val jwt = account.value?.jwt ?: return false
        viewModelScope.launch {
            val form = SavePost(
                post_id = postView.post.id,
                save = !postView.saved,
                auth = jwt,
            )

            savePostRes = ApiState.Loading
            savePostRes = apiWrapper(API.getInstance().savePost(form))
            when (val saveRes = savePostRes) {
                is ApiState.Success -> {
                    updatePost(saveRes.data.post_view)
                }

                else -> {}
            }
        }
        return true
    }

    fun deletePost(postView: PostView): Boolean {
        val jwt = account.value?.jwt ?: return false
        viewModelScope.launch {
            val form = DeletePost(
                post_id = postView.post.id,
                deleted = !postView.post.deleted,
                auth = jwt,
            )

            deletePostRes = ApiState.Loading
            deletePostRes = apiWrapper(API.getInstance().deletePost(form))
            when (val deletePost = deletePostRes) {
                is ApiState.Success -> {
                    updatePost(deletePost.data.post_view)
                }
                else -> {}
            }
        }
        return true
    }

    fun blockCommunity(community: Community): Boolean {
        val jwt = account.value?.jwt ?: return false
        viewModelScope.launch {
            val form = BlockCommunity(
                community_id = community.id,
                auth = jwt,
                block = true,
            )

            blockCommunityRes = ApiState.Loading
            blockCommunityRes = apiWrapper(api.blockCommunity(form))
        }
        return true
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
            blockPersonRes = apiWrapper(api.blockPerson(form))
        }
        return true
    }

    fun updatePost(postView: PostView) {
        postsRes = findAndUpdatePost(postsRes, postView)
    }
}
