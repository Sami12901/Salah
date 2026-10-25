package com.salahmate.app.domain

import com.salahmate.app.domain.zakat.NisabBasisOption
import com.salahmate.app.domain.zakat.ZakatCalculator
import com.salahmate.app.domain.zakat.ZakatInput
import com.salahmate.app.domain.zakat.ZakatWarning
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Zakat model tests. Prices are supplied by the test as if the user typed them — the app has no
 * built-in gold or silver price, so nothing here depends on a market rate.
 */
class ZakatCalculatorTest {

    private val base = ZakatInput(
        cash = 100_000.0,
        goldGrams = 0.0,
        goldPricePerGram = 8_000.0,
        silverGrams = 0.0,
        silverPricePerGram = 100.0,
        nisabBasis = NisabBasisOption.GOLD,
        currency = "BDT",
    )

    @Test
    fun `zakat is 2_5 percent of net wealth above nisab`() {
        val result = ZakatCalculator.calculate(base)
        // Nisab on gold basis: 87.48 g * 8000 = 699,840 BDT, so 100,000 cash alone is below nisab.
        assertEquals(699_840.0, result.nisabGoldValue, 0.01)
        assertFalse(result.exceedsNisab)
        assertEquals(0.0, result.zakatDue, 0.0)
        assertTrue(result.warnings.contains(ZakatWarning.BELOW_NISAB))
    }

    @Test
    fun `silver nisab basis makes the same wealth zakatable`() {
        val result = ZakatCalculator.calculate(base.copy(nisabBasis = NisabBasisOption.SILVER))
        // 612.36 g * 100 = 61,236 BDT, which 100,000 cash exceeds.
        assertEquals(61_236.0, result.nisabSilverValue, 0.01)
        assertTrue(result.exceedsNisab)
        assertEquals(100_000.0 * 0.025, result.zakatDue, 0.001)
    }

    @Test
    fun `due debts are deducted before the rate is applied`() {
        val result = ZakatCalculator.calculate(
            base.copy(cash = 200_000.0, deductibleDebts = 50_000.0, nisabBasis = NisabBasisOption.SILVER),
        )
        assertEquals(200_000.0, result.totalAssets, 0.001)
        assertEquals(150_000.0, result.netWealth, 0.001)
        assertEquals(3_750.0, result.zakatDue, 0.001)
    }

    @Test
    fun `net wealth never goes below zero when debts exceed assets`() {
        val result = ZakatCalculator.calculate(base.copy(cash = 10_000.0, deductibleDebts = 90_000.0))
        assertEquals(0.0, result.netWealth, 0.0)
        assertEquals(0.0, result.zakatDue, 0.0)
    }

    @Test
    fun `gold without a price is flagged rather than silently valued`() {
        val result = ZakatCalculator.calculate(base.copy(goldGrams = 100.0, goldPricePerGram = 0.0))
        assertTrue(result.warnings.contains(ZakatWarning.MISSING_GOLD_PRICE))
        assertEquals(0.0, result.goldValue, 0.0)
    }

    @Test
    fun `metal value uses grams times the entered price`() {
        val result = ZakatCalculator.calculate(
            base.copy(cash = 0.0, goldGrams = 90.0, goldPricePerGram = 8_000.0, silverGrams = 700.0, silverPricePerGram = 100.0),
        )
        assertEquals(720_000.0, result.goldValue, 0.001)
        assertEquals(70_000.0, result.silverValue, 0.001)
        assertEquals(790_000.0, result.totalAssets, 0.001)
        assertEquals(790_000.0 * 0.025, result.zakatDue, 0.001)
    }

    @Test
    fun `stale price warning appears only after thirty days`() {
        val day = 24L * 60 * 60 * 1000
        val entered = 1_000_000_000_000L
        val fresh = ZakatCalculator.calculate(base, pricesEnteredAtMillis = entered, nowMillis = entered + 10 * day)
        val stale = ZakatCalculator.calculate(base, pricesEnteredAtMillis = entered, nowMillis = entered + 31 * day)
        assertFalse(fresh.warnings.contains(ZakatWarning.STALE_PRICES))
        assertTrue(stale.warnings.contains(ZakatWarning.STALE_PRICES))
    }

    @Test
    fun `empty form reports no assets`() {
        val result = ZakatCalculator.calculate(ZakatInput())
        assertTrue(result.warnings.contains(ZakatWarning.NO_ASSETS))
        assertEquals(0.0, result.zakatDue, 0.0)
    }

    @Test
    fun `rate constant is one fortieth`() {
        assertEquals(0.025, ZakatCalculator.ZAKAT_RATE, 0.0)
        assertEquals(87.48, ZakatCalculator.NISAB_GOLD_GRAMS, 0.0)
        assertEquals(612.36, ZakatCalculator.NISAB_SILVER_GRAMS, 0.0)
    }
}
