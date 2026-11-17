package com.ifts18.unadecisionmas.data.remote

import android.content.Context
import android.content.Intent
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.auth.api.signin.GoogleSignInStatusCodes
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.database.FirebaseDatabase
import com.ifts18.unadecisionmas.BuildConfig
import com.ifts18.unadecisionmas.data.model.AuthProviderType
import com.ifts18.unadecisionmas.data.model.AuthUiState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.withTimeoutOrNull

/**
 * Repositorio de autenticación con privacidad garantizada:
 * - Soporta Invitado con Apodo (Anonymous), Email/Contraseña y Google Sign-In (Play Services Auth).
 * - Nunca expone ni prellena el nombre real ni el email de Google en el ranking.
 * - Valida el apodo y obliga a definirlo antes de jugar o publicar puntajes.
 */
class AuthRepository(private val context: Context) {

    private val appContext = context.applicationContext
    private val prefs = appContext.getSharedPreferences(PREFS_AUTH, Context.MODE_PRIVATE)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val _authState = MutableStateFlow(AuthUiState())
    val authState: StateFlow<AuthUiState> = _authState.asStateFlow()

    private val _loadedProfile = MutableStateFlow<Pair<String, Int>?>(null)
    val loadedProfile: StateFlow<Pair<String, Int>?> = _loadedProfile.asStateFlow()

    init {
        restoreExistingSession()
    }

    private fun auth(): FirebaseAuth? = FirebaseInitializer.getAuthOrNull(appContext)
    private fun db(): FirebaseDatabase? = FirebaseInitializer.getDatabaseOrNull(appContext)

    fun restoreExistingSession() {
        val currentUser = auth()?.currentUser
        if (currentUser == null) {
            _authState.value = AuthUiState(isAuthenticated = false)
            return
        }

        val uid = currentUser.uid
        val provider = resolveProvider(currentUser)
        val localNickname = prefs.getString(keyNick(uid), null)?.trim().orEmpty()
        val localAvatar = prefs.getInt(keyAvatar(uid), 1).coerceIn(1, 5)

        if (localNickname.isNotBlank()) {
            _loadedProfile.value = localNickname to localAvatar
            _authState.value = AuthUiState(
                isAuthenticated = true,
                uid = uid,
                providerType = provider,
                needsNicknameSetup = false
            )
            scope.launch { syncUserProfileFromCloud(uid, provider) }
        } else {
            _authState.value = AuthUiState(
                isAuthenticated = true,
                uid = uid,
                providerType = provider,
                needsNicknameSetup = true,
                loadingProvider = provider
            )
            scope.launch { syncUserProfileFromCloud(uid, provider) }
        }
    }

    private suspend fun syncUserProfileFromCloud(uid: String, provider: AuthProviderType) {
        try {
            val snapshot = withTimeoutOrNull(6_000L) {
                db()?.getReference("users")?.child(uid)?.get()?.awaitTask()
            }
            val cloudNick = snapshot?.child("nickname")?.getValue(String::class.java)?.trim().orEmpty()
            val cloudAvatar = (snapshot?.child("avatarIndex")?.getValue(Long::class.java)?.toInt() ?: 1)
                .coerceIn(1, 5)

            if (cloudNick.isNotBlank()) {
                saveLocalProfileForUid(uid, cloudNick, cloudAvatar)
                _loadedProfile.value = cloudNick to cloudAvatar
                _authState.update {
                    if (it.uid == uid) {
                        it.copy(needsNicknameSetup = false, loadingProvider = null, providerType = provider)
                    } else it
                }
                return
            }
        } catch (_: Exception) {
        }

        val localNick = prefs.getString(keyNick(uid), null)?.trim().orEmpty()
        _authState.update {
            if (it.uid == uid) {
                it.copy(
                    needsNicknameSetup = localNick.isBlank(),
                    loadingProvider = null
                )
            } else it
        }
    }

    suspend fun signInAnonymouslyWithProfile(nickname: String, avatarIndex: Int): Boolean =
        withContext(Dispatchers.IO) {
            val cleanNick = normalizeNickname(nickname)
            val validationError = validateNickname(cleanNick)
            if (validationError != null) {
                _authState.update { it.copy(errorMessage = validationError) }
                return@withContext false
            }

            val firebaseAuth = auth()
            if (firebaseAuth == null) {
                _authState.update { it.copy(errorMessage = "Firebase no está configurado en local.properties.") }
                return@withContext false
            }

            _authState.update { it.copy(loadingProvider = AuthProviderType.ANONYMOUS, errorMessage = null) }
            try {
                val result = withTimeout(15_000L) {
                    firebaseAuth.signInAnonymously().awaitTask()
                }
                val uid = result.user?.uid ?: throw IllegalStateException("No se obtuvo UID")
                val safeAvatar = avatarIndex.coerceIn(1, 5)
                saveProfileEverywhere(uid, cleanNick, safeAvatar)
                _loadedProfile.value = cleanNick to safeAvatar
                _authState.value = AuthUiState(
                    isAuthenticated = true,
                    uid = uid,
                    providerType = AuthProviderType.ANONYMOUS,
                    needsNicknameSetup = false,
                    loadingProvider = null
                )
                true
            } catch (e: Exception) {
                _authState.update {
                    it.copy(
                        loadingProvider = null,
                        errorMessage = mapFirebaseAuthError(e)
                    )
                }
                false
            }
        }

