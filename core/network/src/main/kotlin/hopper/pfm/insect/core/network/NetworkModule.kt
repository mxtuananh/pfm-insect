package hopper.pfm.insect.core.network

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import javax.inject.Singleton

/** Needs an ApiConfig binding, provided by the app module (base URL differs per build type). */
@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {
    @Provides
    @Singleton
    fun provideHttpClient(config: ApiConfig): HttpClient = createApiClient(OkHttp.create(), config)
}
