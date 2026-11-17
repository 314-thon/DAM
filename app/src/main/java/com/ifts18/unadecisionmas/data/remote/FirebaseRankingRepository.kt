package com.ifts18.unadecisionmas.data.remote

import android.content.Context
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.ifts18.unadecisionmas.BuildConfig
import com.ifts18.unadecisionmas.R
import com.ifts18.unadecisionmas.data.local.GameDao
import com.ifts18.unadecisionmas.data.local.LocalScoreEntity
import com.ifts18.unadecisionmas.data.model.RankingEntry
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import java.util.UUID

/**
 * Repositorio de Ranking compartido con Firebase Realtime Database autenticado,
 * reglas de seguridad por UID, sincronización automática y respaldo offline en Room.
 */
class FirebaseRankingRepository(
    private val context: Context,
    private val gameDao: GameDao
) {
    private val appContext = context.applicationContext
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val _rankingList = MutableStateFlow<List<RankingEntry>>(emptyList())
    val rankingList: StateFlow<List<RankingEntry>> = _rankingList.asStateFlow()

    private val _isOnlineSynced = MutableStateFlow(false)
    val isOnlineSynced: StateFlow<Boolean> = _isOnlineSynced.asStateFlow()

    private val _connectionStatusMessage = MutableStateFlow("Conectando con ranking Firebase...")
    val connectionStatusMessage: StateFlow<String> = _connectionStatusMessage.asStateFlow()

    private var firebaseDb: FirebaseDatabase? = null
    private var firebaseAuth: FirebaseAuth? = null
    private var rankingRef: DatabaseReference? = null
    private var valueListener: ValueEventListener? = null

    @Volatile
    private var latestRemoteEntries: List<RankingEntry> = emptyList()

    @Volatile
    private var latestLocalEntries: List<RankingEntry> = emptyList()

    private val authStateListener = FirebaseAuth.AuthStateListener { auth ->
        if (auth.currentUser != null) {
            attachRealtimeListener()
            scope.launch { refreshRanking() }
        } else {
            detachRealtimeListener()
            latestRemoteEntries = emptyList()
            _isOnlineSynced.value = false
            _connectionStatusMessage.value = "Iniciá sesión para sincronizar con Firebase"
            publishMergedRanking()
        }
    }

    init {
        initFirebaseSdkIfConfigured()
        observeLocalScores()
    }

    private fun initFirebaseSdkIfConfigured() {
        firebaseAuth = FirebaseInitializer.getAuthOrNull(appContext)
        firebaseDb = FirebaseInitializer.getDatabaseOrNull(appContext)
        firebaseAuth?.addAuthStateListener(authStateListener)

        if (firebaseAuth?.currentUser != null) {
            attachRealtimeListener()
        } else {
            _isOnlineSynced.value = false
            _connectionStatusMessage.value = "Iniciá sesión para sincronizar con Firebase"
        }
    }

    private fun attachRealtimeListener() {
        if (valueListener != null) return
        val db = firebaseDb ?: FirebaseInitializer.getDatabaseOrNull(appContext)?.also { firebaseDb = it } ?: return
        val currentUser = firebaseAuth?.currentUser ?: return
        if (currentUser.uid.isBlank()) return

        try {
            val ref = db.getReference("ranking_juego_limpio")
            val listener = object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    scope.launch {
                        try {
                            val remoteEntries = mutableListOf<RankingEntry>()
                            for (child in snapshot.children) {
                                val map = child.value as? Map<*, *> ?: continue
                                remoteEntries.add(
                                    RankingEntry(
                                        id = child.key ?: UUID.randomUUID().toString(),
                                        uid = (map["uid"] as? String).orEmpty(),
                                        playerName = (map["playerName"] as? String) ?: "Jugador",
                                        avatarIndex = ((map["avatarIndex"] as? Number)?.toInt()) ?: 1,
                                        score = ((map["score"] as? Number)?.toInt()) ?: 0,
                                        debt = ((map["debt"] as? Number)?.toInt()) ?: 0,
                                        savings = ((map["savings"] as? Number)?.toInt()) ?: 15000,
                                        wellbeingPoints = ((map["wellbeingPoints"] as? Number)?.toInt()) ?: 0,
                                        totalTimeMillis = ((map["totalTimeMillis"] as? Number)?.toLong()) ?: 30000L,
                                        achievementsCount = ((map["achievementsCount"] as? Number)?.toInt()) ?: 0,
                                        hasSixSeven = (map["hasSixSeven"] as? Boolean) ?: false,
                                        gameMode = (map["gameMode"] as? String) ?: "La Semana",
                                        timestamp = ((map["timestamp"] as? Number)?.toLong()) ?: System.currentTimeMillis(),
                                        isSynced = true
                                    )
                                )
                            }
                            latestRemoteEntries = remoteEntries
                            _isOnlineSynced.value = true
                            _connectionStatusMessage.value = "Firebase en tiempo real activo"
                            publishMergedRanking()
                        } catch (_: Exception) {}
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    _isOnlineSynced.value = false
                    _connectionStatusMessage.value = when (error.code) {
                        DatabaseError.PERMISSION_DENIED -> "Ranking local Room (requiere sesión activa)"
                        else -> "Ranking local Room activo (Firebase sin conexión)"
                    }
                }
            }
            ref.addValueEventListener(listener)
            rankingRef = ref
            valueListener = listener
        } catch (_: Exception) {}
    }

    private fun detachRealtimeListener() {
        val listener = valueListener
        if (listener != null) {
            runCatching { rankingRef?.removeEventListener(listener) }
        }
        valueListener = null
        rankingRef = null
    }

    private fun observeLocalScores() {
        scope.launch {
            gameDao.observeTopScores().collect { localEntities ->
                latestLocalEntries = localEntities.map { it.toRankingEntry() }
                publishMergedRanking()
            }
        }
        scope.launch {
            refreshFromFirebaseRestFallback()
        }
    }

    /**
     * Registra un nuevo puntaje:
     * 1) Lo guarda inmediatamente en Room/SQLite local.
     * 2) Lo envía a Firebase Realtime Database autenticado (SDK o REST con ID token).
     */
    suspend fun submitScore(entry: RankingEntry) = withContext(Dispatchers.IO) {
        val activeUid = firebaseAuth?.currentUser?.uid.orEmpty()
        val modeSlug = entry.gameMode.trim().lowercase()
            .replace(Regex("[^a-z0-9]+"), "_")
            .trim('_')
            .ifBlank { "general" }
        val remoteId = entry.id.ifBlank {
            if (activeUid.isNotBlank()) "${activeUid}_$modeSlug" else UUID.randomUUID().toString()
        }
        val entryToSave = entry.copy(id = remoteId, uid = activeUid)

        val localId = gameDao.insertScore(
            LocalScoreEntity(
                remoteId = remoteId,
                playerName = entryToSave.playerName,
                avatarIndex = entryToSave.avatarIndex,
                score = entryToSave.score,
                debt = entryToSave.debt,
                savings = entryToSave.savings,
                wellbeingPoints = entryToSave.wellbeingPoints,
                totalTimeMillis = entryToSave.totalTimeMillis,
                achievementsCount = entryToSave.achievementsCount,
                hasSixSeven = entryToSave.hasSixSeven,
                gameMode = entryToSave.gameMode,
                timestamp = entryToSave.timestamp,
                isSynced = false
            )
        )

        // Si ya existe un puntaje remoto mejor para este mismo ID (mismo usuario y modo), conservamos el récord
        val existingRemote = latestRemoteEntries.find { it.id == remoteId }
        val bestToUpload = if (existingRemote != null && !isBetterRankingEntry(entryToSave, existingRemote)) {
            existingRemote
        } else {
            entryToSave
        }

        val uploaded = pushToFirebase(bestToUpload)
        if (uploaded) {
            gameDao.markScoreSynced(localId)
            _isOnlineSynced.value = true
            _connectionStatusMessage.value = "Sincronizado con Firebase Cloud"
        } else {
            _isOnlineSynced.value = false
            _connectionStatusMessage.value = if (activeUid.isBlank()) {
                "Guardado en Room (iniciá sesión para subir)"
            } else {
                "Guardado en Room (pendiente de red Firebase)"
            }
        }
        refreshFromFirebaseRestFallback()
    }

    suspend fun refreshRanking() = withContext(Dispatchers.IO) {
        val currentUser = firebaseAuth?.currentUser
        if (currentUser != null) {
            attachRealtimeListener()
            val unsynced = gameDao.getUnsyncedScores()
            for (item in unsynced) {
                val entry = item.toRankingEntry().copy(uid = currentUser.uid)
                if (pushToFirebase(entry)) {
                    gameDao.markScoreSynced(item.id)
                }
            }
        }
        refreshFromFirebaseRestFallback()
    }

    private suspend fun pushToFirebase(entry: RankingEntry): Boolean = withContext(Dispatchers.IO) {
        val currentUser = firebaseAuth?.currentUser ?: return@withContext false
        val uid = currentUser.uid
        val sanitizedName = entry.playerName.trim().ifBlank { "Jugador" }.take(30)
        val safeAvatar = entry.avatarIndex.coerceIn(1, 5)
        val safeScore = entry.score.coerceAtLeast(0)
        val safeDebt = entry.debt.coerceAtLeast(0)
        val safeTime = entry.totalTimeMillis.coerceAtLeast(0L)
        val safeAchievements = entry.achievementsCount.coerceAtLeast(0)

        // 1. Intentar vía SDK autenticado de Firebase
        try {
            val db = firebaseDb ?: FirebaseInitializer.getDatabaseOrNull(appContext)?.also { firebaseDb = it }
            if (db != null) {
                val payload = mapOf(
                    "uid" to uid,
                    "playerName" to sanitizedName,
                    "avatarIndex" to safeAvatar,
                    "score" to safeScore,
                    "debt" to safeDebt,
                    "savings" to entry.savings,
                    "wellbeingPoints" to entry.wellbeingPoints,
                    "totalTimeMillis" to safeTime,
                    "achievementsCount" to safeAchievements,
                    "hasSixSeven" to entry.hasSixSeven,
                    "gameMode" to entry.gameMode,
                    "timestamp" to entry.timestamp
                )
                db.getReference("ranking_juego_limpio")
                    .child(entry.id)
                    .setValue(payload)
                    .awaitTask()
                return@withContext true
            }
        } catch (_: Exception) {}

        // 2. Respaldo vía endpoint REST autenticado con ID Token
        try {
            val baseUrl = BuildConfig.FIREBASE_DB_URL.trimEnd('/')
            val idToken = currentUser.getIdToken(false).awaitTask().token
            if (baseUrl.startsWith("https://") && !idToken.isNullOrBlank()) {
                val url = URL("$baseUrl/ranking_juego_limpio/${entry.id}.json?auth=$idToken")
                val conn = (url.openConnection() as HttpURLConnection).apply {
                    requestMethod = "PUT"
                    connectTimeout = 4000
                    readTimeout = 4000
                    doOutput = true
                    setRequestProperty("Content-Type", "application/json; charset=UTF-8")
                }
                val json = JSONObject().apply {
                    put("uid", uid)
                    put("playerName", sanitizedName)
                    put("avatarIndex", safeAvatar)
                    put("score", safeScore)
                    put("debt", safeDebt)
                    put("savings", entry.savings)
                    put("wellbeingPoints", entry.wellbeingPoints)
                    put("totalTimeMillis", safeTime)
                    put("achievementsCount", safeAchievements)
                    put("hasSixSeven", entry.hasSixSeven)
                    put("gameMode", entry.gameMode)
                    put("timestamp", entry.timestamp)
                }
                OutputStreamWriter(conn.outputStream, Charsets.UTF_8).use { writer ->
                    writer.write(json.toString())
                }
                val code = conn.responseCode
                conn.disconnect()
                if (code in 200..299) {
                    return@withContext true
                }
            }
        } catch (_: Exception) {}

        false
    }

    private suspend fun refreshFromFirebaseRestFallback() = withContext(Dispatchers.IO) {
        try {
            val currentUser = firebaseAuth?.currentUser ?: return@withContext
            val baseUrl = BuildConfig.FIREBASE_DB_URL.trimEnd('/')
            if (!baseUrl.startsWith("https://")) return@withContext

            val idToken = currentUser.getIdToken(false).awaitTask().token ?: return@withContext
            val url = URL("$baseUrl/ranking_juego_limpio.json?auth=$idToken")
            val conn = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 4000
                readTimeout = 4000
            }
            val code = conn.responseCode
            if (code in 200..299) {
                val body = conn.inputStream.bufferedReader().use { it.readText() }
                conn.disconnect()
                val fetched = mutableListOf<RankingEntry>()
                if (body.isNotBlank() && body != "null") {
                    val root = JSONObject(body)
                    val keys = root.keys()
                    while (keys.hasNext()) {
                        val key = keys.next()
                        val obj = root.optJSONObject(key) ?: continue
                        fetched.add(
                            RankingEntry(
                                id = key,
                                uid = obj.optString("uid", ""),
                                playerName = obj.optString("playerName", "Jugador"),
                                avatarIndex = obj.optInt("avatarIndex", 1),
                                score = obj.optInt("score", 0),
                                debt = obj.optInt("debt", 0),
                                savings = obj.optInt("savings", 15000),
                                wellbeingPoints = obj.optInt("wellbeingPoints", 0),
                                totalTimeMillis = obj.optLong("totalTimeMillis", 30000L),
                                achievementsCount = obj.optInt("achievementsCount", 0),
                                hasSixSeven = obj.optBoolean("hasSixSeven", false),
                                gameMode = obj.optString("gameMode", "La Semana"),
                                timestamp = obj.optLong("timestamp", System.currentTimeMillis()),
                                isSynced = true
                            )
                        )
                    }
                }
                latestRemoteEntries = fetched
                _isOnlineSynced.value = true
                _connectionStatusMessage.value = "Firebase Cloud conectado en vivo"
                publishMergedRanking()
                return@withContext
            }
            conn.disconnect()
        } catch (_: Exception) {}
    }

    private fun publishMergedRanking() {
        val byId = LinkedHashMap<String, RankingEntry>()
        for (local in latestLocalEntries) {
            val key = local.id.ifBlank { "${local.playerName}_${local.score}_${local.totalTimeMillis}" }
            byId[key] = local
        }
        for (remote in latestRemoteEntries) {
            val key = remote.id.ifBlank { "${remote.playerName}_${remote.score}_${remote.totalTimeMillis}" }
            byId[key] = remote
        }

        // Deduplicación por apodo normalizado (case-insensitive):
        // Si alguien desinstaló la app y volvió a entrar rápido con el mismo apodo (otro UID),
        // la tabla muestra una sola fila por apodo conservando su mejor marca.
        val dedupByNickname = LinkedHashMap<String, RankingEntry>()
        for (entry in byId.values) {
            val nickKey = entry.playerName.trim().replace(Regex("\\s+"), " ").lowercase()
                .ifBlank { entry.id }
            val currentBest = dedupByNickname[nickKey]
            if (currentBest == null || isBetterRankingEntry(entry, currentBest)) {
                dedupByNickname[nickKey] = entry
            }
        }

        // Ordenamiento oficial: 1° Mayor Puntaje, 2° Menor Tiempo (desempate), 3° Más Logros
        _rankingList.value = dedupByNickname.values
            .sortedWith(
                compareByDescending<RankingEntry> { it.score }
                    .thenBy { it.totalTimeMillis }
                    .thenByDescending { it.achievementsCount }
            )
            .take(30)
    }

    private fun isBetterRankingEntry(candidate: RankingEntry, current: RankingEntry): Boolean {
        if (candidate.score != current.score) return candidate.score > current.score
        if (candidate.totalTimeMillis != current.totalTimeMillis) {
            return candidate.totalTimeMillis < current.totalTimeMillis
        }
        if (candidate.achievementsCount != current.achievementsCount) {
            return candidate.achievementsCount > current.achievementsCount
        }
        return candidate.isSynced && !current.isSynced
    }

    private fun LocalScoreEntity.toRankingEntry(): RankingEntry {
        return RankingEntry(
            id = remoteId.ifBlank { "local_$id" },
            uid = firebaseAuth?.currentUser?.uid.orEmpty(),
            playerName = playerName,
            avatarIndex = avatarIndex,
            score = score,
            debt = debt,
            savings = savings,
            wellbeingPoints = wellbeingPoints,
            totalTimeMillis = totalTimeMillis,
            achievementsCount = achievementsCount,
            hasSixSeven = hasSixSeven,
            gameMode = gameMode,
            timestamp = timestamp,
            isSynced = isSynced
        )
    }

    companion object {
        fun avatarResToIndex(resId: Int): Int = when (resId) {
            R.drawable.avatar_1 -> 1
            R.drawable.avatar_2 -> 2
            R.drawable.avatar_3 -> 3
            R.drawable.avatar_4 -> 4
            R.drawable.avatar_5 -> 5
            else -> 1
        }

        fun indexToAvatarRes(index: Int): Int = when (index) {
            1 -> R.drawable.avatar_1
            2 -> R.drawable.avatar_2
            3 -> R.drawable.avatar_3
            4 -> R.drawable.avatar_4
            5 -> R.drawable.avatar_5
            else -> R.drawable.avatar_1
        }
    }
}