    suspend fun signInOrRegisterWithEmail(
        email: String,
        password: String,
        isRegister: Boolean
    ): Boolean = withContext(Dispatchers.IO) {
        val cleanEmail = email.trim()
        if (cleanEmail.isBlank() || !cleanEmail.contains("@")) {
            _authState.update { it.copy(errorMessage = "Ingresá un correo válido.") }
            return@withContext false
        }
        if (password.length < 6) {
            _authState.update { it.copy(errorMessage = "La contraseña debe tener al menos 6 caracteres.") }
            return@withContext false
        }

        val firebaseAuth = auth()
        if (firebaseAuth == null) {
            _authState.update { it.copy(errorMessage = "Firebase no está configurado en local.properties.") }
            return@withContext false
        }

        _authState.update { it.copy(loadingProvider = AuthProviderType.EMAIL, errorMessage = null) }
        try {
            val result = withTimeout(15_000L) {
                if (isRegister) {
                    firebaseAuth.createUserWithEmailAndPassword(cleanEmail, password).awaitTask()
                } else {
                    firebaseAuth.signInWithEmailAndPassword(cleanEmail, password).awaitTask()
                }
            }
            val user = result.user ?: throw IllegalStateException("No se obtuvo usuario")
            handleAuthenticatedUserPrivacyFirst(user, AuthProviderType.EMAIL)
            true
        } catch (e: Exception) {
            _authState.update {
                it.copy(
                    loadingProvider = null,
                    errorMessage = mapFirebaseAuthError(e)
                )
            }
            false
        }
    }

    fun buildGoogleSignInIntent(activityContext: Context): Intent? {
        val webClientId = BuildConfig.GOOGLE_WEB_CLIENT_ID
        if (webClientId.isBlank()) {
            _authState.update {
                it.copy(
                    loadingProvider = null,
                    errorMessage = "Falta GOOGLE_WEB_CLIENT_ID en local.properties."
                )
            }
            return null
        }
        if (auth() == null) {
            _authState.update {
                it.copy(
                    loadingProvider = null,
                    errorMessage = "Firebase no está configurado en local.properties."
                )
            }
            return null
        }

        _authState.update { it.copy(loadingProvider = AuthProviderType.GOOGLE, errorMessage = null) }
        return try {
            val client = getGoogleClient(activityContext, webClientId)
            // Limpiamos selección previa en Play Services para permitir elegir cuenta sin trabas
            runCatching { client.signOut() }
            client.signInIntent
        } catch (e: Exception) {
            _authState.update {
                it.copy(
                    loadingProvider = null,
                    errorMessage = "No se pudo abrir Google Sign-In: ${e.localizedMessage ?: "error desconocido"}"
                )
            }
            null
        }
    }

    suspend fun handleGoogleSignInResult(data: Intent?): Boolean = withContext(Dispatchers.IO) {
        val firebaseAuth = auth()
        if (firebaseAuth == null) {
            _authState.update {
                it.copy(
                    loadingProvider = null,
                    errorMessage = "Firebase no está configurado."
                )
            }
            return@withContext false
        }

        if (data == null) {
            _authState.update { it.copy(loadingProvider = null, errorMessage = null) }
            return@withContext false
        }

        return@withContext try {
            val task = GoogleSignIn.getSignedInAccountFromIntent(data)
            val account = task.getResult(ApiException::class.java)
            val idToken = account.idToken
            if (idToken.isNullOrBlank()) {
                _authState.update {
                    it.copy(
                        loadingProvider = null,
                        errorMessage = "Google no devolvió un token válido. Probá entrar rápido con apodo o email."
                    )
                }
                return@withContext false
            }

            val firebaseCred = GoogleAuthProvider.getCredential(idToken, null)
            val authResult = withTimeout(15_000L) {
                firebaseAuth.signInWithCredential(firebaseCred).awaitTask()
            }
            val user = authResult.user ?: throw IllegalStateException("No se obtuvo usuario de Google")
            handleAuthenticatedUserPrivacyFirst(user, AuthProviderType.GOOGLE)
            true
        } catch (e: ApiException) {
            val msg = when (e.statusCode) {
                GoogleSignInStatusCodes.SIGN_IN_CANCELLED -> null
                GoogleSignInStatusCodes.SIGN_IN_CURRENTLY_IN_PROGRESS -> "Ya hay un inicio de sesión en curso."
                GoogleSignInStatusCodes.DEVELOPER_ERROR ->
                    "Error de configuración OAuth/SHA-1 en Google (código 10). Podés usar Rápido con Apodo o Email."
                GoogleSignInStatusCodes.NETWORK_ERROR ->
                    "Sin conexión para validar la cuenta de Google."
                else -> "Error de Google Sign-In (código ${e.statusCode}). Podés usar Rápido con Apodo o Email."
            }
            _authState.update { it.copy(loadingProvider = null, errorMessage = msg) }
            false
        } catch (e: Exception) {
            _authState.update {
                it.copy(
                    loadingProvider = null,
                    errorMessage = mapFirebaseAuthError(e)
                )
            }
            false
        }
    }

