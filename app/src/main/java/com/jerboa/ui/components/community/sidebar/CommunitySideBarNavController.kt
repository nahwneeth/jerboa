package com.jerboa.ui.components.community.sidebar

import androidx.lifecycle.ViewModel
import androidx.navigation.NavController
import com.jerboa.datatypes.types.Community
import com.jerboa.nav.NavControllerWrapper
import com.jerboa.nav.NavigateWithNoArgs
import com.jerboa.nav.NavigateWithNoArgsAndDependencies

typealias ToCommunitySideBar = NavigateWithNoArgsAndDependencies

class CommunitySideBarNavController(
    override val navController: NavController
) : NavControllerWrapper()
