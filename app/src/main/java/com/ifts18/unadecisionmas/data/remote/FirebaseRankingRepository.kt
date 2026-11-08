package com.ifts18.unadecisionmas.data.remote

import android.content.Context
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
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
 * Repositorio de Ranking compartido con soporte para Firebase Realtime Database,
 * sincronización automática y respaldo local en Room ante cortes de conexión.
 */
class FirebaseRankingRepository(
    private val context: Context,
    private val gameDao: GameDao
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val _rankingList = MutableStateFlow<List<RankingEntry>>(emptyList())
    val rankingList: StateFlow<List<RankingEntry>> = _rankingList.asStateFlow()

    private val _isOnlineSynced = MutableStateFlow(false)
    val isOnlineSynced: StateFlow<Boolean> = _isOnlineSynced.asStateFlow()

    private val _connectionStatusMessage = MutableStateFlow("Conectando con ranking Firebase...")
    val connectionStatusMessage: StateFlow<String> = _connectionStatusMessage.asStateFlow()

    private var firebaseDb: FirebaseDatabase? = null

    init {
        initFirebaseSdkIfConfigured()
        observeLocalAndCloudScores()
    }

    private fun initFirebaseSdkIfConfigured() {
        try {
            val existingApps = FirebaseApp.getApps(context)
            if (existingApps.isEmpty() &&
                BuildConfig.FIREBASE_API_KEY.isNotBlank() &&
                BuildConfig.FIREBASE_APP_ID.isNotBlank()
            ) {
                val options = FirebaseOptions.Builder()
                    .setApiKey(BuildConfig.FIREBASE_API_KEY)
                    .setApplicationId(BuildConfig.FIREBASE_APP_ID)
                    .setDatabaseUrl(BuildConfig.FIREBASE_DB_URL)
                    .setProjectId(BuildConfig.FIREBASE_PROJECT_ID)
                    .build()
                FirebaseApp.initializeApp(context, options)
            }

            if (FirebaseApp.getApps(context).isNotEmpty()) {
                firebaseDb = if (BuildConfig.FIREBASE_DB_URL.isNotBlank()) {
                    FirebaseDatabase.getInstance(BuildConfig.FIREBASE_DB_URL)
                } else {
                    FirebaseDatabase.getInstance()
                }
                attachRealtimeListener()
            }
        } catch (_: Exception) {
            firebaseDb = null
        }
    }

    private fun attachRealtimeListener() {
        try {
            val ref = firebaseDb?.getReference("ranking_juego_limpio") ?: return
            ref.addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    scope.launch {
                        try {
                            val remoteEntries = mutableListOf<RankingEntry>()
                            for (child in snapshot.children) {
                                val map = child.value as? Map<*, *> ?: continue
                                remoteEntries.add(
                                    RankingEntry(
                                        id = child.key ?: UUID.randomUUID().toString(),
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
                            _isOnlineSynced.value = true
                            _connectionStatusMessage.value = "Firebase en tiempo real activo"
                            mergeAndPublish(remoteEntries)
                        } catch (_: Exception) {}
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    _isOnlineSynced.value = false
                    _connectionStatusMessage.value = "Ranking local Room activo (Firebase sin conexión)"
                }
            })
        } catch (_: Exception) {}
    }

    private fun observeLocalAndCloudScores() {
        scope.launch {
            // Observar los puntajes locales en Room continuamente
            gameDao.observeTopScores().collect { localEntities ->
                val localMapped = localEntities.map { it.toRankingEntry() }
                if (_rankingList.value.isEmpty() && localMapped.isEmpty()) {
                    seedInitialClassmatesIfEmpty()
                } else {
                    mergeAndPublish(localMapped)
                }
            }
        }
        scope.launch {
            refreshFromFirebaseRestFallback()
        }
    }

    /**
     * Registra un nuevo puntaje:
     * 1) Lo guarda inmediatamente en Room/SQLite local.
     * 2) Lo envía a Firebase Realtime Database (SDK o REST JSON).
     */
    suspend fun submitScore(entry: RankingEntry) = withContext(Dispatchers.IO) {
        val remoteId = entry.id.ifBlank { UUID.randomUUID().toString() }
        val localId = gameDao.insertScore(
            LocalScoreEntity(
                remoteId = remoteId,
                playerName = entry.playerName,
                avatarIndex = entry.avatarIndex,
                score = entry.score,
                debt = entry.debt,
                savings = entry.savings,
                wellbeingPoints = entry.wellbeingPoints,
                totalTimeMillis = entry.totalTimeMillis,
                achievementsCount = entry.achievementsCount,
                hasSixSeven = entry.hasSixSeven,
                gameMode = entry.gameMode,
                timestamp = entry.timestamp,
                isSynced = false
            )
        )

        val uploaded = pushToFirebase(entry.copy(id = remoteId))
        if (uploaded) {
            gameDao.markScoreSynced(localId)
            _isOnlineSynced.value = true
            _connectionStatusMessage.value = "Sincronizado con Firebase Cloud"
        } else {
            _isOnlineSynced.value = false
            _connectionStatusMessage.value = "Guardado en Room (pendiente de red Firebase)"
        }
        refreshFromFirebaseRestFallback()
    }

    suspend fun refreshRanking() = withContext(Dispatchers.IO) {
        // Reintentar subir puntajes pendientes
        val unsynced = gameDao.getUnsyncedScores()
        for (item in unsynced) {
            if (pushToFirebase(item.toRankingEntry())) {
                gameDao.markScoreSynced(item.id)
            }
        }
        refreshFromFirebaseRestFallback()
    }

    private suspend fun pushToFirebase(entry: RankingEntry): Boolean = withContext(Dispatchers.IO) {
        // 1. Intentar vía SDK de Firebase si está inicializado
        try {
            val db = firebaseDb
            if (db != null) {
                val payload = mapOf(
                    "playerName" to entry.playerName,
                    "avatarIndex" to entry.avatarIndex,
                    "score" to entry.score,
                    "debt" to entry.debt,
                    "savings" to entry.savings,
                    "wellbeingPoints" to entry.wellbeingPoints,
                    "totalTimeMillis" to entry.totalTimeMillis,
                    "achievementsCount" to entry.achievementsCount,
                    "hasSixSeven" to entry.hasSixSeven,
                    "gameMode" to entry.gameMode,
                    "timestamp" to entry.timestamp
                )
                db.getReference("ranking_juego_limpio").child(entry.id).setValue(payload)
                return@withContext true
            }
        } catch (_: Exception) {}

        // 2. Intentar vía endpoint REST de Firebase Realtime Database
        try {
            val baseUrl = BuildConfig.FIREBASE_DB_URL.trimEnd('/')
            if (baseUrl.startsWith("https://")) {
                val url = URL("$baseUrl/ranking_juego_limpio/${entry.id}.json")
                val conn = (url.openConnection() as HttpURLConnection).apply {
                    requestMethod = "PUT"
                    connectTimeout = 3500
                    readTimeout = 3500
                    doOutput = true
                    setRequestProperty("Content-Type", "application/json; charset=UTF-8")
                }
                val json = JSONObject().apply {
                    put("playerName", entry.playerName)
                    put("avatarIndex", entry.avatarIndex)
                    put("score", entry.score)
                    put("debt", entry.debt)
                    put("savings", entry.savings)
                    put("wellbeingPoints", entry.wellbeingPoints)
                    put("totalTimeMillis", entry.totalTimeMillis)
                    put("achievementsCount", entry.achievementsCount)
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
            val baseUrl = BuildConfig.FIREBASE_DB_URL.trimEnd('/')
            if (!baseUrl.startsWith("https://")) return@withContext

            val url = URL("$baseUrl/ranking_juego_limpio.json")
            val conn = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 3500
                readTimeout = 3500
            }
            val code = conn.responseCode
            if (code in 200..299) {
                val body = conn.inputStream.bufferedReader().use { it.readText() }
                conn.disconnect()
                if (body.isNotBlank() && body != "null") {
                    val root = JSONObject(body)
                    val fetched = mutableListOf<RankingEntry>()
                    val keys = root.keys()
                    while (keys.hasNext()) {
                        val key = keys.next()
                        val obj = root.optJSONObject(key) ?: continue
                        fetched.add(
                            RankingEntry(
                                id = key,
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
                    _isOnlineSynced.value = true
                    _connectionStatusMessage.value = "Firebase Cloud conectado en vivo"
                    mergeAndPublish(fetched)
                    return@withContext
                }
            }
            conn.disconnect()
        } catch (_: Exception) {}
    }

    private suspend fun seedInitialClassmatesIfEmpty() {
        val samplePeers = listOf(
            LocalScoreEntity(
                remoteId = "seed_evelyn",
                playerName = "Evelyn G.",
                avatarIndex = 2,
                score = 16450,
                debt = 0,
                savings = 22000,
                wellbeingPoints = 95,
                totalTimeMillis = 28400L,
                achievementsCount = 4,
                hasSixSeven = false,
                gameMode = "La Semana",
                isSynced = true
            ),
            LocalScoreEntity(
                remoteId = "seed_martin",
                playerName = "Martín A.",
                avatarIndex = 1,
                score = 15980,
                debt = 0,
                savings = 19500,
                wellbeingPoints = 88,
                totalTimeMillis = 31200L,
                achievementsCount = 4,
                hasSixSeven = true,
                gameMode = "Scroll sin Caer",
                isSynced = true
            ),
            LocalScoreEntity(
                remoteId = "seed_lucia",
                playerName = "Lucía P.",
                avatarIndex = 3,
                score = 14820,
                debt = 0,
                savings = 18000,
                wellbeingPoints = 82,
                totalTimeMillis = 34500L,
                achievementsCount = 3,
                hasSixSeven = false,
                gameMode = "Trampa del Jackpot",
                isSynced = true
            ),
            LocalScoreEntity(
                remoteId = "seed_juan",
                playerName = "Juan C.",
                avatarIndex = 4,
                score = 14100,
                debt = 0,
                savings = 16500,
                wellbeingPoints = 78,
                totalTimeMillis = 36800L,
                achievementsCount = 3,
                hasSixSeven = false,
                gameMode = "La Semana",
                isSynced = true
            ),
            LocalScoreEntity(
                remoteId = "seed_roberto",
                playerName = "Roberto A.",
                avatarIndex = 5,
                score = 13650,
                debt = 0,
                savings = 15000,
                wellbeingPoints = 75,
                totalTimeMillis = 39100L,
                achievementsCount = 2,
                hasSixSeven = false,
                gameMode = "La Semana",
                isSynced = true
            )
        )
        samplePeers.forEach { gameDao.insertScore(it) }
    }

    private fun mergeAndPublish(incoming: List<RankingEntry>) {
        val combined = (_rankingList.value + incoming)
            .associateBy { it.id.ifBlank { "${it.playerName}_${it.score}_${it.totalTimeMillis}" } }
            .values
            // Ordenamiento oficial: 1° Mayor Puntaje, 2° Menor Tiempo (desempate), 3° Más Logros
            .sortedWith(
                compareByDescending<RankingEntry> { it.score }
                    .thenBy { it.totalTimeMillis }
                    .thenByDescending { it.achievementsCount }
            )
            .take(30)
        _rankingList.value = combined
    }

    private fun LocalScoreEntity.toRankingEntry(): RankingEntry {
        return RankingEntry(
            id = remoteId.ifBlank { "local_$id" },
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