    fun cancelGoogleSignInLoading() {
        _authState.update {
            if (it.loadingProvider == AuthProviderType.GOOGLE) {
                it.copy(loadingProvider = null)
            } else it
        }
    }

    /**
     * Privacidad estricta: aunque entre con Google o Email, jamás usa displayName ni email.
     * Solo omite la pantalla de apodo si ya existe un apodo guardado explícitamente en /users/{uid} o local.
     */
    private suspend fun handleAuthenticatedUserPrivacyFirst(
        user: FirebaseUser,
        provider: AuthProviderType
    ) {
        val uid = user.uid
        var resolvedNick = prefs.getString(keyNick(uid), null)?.trim().orEmpty()
        var resolvedAvatar = prefs.getInt(keyAvatar(uid), 1).coerceIn(1, 5)

        if (resolvedNick.isBlank()) {
            try {
                val snapshot = withTimeoutOrNull(6_000L) {
                    db()?.getReference("users")?.child(uid)?.get()?.awaitTask()
                }
                val cloudNick = snapshot?.child("nickname")?.getValue(String::class.java)?.trim().orEmpty()
                val cloudAvatar = (snapshot?.child("avatarIndex")?.getValue(Long::class.java)?.toInt() ?: 1)
                    .coerceIn(1, 5)
                if (cloudNick.isNotBlank()) {
                    resolvedNick = cloudNick
                    resolvedAvatar = cloudAvatar
                    saveLocalProfileForUid(uid, resolvedNick, resolvedAvatar)
                }
            } catch (_: Exception) {}
        }

        if (resolvedNick.isNotBlank()) {
            _loadedProfile.value = resolvedNick to resolvedAvatar
            _authState.value = AuthUiState(
                isAuthenticated = true,
                uid = uid,
                providerType = provider,
                needsNicknameSetup = false,
                loadingProvider = null
            )
        } else {
            _authState.value = AuthUiState(
                isAuthenticated = true,
                uid = uid,
                providerType = provider,
                needsNicknameSetup = true,
                loadingProvider = null
            )
        }
    }

    suspend fun saveNicknameAndAvatar(nickname: String, avatarIndex: Int): Boolean =
        withContext(Dispatchers.IO) {
            val cleanNick = normalizeNickname(nickname)
            val validationError = validateNickname(cleanNick)
            if (validationError != null) {
                _authState.update { it.copy(errorMessage = validationError) }
                return@withContext false
            }

            val currentUser = auth()?.currentUser
            if (currentUser == null) {
                _authState.update { it.copy(errorMessage = "Sesión expirada. Volvé a ingresar.") }
                return@withContext false
            }

            val currentProvider = _authState.value.providerType
            _authState.update { it.copy(loadingProvider = currentProvider, errorMessage = null) }

            val safeAvatar = avatarIndex.coerceIn(1, 5)
            saveProfileEverywhere(currentUser.uid, cleanNick, safeAvatar)
            _loadedProfile.value = cleanNick to safeAvatar
            _authState.update {
                it.copy(
                    needsNicknameSetup = false,
                    loadingProvider = null,
                    errorMessage = null
                )
            }
            true
        }

    fun updateProfileIfAuthenticated(nickname: String, avatarIndex: Int) {
        val currentUser = auth()?.currentUser ?: return
        val cleanNick = normalizeNickname(nickname)
        if (validateNickname(cleanNick) != null) return
        val safeAvatar = avatarIndex.coerceIn(1, 5)
        scope.launch {
            saveProfileEverywhere(currentUser.uid, cleanNick, safeAvatar)
        }
    }

