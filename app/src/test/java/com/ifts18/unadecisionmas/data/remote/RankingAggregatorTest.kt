package com.ifts18.unadecisionmas.data.remote

import com.ifts18.unadecisionmas.data.model.AchievementsCatalog
import com.ifts18.unadecisionmas.data.model.GameModes
import com.ifts18.unadecisionmas.data.model.GameState
import com.ifts18.unadecisionmas.data.model.PlayerMedalsRecord
import com.ifts18.unadecisionmas.data.model.RankingEntry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class RankingAggregatorTest {

    @Test
    fun `la semana score excludes session medals because medals are added separately`() {
        val withoutMedals = GameState(
            currentSavings = 20000,
            currentDebt = 0,
            wellbeingPoints = 50,
            correctDecisionsCount = 5,
            totalDecisionTimeMillis = 25_000L,
            sessionAchievementIds = emptySet()
        )
        val withMedals = withoutMedals.copy(
            sessionAchievementIds = setOf(
                AchievementsCatalog.ID_INTOCABLE,
                AchievementsCatalog.ID_HONRADO_100,
                AchievementsCatalog.ID_SIXSEVEN
            )
        )

        assertEquals(withoutMedals.calculateCompetitiveScore(), withMedals.calculateCompetitiveScore())
    }

    @Test
    fun `player total sums best score of each of the 4 modes plus unique medal bonuses`() {
        val uid = "u1"
        val entries = listOf(
            modeEntry(uid, "Mateo", GameModes.LA_SEMANA, score = 20_000, timeMs = 30_000L),
            modeEntry(uid, "Mateo", GameModes.LA_SEMANA, score = 12_000, timeMs = 15_000L, id = "u1_la_semana_old"),
            modeEntry(uid, "Mateo", GameModes.SCROLL_SIN_CAER, score = 15_000, timeMs = 10_000L),
            modeEntry(uid, "Mateo", GameModes.TRAMPA_JACKPOT, score = 14_000, timeMs = 5_000L),
            modeEntry(uid, "Mateo", GameModes.LA_RULETA_29, score = 16_000, timeMs = 6_000L)
        )
        val medals = listOf(
            PlayerMedalsRecord(
                uid = uid,
                playerName = "Mateo",
                avatarIndex = 2,
                unlockedIds = setOf(
                    AchievementsCatalog.ID_INTOCABLE, // 1500
                    AchievementsCatalog.ID_OJO_ABIERTO, // 1200
                    AchievementsCatalog.ID_SIXSEVEN // 2500
                ),
                medalsBonus = 5200,
                achievementsCount = 3,
                hasSixSeven = true,
                timestamp = 200L
            )
        )

        val result = RankingAggregator.buildConsolidatedRanking(
            localModeEntries = emptyList(),
            remoteModeEntries = entries,
            remoteMedalRecords = medals
        )

        assertEquals(1, result.topList.size)
        val summary = result.topList.first()
        assertEquals(65_000, summary.modesTotalScore)
        assertEquals(5_200, summary.medalsBonusScore)
        assertEquals(70_200, summary.totalScore)
        assertEquals(4, summary.modesPlayedCount)
        assertEquals(3, summary.achievementsCount)
        assertTrue(summary.hasSixSeven)
    }

    @Test
    fun `sixseven qr medal counts even when player has not played any game mode yet`() {
        val result = RankingAggregator.buildConsolidatedRanking(
            localModeEntries = emptyList(),
            remoteModeEntries = emptyList(),
            remoteMedalRecords = emptyList(),
            currentUserUid = "u_qr",
            currentUserName = "3,14",
            currentUserAvatarIndex = 5,
            currentUserMedalIds = setOf(AchievementsCatalog.ID_SIXSEVEN)
        )

        assertEquals(1, result.topList.size)
        val summary = result.topList.first()
        assertEquals(0, summary.modesTotalScore)
        assertEquals(2500, summary.medalsBonusScore)
        assertEquals(2500, summary.totalScore)
        assertEquals(0, summary.modesPlayedCount)
        assertTrue(summary.hasSixSeven)
    }

    @Test
    fun `tie breaker prioritizes higher medal points then medal count then lower time`() {
        // Both players have totalScore = 20_000
        val playerMoreMedalPoints = modeEntry("uA", "Ana", GameModes.LA_SEMANA, score = 17_500, timeMs = 40_000L)
        val playerFewerMedalPoints = modeEntry("uB", "Beto", GameModes.LA_SEMANA, score = 18_500, timeMs = 10_000L)

        // Ana has SIXSEVEN (2500 pts); Beto has INTOCABLE (1500 pts)
        val medals = listOf(
            PlayerMedalsRecord(
                uid = "uA",
                playerName = "Ana",
                unlockedIds = setOf(AchievementsCatalog.ID_SIXSEVEN),
                medalsBonus = 2500,
                achievementsCount = 1,
                hasSixSeven = true
            ),
            PlayerMedalsRecord(
                uid = "uB",
                playerName = "Beto",
                unlockedIds = setOf(AchievementsCatalog.ID_INTOCABLE),
                medalsBonus = 1500,
                achievementsCount = 1
            )
        )

        val result = RankingAggregator.buildConsolidatedRanking(
            localModeEntries = emptyList(),
            remoteModeEntries = listOf(playerFewerMedalPoints, playerMoreMedalPoints),
            remoteMedalRecords = medals
        )

        assertEquals(listOf("Ana", "Beto"), result.topList.map { it.playerName })

        // Now test same totalScore AND same medal bonus (2500), different medal count:
        // Carla: VIDA_REAL (1000) + INTOCABLE (1500) = 2500 (2 medals)
        // Diego: SIXSEVEN (2500) = 2500 (1 medal, faster time)
        val carlaMode = modeEntry("uC", "Carla", GameModes.LA_SEMANA, score = 17_500, timeMs = 50_000L)
        val diegoMode = modeEntry("uD", "Diego", GameModes.LA_SEMANA, score = 17_500, timeMs = 5_000L)
        val tieMedals = listOf(
            PlayerMedalsRecord(
                uid = "uC",
                playerName = "Carla",
                unlockedIds = setOf(AchievementsCatalog.ID_VIDA_REAL, AchievementsCatalog.ID_INTOCABLE),
                medalsBonus = 2500,
                achievementsCount = 2
            ),
            PlayerMedalsRecord(
                uid = "uD",
                playerName = "Diego",
                unlockedIds = setOf(AchievementsCatalog.ID_SIXSEVEN),
                medalsBonus = 2500,
                achievementsCount = 1,
                hasSixSeven = true
            )
        )

        val tieResult = RankingAggregator.buildConsolidatedRanking(
            localModeEntries = emptyList(),
            remoteModeEntries = listOf(diegoMode, carlaMode),
            remoteMedalRecords = tieMedals
        )

        assertEquals(listOf("Carla", "Diego"), tieResult.topList.map { it.playerName })
    }

    @Test
    fun `unsynced v2 local score beats stale remote v1 or lower remote v2 score`() {
        val remoteStaleV1 = modeEntry(
            uid = "u1",
            name = "Lu",
            mode = GameModes.LA_SEMANA,
            score = 30_892,
            timeMs = 44_000L,
            scoreVersion = 1,
            isSynced = true
        )
        val localFreshV2 = modeEntry(
            uid = "u1",
            name = "Lu",
            mode = GameModes.LA_SEMANA,
            score = 26_400,
            timeMs = 20_000L,
            scoreVersion = 2,
            isSynced = false
        )

        val result = RankingAggregator.buildConsolidatedRanking(
            localModeEntries = listOf(localFreshV2),
            remoteModeEntries = listOf(remoteStaleV1),
            remoteMedalRecords = emptyList(),
            currentUserUid = "u1",
            currentUserName = "Lu",
            currentUserMedalIds = setOf(AchievementsCatalog.ID_INTOCABLE) // 1500
        )

        val lu = result.currentUserSummary
        assertNotNull(lu)
        assertEquals(26_400, lu!!.modesTotalScore)
        assertEquals(1_500, lu.medalsBonusScore)
        assertEquals(27_900, lu.totalScore)
    }

    @Test
    fun `all 7 medals sum to 9100 points`() {
        val allIds = AchievementsCatalog.all.map { it.id }
        assertEquals(9100, AchievementsCatalog.calculateTotalBonus(allIds))
        assertEquals(9100, AchievementsCatalog.MAX_TOTAL_BONUS)
    }

    private fun modeEntry(
        uid: String,
        name: String,
        mode: String,
        score: Int,
        timeMs: Long,
        id: String = "${uid}_${GameModes.slug(mode)}",
        scoreVersion: Int = RankingEntry.CURRENT_SCORE_VERSION,
        isSynced: Boolean = true
    ): RankingEntry = RankingEntry(
        id = id,
        uid = uid,
        playerName = name,
        avatarIndex = 1,
        score = score,
        totalTimeMillis = timeMs,
        gameMode = mode,
        timestamp = 100L,
        isSynced = isSynced,
        scoreVersion = scoreVersion
    )
}
