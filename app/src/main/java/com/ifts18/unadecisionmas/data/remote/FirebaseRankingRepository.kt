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
import com.ifts18.unadecisionmas.data.model.AchievementsCatalog
import com.ifts18.unadecisionmas.data.model.GameModes
import com.ifts18.unadecisionmas.data.model.PlayerMedalsRecord
import com.ifts18.unadecisionmas.data.model.PlayerRankingSummary
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
 *
 * Consolida el ranking sumando el puntaje más alto de cada modo de juego por jugador
 * y sumando por separado el puntaje único de cada medalla desbloqueada.
 */
class FirebaseRankingRepository(
    context: Context,
    private val gameDao: GameDao
) {
    private val appContext = context.applicationContext
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val _rankingList = MutableStateFlow<List<PlayerRankingSummary>>(emptyList())
    val rankingList: StateFlow<List<PlayerRankingSummary>> = _rankingList.asStateFlow()

    private val _currentUserSummary = MutableStateFlow<PlayerRankingSummary?>(null)
    val currentUserSummary: StateFlow<PlayerRankingSummary?> = _currentUserSummary.asStateFlow()

    private val _isOnlineSynced = MutableStateFlow(false)
    val isOnlineSynced: StateFlow<Boolean> = _isOnlineSynced.asStateFlow()

    private val _connectionStatusMessage = MutableStateFlow("Conectando con ranking Firebase...")
    val connectionStatusMessage: StateFlow<String> = _connectionStatusMessage.asStateFlow()

    private var firebaseDb: FirebaseDatabase? = null
    private var firebaseAuth: FirebaseAuth? = null
    private var rankingRef: DatabaseReference? = null
    private var rankingValueListener: ValueEventListener? = null
    private var medalsRef: DatabaseReference? = null
    private var medalsValueListener: ValueEventListener? = null

    @Volatile
    private var latestRemoteEntries: List<RankingEntry> = emptyList()

    @Volatile
    private var latestRemoteMedals: List<PlayerMedalsRecord> = emptyList()

    @Volatile
    private var latestLocalEntries: List<RankingEntry> = emptyList()

    @Volatile
    private var localUserUid: String = ""

    @Volatile
    private var localUserName: String = ""

    @Volatile
    private var localUserAvatarIndex: Int = 1

    @Volatile
    private var localUserMedalIds: Set<String> = emptySet()

    private val authStateListener = FirebaseAuth.AuthStateListener { auth ->
        val user = auth.currentUser
        if (user != null) {
            localUserUid = user.uid
            attachRealtimeListeners()
            scope.launch { refreshRanking() }
        } else {
            detachRealtimeListeners()
            latestRemoteEntries = emptyList()
            latestRemoteMedals = emptyList()
            localUserUid = ""
            localUserMedalIds = emptySet()
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

        val currentUser = firebaseAuth?.currentUser
        if (currentUser != null) {
            localUserUid = currentUser.uid
            attachRealtimeListeners()
        } else {
            _isOnlineSynced.value = false
            _connectionStatusMessage.value = "Iniciá sesión para sincronizar con Firebase"
        }
    }

    private fun attachRealtimeListeners() {
        val db = firebaseDb ?: FirebaseInitializer.getDatabaseOrNull(appContext)?.also { firebaseDb = it } ?: return
        val currentUser = firebaseAuth?.currentUser ?: return
        if (currentUser.uid.isBlank()) return

        if (rankingValueListener == null) {
            try {
                val ref = db.getReference(NODE_RANKING_MODES)
                val listener = object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        scope.launch {
                            try {
                                val remoteEntries = mutableListOf<RankingEntry>()
                                for (child in snapshot.children) {
                                    val map = child.value as? Map<*, *> ?: continue
                                    remoteEntries.add(parseModeEntryFromMap(child.key, map))
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
                rankingValueListener = listener
            } catch (_: Exception) {}
        }

        if (medalsValueListener == null) {
            try {
                val ref = db.getReference(NODE_RANKING_MEDALS)
                val listener = object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        scope.launch {
                            try {
                                val records = mutableListOf<PlayerMedalsRecord>()
                                for (child in snapshot.children) {
                                    val map = child.value as? Map<*, *> ?: continue
                                    parseMedalsRecordFromMap(child.key, map)?.let { records.add(it) }
                                }
                                latestRemoteMedals = records
                                publishMergedRanking()
                            } catch (_: Exception) {}
                        }
                    }

                    override fun onCancelled(error: DatabaseError) {}
                }
                ref.addValueEventListener(listener)
                medalsRef = ref
                medalsValueListener = listener
            } catch (_: Exception) {}
        }
    }

    private fun detachRealtimeListeners() {
        rankingValueListener?.let { listener ->
            runCatching { rankingRef?.removeEventListener(listener) }
        }
        rankingValueListener = null
        rankingRef = null

        medalsValueListener?.let { listener ->
            runCatching { medalsRef?.removeEventListener(listener) }
        }
        medalsValueListener = null
        medalsRef = null
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
     * Actualiza los datos de perfil y medallas del usuario activo en memoria,
     * refresca de inmediato el ranking consolidado (incluyendo modo offline)
     * y sincroniza `ranking_medallas/{uid}` con Firebase si corresponde.
     */
    fun syncLocalUserMedals(
        uid: String,
        playerName: String,
        avatarIndex: Int,
        unlockedMedalIds: Set<String>,
        allowRemotePush: Boolean
    ) {
        if (uid.isBlank()) return
        val cleanName = playerName.trim().take(30)
        val safeAvatar = avatarIndex.coerceIn(1, 5)
        val validIds = unlockedMedalIds.filter { AchievementsCatalog.findById(it) != null }.toSet()

        localUserUid = uid
        if (cleanName.isNotBlank()) {
            localUserName = cleanName
        }
        localUserAvatarIndex = safeAvatar
        localUserMedalIds = validIds
        publishMergedRanking()

        val hasRemoteMedalDoc = latestRemoteMedals.any { it.uid == uid }
        if (allowRemotePush && cleanName.isNotBlank() && (validIds.isNotEmpty() || hasRemoteMedalDoc)) {
            scope.launch {
                val record = buildPlayerMedalsRecord(uid, cleanName, safeAvatar, validIds)
                if (pushMedalsToFirebase(record)) {
                    upsertRemoteMedalInMemory(record)
                    publishMergedRanking()
                }
            }
        }
    }

    fun clearLocalUserContext() {
        localUserUid = ""
        localUserName = ""
        localUserAvatarIndex = 1
        localUserMedalIds = emptySet()
        publishMergedRanking()
    }

    /**
     * Registra un puntaje puro de un modo de juego:
     * 1) Conserva en Room únicamente la mejor marca local del usuario para ese modo.
     * 2) La sube a Firebase Realtime Database autenticado (`ranking_juego_limpio/{uid}_{modo}`).
     * 3) Sincroniza también `ranking_medallas/{uid}` si tiene medallas desbloqueadas.
     */
    suspend fun submitScore(entry: RankingEntry) = withContext(Dispatchers.IO) {
        val activeUid = firebaseAuth?.currentUser?.uid.orEmpty()
        val canonicalMode = GameModes.canonicalize(entry.gameMode)
        val modeSlug = GameModes.slug(canonicalMode)
        val remoteId = entry.id.ifBlank {
            if (activeUid.isNotBlank()) "${activeUid}_$modeSlug" else UUID.randomUUID().toString()
        }
        val entryToSave = entry.copy(
            id = remoteId,
            uid = activeUid,
            gameMode = canonicalMode,
            scoreVersion = RankingEntry.CURRENT_SCORE_VERSION
        )

        val existingLocal = gameDao.getBestScoreByRemoteId(remoteId)?.toRankingEntry()
        val existingRemote = latestRemoteEntries.find { it.id == remoteId }

        val shouldSaveLocally = existingLocal == null ||
            RankingAggregator.isBetterModeEntry(entryToSave, existingLocal)

        val candidateForRemote = if (shouldSaveLocally) {
            gameDao.deleteScoresByRemoteId(remoteId)
            val localId = gameDao.insertScore(
                LocalScoreEntity(
                    uid = activeUid,
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
                    gameMode = canonicalMode,
                    timestamp = entryToSave.timestamp,
                    isSynced = false,
                    scoreVersion = RankingEntry.CURRENT_SCORE_VERSION
                )
            )
            entryToSave to localId
        } else {
            val bestLocal = existingLocal!!
            val syncedFlagId = if (!bestLocal.isSynced) {
                gameDao.getBestScoreByRemoteId(remoteId)?.id ?: 0L
            } else 0L
            bestLocal to syncedFlagId
        }

        val (bestLocalEntry, localRowIdToMark) = candidateForRemote
        val bestToUpload = if (
            existingRemote != null &&
            !RankingAggregator.isBetterModeEntry(bestLocalEntry, existingRemote)
        ) {
            existingRemote
        } else {
            bestLocalEntry
        }

        val uploaded = pushScoreToFirebase(bestToUpload)
        if (uploaded) {
            if (localRowIdToMark > 0L) {
                gameDao.markScoreSynced(localRowIdToMark)
            }
            if (activeUid.isNotBlank()) {
                val roomIds = gameDao.getAchievements(activeUid)
                    .map { it.achievementId }
                    .filter { AchievementsCatalog.findById(it) != null }
                    .toSet()
                val mergedIds = (roomIds + localUserMedalIds)
                if (mergedIds.isNotEmpty() || latestRemoteMedals.any { it.uid == activeUid }) {
                    val medalsRecord = buildPlayerMedalsRecord(
                        uid = activeUid,
                        playerName = entryToSave.playerName,
                        avatarIndex = entryToSave.avatarIndex,
                        unlockedIds = mergedIds
                    )
                    if (pushMedalsToFirebase(medalsRecord)) {
                        upsertRemoteMedalInMemory(medalsRecord)
                    }
                }
            }
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
            attachRealtimeListeners()
            val unsynced = gameDao.getUnsyncedScores(currentUser.uid)
            for (item in unsynced) {
                val entry = item.toRankingEntry().copy(uid = currentUser.uid)
                val existingRemote = latestRemoteEntries.find { it.id == entry.id }
                val toPush = if (existingRemote != null && !RankingAggregator.isBetterModeEntry(entry, existingRemote)) {
                    existingRemote
                } else {
                    entry
                }
                if (pushScoreToFirebase(toPush)) {
                    gameDao.markScoreSynced(item.id)
                }
            }

            val roomMedalIds = gameDao.getAchievements(currentUser.uid)
                .map { it.achievementId }
                .filter { AchievementsCatalog.findById(it) != null }
                .toSet()
            val mergedIds = roomMedalIds + localUserMedalIds
            val resolvedName = localUserName.trim()
            if (resolvedName.isNotBlank() && (mergedIds.isNotEmpty() || latestRemoteMedals.any { it.uid == currentUser.uid })) {
                val medalsRecord = buildPlayerMedalsRecord(
                    uid = currentUser.uid,
                    playerName = resolvedName,
                    avatarIndex = localUserAvatarIndex,
                    unlockedIds = mergedIds
                )
                if (pushMedalsToFirebase(medalsRecord)) {
                    upsertRemoteMedalInMemory(medalsRecord)
                }
            }
        }
        refreshFromFirebaseRestFallback()
    }

    private fun buildPlayerMedalsRecord(
        uid: String,
        playerName: String,
        avatarIndex: Int,
        unlockedIds: Set<String>
    ): PlayerMedalsRecord {
        val validIds = unlockedIds.filter { AchievementsCatalog.findById(it) != null }.toSet()
        return PlayerMedalsRecord(
            uid = uid,
            playerName = playerName.trim().ifBlank { "Jugador" }.take(30),
            avatarIndex = avatarIndex.coerceIn(1, 5),
            unlockedIds = validIds,
            medalsBonus = AchievementsCatalog.calculateTotalBonus(validIds),
            achievementsCount = validIds.size,
            hasSixSeven = validIds.contains(AchievementsCatalog.ID_SIXSEVEN),
            timestamp = System.currentTimeMillis()
        )
    }

    private fun upsertRemoteMedalInMemory(record: PlayerMedalsRecord) {
        latestRemoteMedals = latestRemoteMedals.filterNot { it.uid == record.uid } + record
    }

    private suspend fun pushScoreToFirebase(entry: RankingEntry): Boolean = withContext(Dispatchers.IO) {
        val currentUser = firebaseAuth?.currentUser ?: return@withContext false
        val uid = currentUser.uid
        val sanitizedName = entry.playerName.trim().ifBlank { "Jugador" }.take(30)
        val safeAvatar = entry.avatarIndex.coerceIn(1, 5)
        val safeScore = entry.score.coerceAtLeast(0)
        val safeDebt = entry.debt.coerceAtLeast(0)
        val safeTime = entry.totalTimeMillis.coerceAtLeast(0L)
        val safeAchievements = entry.achievementsCount.coerceAtLeast(0)
        val canonicalMode = GameModes.canonicalize(entry.gameMode)

        val payloadMap = mapOf(
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
            "gameMode" to canonicalMode,
            "timestamp" to entry.timestamp,
            "scoreVersion" to entry.scoreVersion
        )

        if (writeNodeWithSdk(NODE_RANKING_MODES, entry.id, payloadMap)) {
            return@withContext true
        }

        val json = JSONObject().apply {
            payloadMap.forEach { (k, v) -> put(k, v) }
        }
        httpPutJson(
            relativePath = "$NODE_RANKING_MODES/${entry.id}.json",
            idToken = currentUser.getIdToken(false).awaitTokenOrNull(),
            jsonBody = json.toString()
        )
    }

    private suspend fun pushMedalsToFirebase(record: PlayerMedalsRecord): Boolean = withContext(Dispatchers.IO) {
        val currentUser = firebaseAuth?.currentUser ?: return@withContext false
        if (currentUser.uid != record.uid) return@withContext false

        val unlockedCsv = record.unlockedIds.sorted().joinToString(",")
        val payloadMap = mapOf(
            "uid" to record.uid,
            "playerName" to record.playerName.trim().ifBlank { "Jugador" }.take(30),
            "avatarIndex" to record.avatarIndex.coerceIn(1, 5),
            "unlockedIds" to unlockedCsv,
            "achievementsCount" to record.achievementsCount.coerceIn(0, 7),
            "medalsBonus" to record.medalsBonus.coerceIn(0, AchievementsCatalog.MAX_TOTAL_BONUS),
            "hasSixSeven" to record.hasSixSeven,
            "timestamp" to record.timestamp
        )

        if (writeNodeWithSdk(NODE_RANKING_MEDALS, record.uid, payloadMap)) {
            return@withContext true
        }

        val json = JSONObject().apply {
            payloadMap.forEach { (k, v) -> put(k, v) }
        }
        httpPutJson(
            relativePath = "$NODE_RANKING_MEDALS/${record.uid}.json",
            idToken = currentUser.getIdToken(false).awaitTokenOrNull(),
            jsonBody = json.toString()
        )
    }

    private suspend fun writeNodeWithSdk(
        rootNode: String,
        childKey: String,
        payload: Map<String, Any>
    ): Boolean {
        return try {
            val db = firebaseDb ?: FirebaseInitializer.getDatabaseOrNull(appContext)?.also { firebaseDb = it }
                ?: return false
            db.getReference(rootNode)
                .child(childKey)
                .setValue(payload)
                .awaitTask()
            true
        } catch (_: Exception) {
            false
        }
    }

    private suspend fun com.google.android.gms.tasks.Task<com.google.firebase.auth.GetTokenResult>.awaitTokenOrNull(): String? {
        return try {
            awaitTask().token
        } catch (_: Exception) {
            null
        }
    }

    private fun httpPutJson(relativePath: String, idToken: String?, jsonBody: String): Boolean {
        return try {
            val baseUrl = BuildConfig.FIREBASE_DB_URL.trimEnd('/')
            if (!baseUrl.startsWith("https://") || idToken.isNullOrBlank()) return false
            val url = URL("$baseUrl/$relativePath?auth=$idToken")
            val conn = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "PUT"
                connectTimeout = 4000
                readTimeout = 4000
                doOutput = true
                setRequestProperty("Content-Type", "application/json; charset=UTF-8")
            }
            OutputStreamWriter(conn.outputStream, Charsets.UTF_8).use { writer ->
                writer.write(jsonBody)
            }
            val code = conn.responseCode
            conn.disconnect()
            code in 200..299
        } catch (_: Exception) {
            false
        }
    }

    private fun httpGetJsonRoot(relativePath: String, idToken: String): JSONObject? {
        return try {
            val baseUrl = BuildConfig.FIREBASE_DB_URL.trimEnd('/')
            if (!baseUrl.startsWith("https://")) return null
            val url = URL("$baseUrl/$relativePath?auth=$idToken")
            val conn = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 4000
                readTimeout = 4000
            }
            val code = conn.responseCode
            if (code !in 200..299) {
                conn.disconnect()
                return null
            }
            val body = conn.inputStream.bufferedReader().use { it.readText() }
            conn.disconnect()
            if (body.isBlank() || body == "null") JSONObject() else JSONObject(body)
        } catch (_: Exception) {
            null
        }
    }

    private suspend fun refreshFromFirebaseRestFallback() = withContext(Dispatchers.IO) {
        try {
            val currentUser = firebaseAuth?.currentUser ?: return@withContext
            val idToken = currentUser.getIdToken(false).awaitTokenOrNull() ?: return@withContext

            val modesRoot = httpGetJsonRoot("$NODE_RANKING_MODES.json", idToken)
            val medalsRoot = httpGetJsonRoot("$NODE_RANKING_MEDALS.json", idToken)

            if (modesRoot != null) {
                val fetchedModes = mutableListOf<RankingEntry>()
                val keys = modesRoot.keys()
                while (keys.hasNext()) {
                    val key = keys.next()
                    val obj = modesRoot.optJSONObject(key) ?: continue
                    fetchedModes.add(parseModeEntryFromJson(key, obj))
                }
                latestRemoteEntries = fetchedModes
            }

            if (medalsRoot != null) {
                val fetchedMedals = mutableListOf<PlayerMedalsRecord>()
                val keys = medalsRoot.keys()
                while (keys.hasNext()) {
                    val key = keys.next()
                    val obj = medalsRoot.optJSONObject(key) ?: continue
                    parseMedalsRecordFromJson(key, obj)?.let { fetchedMedals.add(it) }
                }
                latestRemoteMedals = fetchedMedals
            }

            if (modesRoot != null || medalsRoot != null) {
                _isOnlineSynced.value = true
                _connectionStatusMessage.value = "Firebase Cloud conectado en vivo"
                publishMergedRanking()
            }
        } catch (_: Exception) {}
    }

    private fun parseModeEntryFromMap(key: String?, map: Map<*, *>): RankingEntry {
        return RankingEntry(
            id = key ?: UUID.randomUUID().toString(),
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
            gameMode = GameModes.canonicalize((map["gameMode"] as? String) ?: GameModes.LA_SEMANA),
            timestamp = ((map["timestamp"] as? Number)?.toLong()) ?: System.currentTimeMillis(),
            isSynced = true,
            scoreVersion = ((map["scoreVersion"] as? Number)?.toInt()) ?: 1
        )
    }

    private fun parseModeEntryFromJson(key: String, obj: JSONObject): RankingEntry {
        return RankingEntry(
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
            gameMode = GameModes.canonicalize(obj.optString("gameMode", GameModes.LA_SEMANA)),
            timestamp = obj.optLong("timestamp", System.currentTimeMillis()),
            isSynced = true,
            scoreVersion = obj.optInt("scoreVersion", 1)
        )
    }

    private fun parseMedalsRecordFromMap(key: String?, map: Map<*, *>): PlayerMedalsRecord? {
        val uid = (map["uid"] as? String).orEmpty().ifBlank { key.orEmpty() }
        if (uid.isBlank()) return null
        val unlockedIds = AchievementsCatalog.parseValidIds((map["unlockedIds"] as? String).orEmpty())
        return PlayerMedalsRecord(
            uid = uid,
            playerName = (map["playerName"] as? String) ?: "Jugador",
            avatarIndex = ((map["avatarIndex"] as? Number)?.toInt()) ?: 1,
            unlockedIds = unlockedIds,
            medalsBonus = ((map["medalsBonus"] as? Number)?.toInt())
                ?: AchievementsCatalog.calculateTotalBonus(unlockedIds),
            achievementsCount = ((map["achievementsCount"] as? Number)?.toInt()) ?: unlockedIds.size,
            hasSixSeven = (map["hasSixSeven"] as? Boolean)
                ?: unlockedIds.contains(AchievementsCatalog.ID_SIXSEVEN),
            timestamp = ((map["timestamp"] as? Number)?.toLong()) ?: 0L
        )
    }

    private fun parseMedalsRecordFromJson(key: String, obj: JSONObject): PlayerMedalsRecord? {
        val uid = obj.optString("uid", "").ifBlank { key }
        if (uid.isBlank()) return null
        val unlockedIds = AchievementsCatalog.parseValidIds(obj.optString("unlockedIds", ""))
        return PlayerMedalsRecord(
            uid = uid,
            playerName = obj.optString("playerName", "Jugador"),
            avatarIndex = obj.optInt("avatarIndex", 1),
            unlockedIds = unlockedIds,
            medalsBonus = obj.optInt("medalsBonus", AchievementsCatalog.calculateTotalBonus(unlockedIds)),
            achievementsCount = obj.optInt("achievementsCount", unlockedIds.size),
            hasSixSeven = obj.optBoolean("hasSixSeven", unlockedIds.contains(AchievementsCatalog.ID_SIXSEVEN)),
            timestamp = obj.optLong("timestamp", 0L)
        )
    }

    private fun publishMergedRanking() {
        val result = RankingAggregator.buildConsolidatedRanking(
            localModeEntries = latestLocalEntries,
            remoteModeEntries = latestRemoteEntries,
            remoteMedalRecords = latestRemoteMedals,
            currentUserUid = localUserUid,
            currentUserName = localUserName,
            currentUserAvatarIndex = localUserAvatarIndex,
            currentUserMedalIds = localUserMedalIds,
            limit = 30
        )
        _rankingList.value = result.topList
        _currentUserSummary.value = result.currentUserSummary
    }

    private fun LocalScoreEntity.toRankingEntry(): RankingEntry {
        return RankingEntry(
            id = remoteId.ifBlank { "local_$id" },
            uid = uid,
            playerName = playerName,
            avatarIndex = avatarIndex,
            score = score,
            debt = debt,
            savings = savings,
            wellbeingPoints = wellbeingPoints,
            totalTimeMillis = totalTimeMillis,
            achievementsCount = achievementsCount,
            hasSixSeven = hasSixSeven,
            gameMode = GameModes.canonicalize(gameMode),
            timestamp = timestamp,
            isSynced = isSynced,
            scoreVersion = scoreVersion
        )
    }

    companion object {
        private const val NODE_RANKING_MODES = "ranking_juego_limpio"
        private const val NODE_RANKING_MEDALS = "ranking_medallas"

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
