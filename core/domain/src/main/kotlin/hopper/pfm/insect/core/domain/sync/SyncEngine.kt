package hopper.pfm.insect.core.domain.sync

/** Outcome of one full round (push everything, then pull everything). */
data class SyncResult(
    val pushed: Int = 0,
    val rejected: Int = 0,
    val applied: Int = 0,
    val problem: SyncProblem? = null,
) {
    val ok: Boolean get() = problem == null
}

/**
 * Runs the resources in dependency order: categories first (an expense may refer to a category the server does not know
 * yet), then expenses, habits and budgets. The push runs before the pull so an older server copy cannot overwrite a
 * local edit that has not been sent.
 */
class SyncEngine(private val resources: List<ResourceSyncer<*>>) {

    suspend fun run(): SyncResult {
        var pushed = 0
        var rejected = 0
        for (resource in resources) {
            val report = resource.push()
            pushed += report.pushed
            rejected += report.rejected
            // Without a connection or a session nothing else can succeed; stop early and keep everything dirty.
            if (report.problem != null) return SyncResult(pushed, rejected, problem = report.problem)
        }
        var applied = 0
        for (resource in resources) {
            val report = resource.pull()
            applied += report.applied
            if (report.problem != null) return SyncResult(pushed, rejected, applied, report.problem)
        }
        return SyncResult(pushed, rejected, applied)
    }
}
