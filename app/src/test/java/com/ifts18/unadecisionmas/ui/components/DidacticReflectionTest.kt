package com.ifts18.unadecisionmas.ui.components

import com.ifts18.unadecisionmas.data.model.GameModes
import com.ifts18.unadecisionmas.data.model.GameState
import com.ifts18.unadecisionmas.data.model.PlayerProfile
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DidacticReflectionTest {

    @Test
    fun `la semana limpia felicita y mantiene las 5 historias didacticas`() {
        val state = GameState(
            profile = PlayerProfile(name = "Lucía"),
            lastCompletedMode = GameModes.LA_SEMANA,
            currentDebt = 0,
            incorrectDecisionsCount = 0
        )

        val deck = buildReflectionDeck(state)

        assertEquals("Una decisión más", deck.headerTitle)
        assertTrue(deck.isPositiveOutcome)
        assertEquals(5, deck.slides.size)
        assertTrue(deck.slides.first().headline.contains("Lucía"))
        assertEquals(5, deck.slides[1].listItems.size)
        assertTrue(deck.slides[3].headline.contains("50/50"))
        assertTrue(deck.slides.last().isFinalSlide)
    }

    @Test
    fun `la semana con apuesta o prestamo aclara que no fue su culpa`() {
        val state = GameState(
            lastCompletedMode = GameModes.LA_SEMANA,
            currentDebt = 10000,
            incorrectDecisionsCount = 1
        )

        val deck = buildReflectionDeck(state)

        assertFalse(deck.isPositiveOutcome)
        assertEquals(5, deck.slides.size)
        assertEquals("No fue tu culpa.", deck.slides.first().headline)
        assertTrue(deck.slides.first().paragraphs.first().text.contains("no es tu culpa"))
    }

    @Test
    fun `ruleta tragamonedas y anuncios generan sus historias y graficos del html`() {
        val rouletteDeck = buildReflectionDeck(
            GameState(lastCompletedMode = GameModes.LA_RULETA_29, currentDebt = 5000)
        )
        assertEquals("Ruleta: la trampa", rouletteDeck.headerTitle)
        assertEquals(5, rouletteDeck.slides.size)
        assertEquals(StoryGraphicType.ROULETTE_BALANCE_CURVE, rouletteDeck.slides[0].graphic)

        val slotsDeck = buildReflectionDeck(
            GameState(lastCompletedMode = GameModes.TRAMPA_JACKPOT, currentDebt = 5000)
        )
        assertEquals("Tragamonedas: la trampa", slotsDeck.headerTitle)
        assertEquals(5, slotsDeck.slides.size)
        assertEquals(StoryGraphicType.SLOTS_RISING_BARS, slotsDeck.slides[1].graphic)

        val adsDeck = buildReflectionDeck(
            GameState(lastCompletedMode = GameModes.SCROLL_SIN_CAER, currentDebt = 8000, incorrectDecisionsCount = 2)
        )
        assertEquals("Anuncios: la trampa", adsDeck.headerTitle)
        assertEquals(6, adsDeck.slides.size)
        assertEquals(StoryGraphicType.ADS_FEED_PATTERN, adsDeck.slides[1].graphic)
        assertTrue(adsDeck.slides[0].paragraphs.first().text.contains("Tocaste 2 anuncios trampa"))
    }
}