    fun clearError() {
        _authState.update { it.copy(errorMessage = null) }
    }

    fun signOut() {
        auth()?.signOut()
        runCatching {
            val webClientId = BuildConfig.GOOGLE_WEB_CLIENT_ID
            if (webClientId.isNotBlank()) {
                getGoogleClient(appContext, webClientId).signOut()
            }
        }
        _loadedProfile.value = null
        _authState.value = AuthUiState(
            isAuthenticated = false,
            uid = "",
            providerType = AuthProviderType.NONE,
            needsNicknameSetup = false,
            loadingProvider = null,
            errorMessage = null
        )
    }

    private fun getGoogleClient(context: Context, webClientId: String): GoogleSignInClient {
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(webClientId)
            .requestEmail()
            .build()
        return GoogleSignIn.getClient(context, gso)
    }

    private suspend fun saveProfileEverywhere(uid: String, nickname: String, avatarIndex: Int) {
        saveLocalProfileForUid(uid, nickname, avatarIndex)
        try {
            val payload = mapOf<String, Any>(
                "nickname" to nickname,
                "avatarIndex" to avatarIndex,
                "updatedAt" to System.currentTimeMillis()
            )
            withTimeoutOrNull(5_000L) {
                db()?.getReference("users")?.child(uid)?.updateChildren(payload)?.awaitTask()
            }
        } catch (_: Exception) {
        }
    }

    suspend fun pushAchievementToCloud(uid: String, achievementId: String, unlockedAt: Long) {
        if (uid.isBlank() || achievementId.isBlank()) return
        try {
            val currentUid = auth()?.currentUser?.uid
            if (currentUid != uid) return
            withTimeoutOrNull(5_000L) {
                db()?.getReference("users")
                    ?.child(uid)
                    ?.child("achievements")
                    ?.child(achievementId)
                    ?.setValue(unlockedAt)
                    ?.awaitTask()
            }
        } catch (_: Exception) {
        }
    }

    suspend fun fetchCloudAchievements(uid: String): Map<String, Long> {
        if (uid.isBlank()) return emptyMap()
        return try {
            val currentUid = auth()?.currentUser?.uid
            if (currentUid != uid) return emptyMap()
            val snapshot = withTimeoutOrNull(6_000L) {
                db()?.getReference("users")
                    ?.child(uid)
                    ?.child("achievements")
                    ?.get()
                    ?.awaitTask()
            } ?: return emptyMap()
            val result = mutableMapOf<String, Long>()
            for (child in snapshot.children) {
                val id = child.key ?: continue
                val ts = child.getValue(Long::class.java) ?: System.currentTimeMillis()
                result[id] = ts
            }
            result
        } catch (_: Exception) {
            emptyMap()
        }
    }

    private fun saveLocalProfileForUid(uid: String, nickname: String, avatarIndex: Int) {
        prefs.edit()
            .putString(keyNick(uid), nickname)
            .putInt(keyAvatar(uid), avatarIndex)
            .apply()
    }

    private fun resolveProvider(user: FirebaseUser): AuthProviderType {
        if (user.isAnonymous) return AuthProviderType.ANONYMOUS
        val providerIds = user.providerData.map { it.providerId }
        return when {
            providerIds.contains(GoogleAuthProvider.PROVIDER_ID) -> AuthProviderType.GOOGLE
            else -> AuthProviderType.EMAIL
        }
    }

    private fun mapFirebaseAuthError(e: Exception): String = when (e) {
        is FirebaseAuthWeakPasswordException -> "La contraseña debe tener al menos 6 caracteres."
        is FirebaseAuthInvalidCredentialsException -> "Email o contraseña incorrectos."
        is FirebaseAuthUserCollisionException -> "Ese correo ya está registrado. Probá iniciar sesión."
        is FirebaseAuthInvalidUserException -> "No existe una cuenta con ese correo."
        else -> e.localizedMessage ?: "No se pudo completar la autenticación."
    }

    private fun keyNick(uid: String) = "nick_$uid"
    private fun keyAvatar(uid: String) = "avatar_$uid"

    companion object {
        private const val PREFS_AUTH = "juego_limpio_auth_prefs"

        fun normalizeNickname(raw: String): String =
            raw.trim().replace(Regex("\\s+"), " ").take(22)

        fun validateNickname(cleanNick: String): String? {
            if (cleanNick.length < 2) {
                return "El apodo debe tener al menos 2 caracteres."
            }
            val letterOrDigitCount = cleanNick.count { it.isLetterOrDigit() }
            if (letterOrDigitCount < 2) {
                return "Usá al menos 2 letras o números en tu apodo."
            }
            return null
        }
    }
}
