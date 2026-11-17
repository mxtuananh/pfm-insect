package hopper.pfm.insect.core.model

/** Local state of a record relative to the server. */
enum class SyncStatus {
    /** Same as the server. */
    SYNCED,

    /** Changed locally, waiting to be pushed (shown with the sync marker in the UI). */
    PENDING,

    /** The server rejected the last push; needs the attention of the user. */
    FAILED,
}
