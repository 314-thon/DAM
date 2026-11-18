package com.ifts18.unadecisionmas.data.remote

import com.ifts18.unadecisionmas.data.model.AchievementsCatalog
import com.ifts18.unadecisionmas.data.model.GameModes
import com.ifts18.unadecisionmas.data.model.PlayerMedalsRecord
import com.ifts18.unadecisionmas.data.model.PlayerRankingSummary
import com.ifts18.unadecisionmas.data.model.RankingEntry

data class ConsolidatedRankingResult(
    val topList: List<PlayerRankingSummary>,
    val currentUserSummary: PlayerRankingSummary?
)

/**
 * Consolida el Ranking Global sumando el récord más alto de cada modo por jugador
 * y sumando por separado los puntos únicos de sus medallas desbloqueadas.
 */
object RankingAggregator {

    val summaryComparator: Comparator<PlayerRankingSummary> =
        compareByDescending<PlayerRankingSummary> { it.totalScore }
            .thenByDescending { it.medalsBonusScore }
            .thenByDescending { it.achievementsCount }
            .thenBy { it.totalTimeMillis }

    fun normalizeNicknameKey(raw: String): String =
        raw.trim().replace(Regex("\\s+"), " ").lowercase()

    fun isBetterModeEntry(candidate: RankingEntry, current: RankingEntry): Boolean {
        if (candidate.scoreVersion != current.scoreVersion) {
            return candidate.scoreVersion > current.scoreVersion
        }
        if (candidate.score != current.score) {
            return candidate.score > current.score
        }
        if (candidate.totalTimeMillis != current.totalTimeMillis) {
            return candidate.totalTimeMillis < current.totalTimeMillis
        }
        if (candidate.timestamp != current.timestamp) {
            return candidate.timestamp > current.timestamp
        }
        return candidate.isSynced && !current.isSynced
    }

