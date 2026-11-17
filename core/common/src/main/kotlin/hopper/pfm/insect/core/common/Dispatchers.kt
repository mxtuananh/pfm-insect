package hopper.pfm.insect.core.common

import kotlinx.coroutines.CoroutineDispatcher

/** Injectable dispatchers so tests can replace them. */
interface DispatcherProvider {
    val io: CoroutineDispatcher
    val default: CoroutineDispatcher
    val main: CoroutineDispatcher
}
