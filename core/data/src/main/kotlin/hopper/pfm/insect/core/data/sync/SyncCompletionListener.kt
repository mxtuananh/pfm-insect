package hopper.pfm.insect.core.data.sync

/** Something to do after a successful sync round, provided by the app (it is bound into a set, which may be empty). */
fun interface SyncCompletionListener {
    suspend fun onSyncCompleted()
}
