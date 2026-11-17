package hopper.pfm.insect.core.data

import hopper.pfm.insect.core.common.Outcome
import hopper.pfm.insect.core.common.map
import hopper.pfm.insect.core.domain.HealthRepository
import hopper.pfm.insect.core.model.AppVersion
import hopper.pfm.insect.core.model.ServerHealth
import hopper.pfm.insect.core.network.HealthApi
import hopper.pfm.insect.core.network.HealthDto
import javax.inject.Inject

internal fun HealthDto.toModel() = ServerHealth(
    status = status,
    serverVersion = version,
    minAppVersion = AppVersion.parseOrNull(minAppVersion),
)

class HealthRepositoryImpl @Inject constructor(private val api: HealthApi) : HealthRepository {
    override suspend fun check(): Outcome<ServerHealth> = api.health().map { it.toModel() }
}
