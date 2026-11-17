package hopper.pfm.insect.core.domain

import hopper.pfm.insect.core.common.Outcome
import hopper.pfm.insect.core.model.ServerHealth

interface HealthRepository {
    suspend fun check(): Outcome<ServerHealth>
}
