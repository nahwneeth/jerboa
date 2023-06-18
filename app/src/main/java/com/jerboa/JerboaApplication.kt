package com.jerboa

import android.app.Application
import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.map
import com.jerboa.api.API
import com.jerboa.api.HostInfo
import com.jerboa.db.Account
import com.jerboa.db.AccountRepository
import com.jerboa.db.AppDB
import com.jerboa.db.AppSettingsRepository
import com.jerboa.ui.components.common.getCurrentAccount
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.HiltAndroidApp
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import javax.inject.Singleton

@HiltAndroidApp
class JerboaApplication : Application()

@Module
@InstallIn(SingletonComponent::class)
object MainModule {
    @Provides
    @Singleton
    fun provideHostInfo() = HostInfo()

    @Provides
    @Singleton
    fun provideAPI(hostInfo: HostInfo): API = hostInfo.api

    // LiveData works only if the the modifications are done on the same Dao.
    // Since we access the Dao via the repository, the repository is made singleton.

    @Provides
    @Singleton
    fun provideAppDB(@ApplicationContext context: Context) = AppDB.getDatabase(context)

    @Provides
    @Singleton
    fun provideAccountRepository(database: AppDB) = AccountRepository(database.accountDao())

    @Provides
    @Singleton
    fun provideAppSettingsRepository(database: AppDB) =
        AppSettingsRepository(database.appSettingsDao())

    @Provides
    fun provideCurrentAccount(accountRepository: AccountRepository): LiveData<Account?> {
        return accountRepository.allAccounts.map { getCurrentAccount(it) }
    }
}