    fun buildConsolidatedRanking(
        localModeEntries: List<RankingEntry>,
        remoteModeEntries: List<RankingEntry>,
        remoteMedalRecords: List<PlayerMedalsRecord>,
        currentUserUid: String = "",
        currentUserName: String = "",
        currentUserAvatarIndex: Int = 1,
        currentUserMedalIds: Set<String> = emptySet(),
        limit: Int = 30
    ): ConsolidatedRankingResult {
        val mergedById = LinkedHashMap<String, RankingEntry>()
        for (entry in localModeEntries + remoteModeEntries) {
            val key = entry.id.ifBlank {
                val pKey = entry.uid.ifBlank { normalizeNicknameKey(entry.playerName) }
                "${pKey}_${GameModes.slug(entry.gameMode)}"
            }
            val current = mergedById[key]
            if (current == null || isBetterModeEntry(entry, current)) {
                mergedById[key] = entry
            }
        }

        // Mejor puntaje de cada modo por jugador (agrupado por UID o apodo si no tiene UID)
        val bestModesByPlayer = LinkedHashMap<String, MutableMap<String, RankingEntry>>()
        for (entry in mergedById.values) {
            val playerKey = entry.uid.ifBlank { normalizeNicknameKey(entry.playerName) }.ifBlank { entry.id }
            val canonicalMode = GameModes.canonicalize(entry.gameMode)
            val playerModes = bestModesByPlayer.getOrPut(playerKey) { LinkedHashMap() }
            val currentBest = playerModes[canonicalMode]
            if (currentBest == null || isBetterModeEntry(entry, currentBest)) {
                playerModes[canonicalMode] = entry.copy(gameMode = canonicalMode)
            }
        }

        // Registro público de medallas por jugador
        val medalsByPlayer = LinkedHashMap<String, PlayerMedalsRecord>()
        for (record in remoteMedalRecords) {
            val playerKey = record.uid.ifBlank { normalizeNicknameKey(record.playerName) }
            if (playerKey.isBlank()) continue
            val existing = medalsByPlayer[playerKey]
            if (existing == null || record.timestamp >= existing.timestamp) {
                medalsByPlayer[playerKey] = record
            }
        }

        if (currentUserUid.isNotBlank()) {
            val existingLocalUserMedals = medalsByPlayer[currentUserUid]
            val mergedIds = (existingLocalUserMedals?.unlockedIds.orEmpty() + currentUserMedalIds)
                .filter { AchievementsCatalog.findById(it) != null }
                .toSet()
            val resolvedName = currentUserName.trim()
                .ifBlank { existingLocalUserMedals?.playerName.orEmpty() }
                .ifBlank { "Jugador" }
            val resolvedAvatar = currentUserAvatarIndex.coerceIn(1, 5)
            val resolvedBonus = AchievementsCatalog.calculateTotalBonus(mergedIds)
            medalsByPlayer[currentUserUid] = PlayerMedalsRecord(
                uid = currentUserUid,
                playerName = resolvedName,
                avatarIndex = resolvedAvatar,
                unlockedIds = mergedIds,
                medalsBonus = resolvedBonus,
                achievementsCount = mergedIds.size,
                hasSixSeven = mergedIds.contains(AchievementsCatalog.ID_SIXSEVEN),
                timestamp = maxOf(existingLocalUserMedals?.timestamp ?: 0L, System.currentTimeMillis())
            )
        }

        val allPlayerKeys = LinkedHashSet<String>().apply {
            addAll(bestModesByPlayer.keys)
            addAll(medalsByPlayer.keys)
        }

        val summaries = mutableListOf<PlayerRankingSummary>()
        for (playerKey in allPlayerKeys) {
            val modeEntriesMap = bestModesByPlayer[playerKey].orEmpty()
            val medalRecord = medalsByPlayer[playerKey]

            val validMedalIds = buildSet {
                medalRecord?.unlockedIds?.forEach { id ->
                    if (AchievementsCatalog.findById(id) != null) add(id)
                }
                if (medalRecord == null && modeEntriesMap.values.any { it.hasSixSeven }) {
                    add(AchievementsCatalog.ID_SIXSEVEN)
                }
            }
            val medalsBonusScore = if (validMedalIds.isNotEmpty()) {
                AchievementsCatalog.calculateTotalBonus(validMedalIds)
            } else {
                (medalRecord?.medalsBonus ?: 0).coerceIn(0, AchievementsCatalog.MAX_TOTAL_BONUS)
            }

            if (modeEntriesMap.isEmpty() && medalsBonusScore <= 0 && validMedalIds.isEmpty()) {
                continue
            }

            val bestScoreByMode = LinkedHashMap<String, Int>()
            for (canonicalMode in GameModes.allModes) {
                modeEntriesMap[canonicalMode]?.let { bestScoreByMode[canonicalMode] = it.score }
            }
            for ((modeName, entry) in modeEntriesMap) {
                if (!bestScoreByMode.containsKey(modeName)) {
                    bestScoreByMode[modeName] = entry.score
                }
            }

            val modesTotalScore = bestScoreByMode.values.sum()
            val totalScore = modesTotalScore + medalsBonusScore
            val totalTimeMillis = modeEntriesMap.values.sumOf { it.totalTimeMillis }

            val latestModeEntry = modeEntriesMap.values.maxByOrNull { it.timestamp }
            val useMedalIdentity = medalRecord != null &&
                (latestModeEntry == null || medalRecord.timestamp >= latestModeEntry.timestamp)

            val resolvedName = when {
                playerKey == currentUserUid && currentUserName.isNotBlank() -> currentUserName.trim()
                useMedalIdentity && medalRecord!!.playerName.isNotBlank() -> medalRecord.playerName.trim()
                latestModeEntry != null && latestModeEntry.playerName.isNotBlank() -> latestModeEntry.playerName.trim()
                medalRecord != null && medalRecord.playerName.isNotBlank() -> medalRecord.playerName.trim()
                else -> "Jugador"
            }

            val resolvedAvatar = when {
                playerKey == currentUserUid && currentUserName.isNotBlank() -> currentUserAvatarIndex.coerceIn(1, 5)
                useMedalIdentity && medalRecord != null -> medalRecord.avatarIndex.coerceIn(1, 5)
                latestModeEntry != null -> latestModeEntry.avatarIndex.coerceIn(1, 5)
                medalRecord != null -> medalRecord.avatarIndex.coerceIn(1, 5)
                else -> 1
            }

            val resolvedUid = when {
                latestModeEntry?.uid?.isNotBlank() == true -> latestModeEntry.uid
                medalRecord?.uid?.isNotBlank() == true -> medalRecord.uid
                playerKey != normalizeNicknameKey(resolvedName) -> playerKey
                else -> ""
            }

            summaries.add(
                PlayerRankingSummary(
                    uid = resolvedUid,
                    playerName = resolvedName,
                    avatarIndex = resolvedAvatar,
                    totalScore = totalScore,
                    modesTotalScore = modesTotalScore,
                    medalsBonusScore = medalsBonusScore,
                    bestScoreByMode = bestScoreByMode,
                    modesPlayedCount = bestScoreByMode.size,
                    unlockedMedalIds = validMedalIds,
                    achievementsCount = validMedalIds.size.coerceAtLeast(medalRecord?.achievementsCount ?: 0),
                    hasSixSeven = validMedalIds.contains(AchievementsCatalog.ID_SIXSEVEN) || (medalRecord?.hasSixSeven == true),
                    totalTimeMillis = totalTimeMillis,
                    isSynced = modeEntriesMap.values.all { it.isSynced }
                )
            )
        }

        // Deduplicación por apodo normalizado (conserva la mejor marca si reinstaló la app con otro UID)
        val dedupByNickname = LinkedHashMap<String, PlayerRankingSummary>()
        for (summary in summaries) {
            val nickKey = normalizeNicknameKey(summary.playerName).ifBlank { summary.uid }
            val currentBest = dedupByNickname[nickKey]
            val shouldReplace = if (currentBest == null) {
                true
            } else {
                val cmp = summaryComparator.compare(summary, currentBest)
                cmp < 0 || (cmp == 0 && summary.uid == currentUserUid && currentBest.uid != currentUserUid)
            }
            if (shouldReplace) {
                dedupByNickname[nickKey] = summary
            }
        }

        val sorted = dedupByNickname.values.sortedWith(summaryComparator)
        val currentUserSummary = if (currentUserUid.isNotBlank()) {
            sorted.firstOrNull { it.uid == currentUserUid }
                ?: currentUserName.takeIf { it.isNotBlank() }?.let { name ->
                    val nickKey = normalizeNicknameKey(name)
                    sorted.firstOrNull { normalizeNicknameKey(it.playerName) == nickKey }
                }
        } else {
            null
        }

        return ConsolidatedRankingResult(
            topList = sorted.take(limit),
            currentUserSummary = currentUserSummary
        )
    }
}
