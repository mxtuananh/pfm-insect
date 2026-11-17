package hopper.pfm.insect.core.network

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import javax.inject.Singleton

/** Needs ApiConfig (provided by the app module: the base URL differs per build type) and a TokenProvider (core:data). */
@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {
    @Provides
    @Singleton
    fun provideHttpClient(config: ApiConfig, tokens: TokenProvider): HttpClient =
        createApiClient(OkHttp.create(), config) { installBearerAuth(tokens) }
}
