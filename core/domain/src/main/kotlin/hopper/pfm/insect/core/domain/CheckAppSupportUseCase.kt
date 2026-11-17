package hopper.pfm.insect.core.domain

import hopper.pfm.insect.core.common.Outcome
import hopper.pfm.insect.core.model.AppSupport
import hopper.pfm.insect.core.model.AppVersion

/**
 * Decides at start-up whether this app version is still supported. The app is offline-first, so when the server
 * cannot be reached (or does not publish minAppVersion) the app is treated as supported.
 */
class CheckAppSupportUseCase(
    private val health: HealthRepository,
    private val currentVersion: AppVersion,
) {
    suspend operator fun invoke(): AppSupport {
        val result = health.check()
        val min = (result as? Outcome.Success)?.value?.minAppVersion ?: return AppSupport.Supported
        return if (currentVersion < min) AppSupport.UpdateRequired(min) else AppSupport.Supported
    }
}
