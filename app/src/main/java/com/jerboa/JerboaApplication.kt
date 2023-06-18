package com.jerboa

import android.app.Application
import android.content.Context
import android.content.ContextWrapper
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.lifecycle.LiveData
import androidx.lifecycle.asFlow
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.map
import com.jerboa.db.Account
import com.jerboa.db.AccountDao
import com.jerboa.db.AccountRepository
import com.jerboa.db.AppDB
import com.jerboa.db.AppSettings
import com.jerboa.db.AppSettingsDao
import com.jerboa.db.AppSettingsRepository
import com.jerboa.ui.components.common.getCurrentAccount
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.AndroidEntryPoint
import dagger.hilt.android.HiltAndroidApp
import dagger.hilt.android.components.ActivityComponent
import dagger.hilt.android.components.ViewModelComponent
import dagger.hilt.android.qualifiers.ActivityContext
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.observeOn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Singleton

@HiltAndroidApp
class JerboaApplication : Application()

@Module
@InstallIn(SingletonComponent::class)
object MainModule {
    // LiveData works only if the the modifications are done on the same Dao.
    // Since we access the Dao via the repository, the repository is made singleton.

    @Provides
    @Singleton
    fun provideAppDB(@ApplicationContext context: Context): AppDB {
        return AppDB.getDatabase(context)
    }

    @Provides
    @Singleton
    fun provideAccountRepository(database: AppDB): AccountRepository {
        return AccountRepository(database.accountDao())
    }

    @Provides
    @Singleton
    fun provideAppSettingsRepository(database: AppDB): AppSettingsRepository {
        return AppSettingsRepository(database.appSettingsDao())
    }

    @Provides
    fun provideCurrentAccount(accountRepository: AccountRepository) : LiveData<Account?> {
        return accountRepository.allAccounts.map { getCurrentAccount(it) }
    }
}
