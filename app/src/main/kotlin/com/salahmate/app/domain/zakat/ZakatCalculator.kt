package com.salahmate.app.domain.zakat

/**
 * Zakat calculation model — documented explicitly because different scholars handle some asset
 * categories differently. This is an informational aid, not a fatwa.
 *
 * MODEL USED
 *  • Zakat is due on wealth that has been held for one lunar year (hawl) and exceeds the nisab.
 *  • Rate: 2.5% (1/40) of net zakatable wealth.
 *  • Nisab is the *lower* of the two classical thresholds, as a common precaution:
 *      - 87.48 g of gold  (7.5 tola)
 *      - 612.36 g of silver (52.5 tola)
 *    The user chooses which basis to apply; the app shows both thresholds and their monetary value
 *    so the choice is explicit and auditable.
 *  • Included assets: cash and bank balances, gold, silver, business inventory for sale,
 *    receivables expected to be repaid, and other liquid investments.
 *  • Deliberated (flagged, not silently treated as zero): personal-use gold above the customary
 *    exemption, long-term investments, and money held in pension schemes with no access.
 *  • Excluded from this model: the family home, personal vehicle, household furniture, and
 *    tools of trade, and any asset the user marks as not zakatable.
 *  • Liabilities: current, immediately-due debts are deductible. Long-term debt instalments that
 *    are not yet due are not deducted in this model (some scholars deduct a full year's
 *    instalments; the user can add that amount manually if they follow that view).
 *
 * Metal prices are never assumed: they must be entered by the user and are stamped with the time
 * they were entered so the UI can warn when they are stale.
 */

data class ZakatInput(
    val cash: Double = 0.0,
    val bankBalances: Double = 0.0,
    val goldGrams: Double = 0.0,
    val goldPricePerGram: Double = 0.0,
    val silverGrams: Double = 0.0,
    val silverPricePerGram: Double = 0.0,
    val businessInventory: Double = 0.0,
    val receivables: Double = 0.0,
    val otherAssets: Double = 0.0,
    val deductibleDebts: Double = 0.0,
    val excludedAssets: Double = 0.0,
    val nisabBasis: NisabBasisOption = NisabBasisOption.GOLD,
    val currency: String = "BDT",
)

enum class NisabBasisOption { GOLD, SILVER }

data class ZakatLine(val labelEn: String, val labelBn: String, val amount: Double, val included: Boolean)

data class ZakatResult(
    val currency: String,
    val lines: List<ZakatLine>,
    val goldValue: Double,
    val silverValue: Double,
    val totalAssets: Double,
    val totalLiabilities: Double,
    val netWealth: Double,
    val nisabBasis: NisabBasisOption,
    val nisabGoldValue: Double,
    val nisabSilverValue: Double,
    val nisabApplied: Double,
    val zakatDue: Double,
    val exceedsNisab: Boolean,
    val warnings: List<ZakatWarning>,
) {
    val rate: Double get() = ZakatCalculator.ZAKAT_RATE
}

enum class ZakatWarning {
    MISSING_GOLD_PRICE,
    MISSING_SILVER_PRICE,
    STALE_PRICES,
    BELOW_NISAB,
    NO_ASSETS,
}

object ZakatCalculator {

    const val ZAKAT_RATE = 0.025
    const val NISAB_GOLD_GRAMS = 87.48
    const val NISAB_SILVER_GRAMS = 612.36

    fun calculate(input: ZakatInput, pricesEnteredAtMillis: Long = 0L, nowMillis: Long = 0L): ZakatResult {
        val goldValue = input.goldGrams * input.goldPricePerGram
        val silverValue = input.silverGrams * input.silverPricePerGram

        val lines = listOf(
            ZakatLine("Cash in hand", "নগদ টাকা", input.cash, true),
            ZakatLine("Bank balances", "ব্যাংক ব্যালেন্স", input.bankBalances, true),
            ZakatLine("Gold (${"%.2f".format(input.goldGrams)} g)", "স্বর্ণ (${"%.2f".format(input.goldGrams)} গ্রাম)", goldValue, true),
            ZakatLine("Silver (${"%.2f".format(input.silverGrams)} g)", "রূপা (${"%.2f".format(input.silverGrams)} গ্রাম)", silverValue, true),
            ZakatLine("Business inventory", "ব্যবসার মজুদ", input.businessInventory, true),
            ZakatLine("Receivables", "পাওনা টাকা", input.receivables, true),
            ZakatLine("Other zakatable assets", "অন্যান্য যাকাতযোগ্য সম্পদ", input.otherAssets, true),
            ZakatLine("Less: due debts", "বাদ: পরিশোধ্য ঋণ", input.deductibleDebts, true),
            ZakatLine("Excluded (not zakatable)", "যাকাতের বাইরে", input.excludedAssets, false),
        )

        val totalAssets = input.cash + input.bankBalances + goldValue + silverValue +
            input.businessInventory + input.receivables + input.otherAssets
        val netWealth = (totalAssets - input.deductibleDebts).coerceAtLeast(0.0)

        val nisabGoldValue = NISAB_GOLD_GRAMS * input.goldPricePerGram
        val nisabSilverValue = NISAB_SILVER_GRAMS * input.silverPricePerGram
        val nisabApplied = when (input.nisabBasis) {
            NisabBasisOption.GOLD -> nisabGoldValue
            NisabBasisOption.SILVER -> nisabSilverValue
        }
        val exceeds = nisabApplied > 0.0 && netWealth >= nisabApplied
        val zakatDue = if (exceeds) netWealth * ZAKAT_RATE else 0.0

        val warnings = buildList {
            if (input.goldGrams > 0 && input.goldPricePerGram <= 0) add(ZakatWarning.MISSING_GOLD_PRICE)
            if (input.silverGrams > 0 && input.silverPricePerGram <= 0) add(ZakatWarning.MISSING_SILVER_PRICE)
            if (pricesEnteredAtMillis > 0 && nowMillis > 0 &&
                nowMillis - pricesEnteredAtMillis > 30L * 24 * 60 * 60 * 1000
            ) add(ZakatWarning.STALE_PRICES)
            if (totalAssets <= 0.0) add(ZakatWarning.NO_ASSETS)
            if (!exceeds && totalAssets > 0.0) add(ZakatWarning.BELOW_NISAB)
        }

        return ZakatResult(
            currency = input.currency,
            lines = lines,
            goldValue = goldValue,
            silverValue = silverValue,
            totalAssets = totalAssets,
            totalLiabilities = input.deductibleDebts,
            netWealth = netWealth,
            nisabBasis = input.nisabBasis,
            nisabGoldValue = nisabGoldValue,
            nisabSilverValue = nisabSilverValue,
            nisabApplied = nisabApplied,
            zakatDue = zakatDue,
            exceedsNisab = exceeds,
            warnings = warnings,
        )
    }
}
