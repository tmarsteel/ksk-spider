package com.github.tmarsteel.kskspider.account

import com.github.tmarsteel.kskspider.MoneyAmount

data class BankAccountFinancialStatusDTO(
    val accountId: AccountIdentifier,
    val balance: MoneyAmount
)