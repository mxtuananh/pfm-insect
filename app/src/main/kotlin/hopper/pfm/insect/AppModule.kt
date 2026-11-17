package hopper.pfm.insect

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import hopper.pfm.insect.core.domain.CheckAppSupportUseCase
import hopper.pfm.insect.core.domain.HealthRepository
import hopper.pfm.insect.core.model.AppVersion
import hopper.pfm.insect.core.network.ApiConfig

@Module
@InstallIn(SingletonComponent::class)
object AppModule {
    @Provides
    fun provideApiConfig(): ApiConfig = ApiConfig(BuildConfig.API_BASE_URL)

    @Provides
    fun provideAppVersion(): AppVersion =
        requireNotNull(AppVersion.parseOrNull(BuildConfig.VERSION_NAME)) { "versionName must start with a number" }

    @Provides
    fun provideCheckAppSupport(health: HealthRepository, version: AppVersion) =
        CheckAppSupportUseCase(health, version)
}
