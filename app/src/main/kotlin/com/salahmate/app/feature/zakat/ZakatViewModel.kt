package com.salahmate.app.feature.zakat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.salahmate.app.AppContainer
import com.salahmate.app.core.db.ZakatRecordEntity
import com.salahmate.app.core.prefs.NisabBasis
import com.salahmate.app.core.prefs.UserSettings
import com.salahmate.app.domain.zakat.NisabBasisOption
import com.salahmate.app.domain.zakat.ZakatCalculator
import com.salahmate.app.domain.zakat.ZakatInput
import com.salahmate.app.domain.zakat.ZakatResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class ZakatFormState(
    val cash: Double = 0.0,
    val bank: Double = 0.0,
    val inventory: Double = 0.0,
    val receivables: Double = 0.0,
    val other: Double = 0.0,
    val debts: Double = 0.0,
    val goldGrams: Double = 0.0,
    val goldPrice: Double = 0.0,
    val silverGrams: Double = 0.0,
    val silverPrice: Double = 0.0,
    val nisab: NisabBasisOption = NisabBasisOption.GOLD,
    val currency: String = "BDT",
    val message: String? = null,
    val saved: List<ZakatRecordEntity> = emptyList(),
)

class ZakatViewModel(private val container: AppContainer) : ViewModel() {

    private val _ui = MutableStateFlow(ZakatFormState())
    val ui: StateFlow<ZakatFormState> = _ui

    init {
        viewModelScope.launch {
            val settings = container.settings.settings.first()
            _ui.value = _ui.value.copy(
                currency = settings.zakatCurrency,
                nisab = if (settings.nisabBasis == NisabBasis.GOLD) NisabBasisOption.GOLD else NisabBasisOption.SILVER,
                goldPrice = settings.goldPricePerGram,
                silverPrice = settings.silverPricePerGram,
            )
            container.database.zakatDao().records().collect { records ->
                _ui.value = _ui.value.copy(saved = records)
            }
        }
    }

    fun update(block: (ZakatFormState) -> ZakatFormState) {
        _ui.value = block(_ui.value)
    }

    fun setNisab(basis: NisabBasisOption) {
        _ui.value = _ui.value.copy(nisab = basis)
    }

    fun result(): ZakatResult = ZakatCalculator.calculate(
        ZakatInput(
            cash = _ui.value.cash,
            bankBalances = _ui.value.bank,
            goldGrams = _ui.value.goldGrams,
            goldPricePerGram = _ui.value.goldPrice,
            silverGrams = _ui.value.silverGrams,
            silverPricePerGram = _ui.value.silverPrice,
            businessInventory = _ui.value.inventory,
            receivables = _ui.value.receivables,
            otherAssets = _ui.value.other,
            deductibleDebts = _ui.value.debts,
            nisabBasis = _ui.value.nisab,
            currency = _ui.value.currency,
        ),
    )

    fun save() {
        val result = result()
        if (result.totalAssets <= 0.0) {
            _ui.value = _ui.value.copy(message = "Enter at least one asset before saving.")
            return
        }
        viewModelScope.launch {
            container.database.zakatDao().insert(
                ZakatRecordEntity(
                    createdAt = System.currentTimeMillis(),
                    currency = result.currency,
                    totalAssets = result.totalAssets,
                    deductible = result.totalLiabilities,
                    netWealth = result.netWealth,
                    nisabValue = result.nisabApplied,
                    zakatDue = result.zakatDue,
                    payloadJson = "",
                ),
            )
            // Remember the metal prices the user entered so the next session starts from them.
            container.settings.setMetalPrices(
                gold = _ui.value.goldPrice,
                silver = _ui.value.silverPrice,
                updatedAt = System.currentTimeMillis(),
            )
            _ui.value = _ui.value.copy(message = "Calculation saved on this device.")
        }
    }

    fun reset() {
        _ui.value = ZakatFormState(
            currency = _ui.value.currency,
            nisab = _ui.value.nisab,
            saved = _ui.value.saved,
        )
    }

    fun clearMessage() {
        _ui.value = _ui.value.copy(message = null)
    }
}
