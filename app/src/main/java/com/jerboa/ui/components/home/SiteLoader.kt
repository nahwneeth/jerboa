package com.jerboa.ui.components.home

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Login
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.jerboa.R
import com.jerboa.api.ApiState
import com.jerboa.border
import com.jerboa.db.AccountViewModel
import com.jerboa.ui.theme.MEDIUM_PADDING

@Composable
fun SiteLoader(siteViewModel: SiteViewModel) {
    val siteRes = siteViewModel.siteRes

    Scaffold { padding ->
        Box(
            modifier = Modifier
                .padding(padding)
                .padding(horizontal = 8.dp)
                .fillMaxSize(),
            contentAlignment = Alignment.Center,
        ) {
            when (siteRes) {
                is ApiState.Failure -> {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(MEDIUM_PADDING))
                                .background(MaterialTheme.colorScheme.errorContainer)
                                .padding(MEDIUM_PADDING),
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                            ) {
                                Text(
                                    text = stringResource(R.string.failed_to_retrieve_site_user_info),
                                    color = MaterialTheme.colorScheme.error,
                                )
                                Spacer(modifier = Modifier.size(ButtonDefaults.IconSpacing))
                                ElevatedButton(
                                    onClick = siteViewModel::reload,
                                    modifier = Modifier.fillMaxWidth(),
                                    elevation = ButtonDefaults.elevatedButtonElevation(0.dp),
                                    colors = ButtonDefaults.elevatedButtonColors(
                                        containerColor = MaterialTheme.colorScheme.error,
                                        contentColor = MaterialTheme.colorScheme.onError,
                                    )
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.Refresh,
                                        contentDescription = null,
                                        modifier = Modifier.size(ButtonDefaults.IconSize),
                                    )
                                    Spacer(modifier = Modifier.size(ButtonDefaults.IconSpacing))
                                    Text(stringResource(R.string.retry))
                                }
                            }
                        }

                        val accountViewModel: AccountViewModel = hiltViewModel()
                        val accounts by accountViewModel.allAccounts.observeAsState()

                        accounts?.let { accounts ->
                            Spacer(modifier = Modifier.size(ButtonDefaults.IconSpacing))
                            LazyColumn(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .border(
                                        BorderStroke(1.dp, MaterialTheme.colorScheme.onBackground),
                                        RoundedCornerShape(MEDIUM_PADDING)
                                    ),
                            ) {
                                items(accounts.size, key = { accounts[it].id }) {
                                    val account = accounts[it]
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier
                                            .padding(MEDIUM_PADDING)
                                            .padding(start = ButtonDefaults.IconSpacing),
                                    ) {
                                        Text(account.name)
                                        Spacer(modifier = Modifier.weight(1f))
                                        if (!account.current) {
                                            IconButton(onClick = {
                                                accountViewModel.setCurrent(account.id)
                                            }) {
                                                Icon(
                                                    imageVector = Icons.Outlined.Login,
                                                    contentDescription = null,
                                                )
                                            }
                                        } else {
                                            IconButton(onClick = {
                                                accountViewModel.delete(account)
                                            }) {
                                                Icon(
                                                    imageVector = Icons.Outlined.Delete,
                                                    contentDescription = null,
                                                )
                                            }

                                            IconButton(onClick = {
                                                accountViewModel.removeCurrent()
                                            }) {
                                                Icon(
                                                    imageVector = Icons.Outlined.Close,
                                                    contentDescription = null,
                                                )
                                            }
                                        }
                                    }
                                    if (it < accounts.lastIndex) {
                                        Divider()
                                    }
                                }
                            }
                        }
                    }
                }

                else -> Column {
                    Text(stringResource(R.string.loading_site_account_info))
                    LinearProgressIndicator()
                }
            }
        }
    }
}
