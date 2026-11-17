import hopper.pfm.insect.core.common.AppError
import hopper.pfm.insect.core.common.Outcome
import hopper.pfm.insect.core.domain.CheckAppSupportUseCase
import hopper.pfm.insect.core.domain.HealthRepository
import hopper.pfm.insect.core.model.AppSupport
import hopper.pfm.insect.core.model.AppVersion
import hopper.pfm.insect.core.model.ServerHealth
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class CheckAppSupportUseCaseTest {
    private fun v(t: String) = AppVersion.parseOrNull(t)!!

    private fun useCase(result: Outcome<ServerHealth>, current: String = "1.0.0") =
        CheckAppSupportUseCase(
            health = object : HealthRepository {
                override suspend fun check() = result
            },
            currentVersion = v(current),
        )

    private fun health(min: String?) =
        Outcome.Success(ServerHealth("ok", "1.0", AppVersion.parseOrNull(min)))

    @Test fun supportedWhenNoMinimum() = runTest {
        assertEquals(AppSupport.Supported, useCase(health(null))())
    }

    @Test fun supportedWhenNewEnough() = runTest {
        assertEquals(AppSupport.Supported, useCase(health("1.0.0"))())
    }

    @Test fun updateRequiredWhenOlder() = runTest {
        assertEquals(AppSupport.UpdateRequired(v("1.1.0")), useCase(health("1.1.0"))())
    }

    @Test fun supportedWhenOffline() = runTest {
        assertEquals(AppSupport.Supported, useCase(Outcome.Failure(AppError.Offline))())
    }
}
