package hopper.pfm.insect.core.data

import hopper.pfm.insect.core.common.AuthTokens
import hopper.pfm.insect.core.common.TokenStore
import hopper.pfm.insect.core.model.SessionState
import hopper.pfm.insect.core.network.TokenProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Holds the tokens and the session state. It is the TokenProvider of the network layer, so a refresh that the server
 * rejects ends the session (state Expired) without the UI knowing how tokens are renewed. It has no dependency on the
 * API classes, which keeps the HttpClient from depending on itself.
 */
@Singleton
class SessionManager @Inject constructor(private val store: TokenStore) : TokenProvider {

    private val _state = MutableStateFlow<SessionState>(SessionState.SignedOut)
    val state: StateFlow<SessionState> = _state.asStateFlow()

    suspend fun restore(): SessionState {
        val restored = if (store.get() != null) SessionState.SignedIn else SessionState.SignedOut
        _state.value = restored
        return restored
    }

    suspend fun signIn(tokens: AuthTokens) {
        store.save(tokens)
        _state.value = SessionState.SignedIn
    }

    suspend fun signOut() {
        store.clear()
        _state.value = SessionState.SignedOut
    }

    suspend fun currentTokens(): AuthTokens? = store.get()

    override suspend fun tokens(): AuthTokens? = store.get()

    override suspend fun save(tokens: AuthTokens) = store.save(tokens)

    override suspend fun onRefreshRejected() {
        store.clear()
        _state.value = SessionState.Expired
    }
}
