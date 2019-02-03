package com.github.tmarsteel.kskspider.transaction

import com.github.tmarsteel.kskspider.MoneyAmount
import com.github.tmarsteel.kskspider.account.AccountIdentifier
import com.github.tmarsteel.kskspider.camt.csv.CSVCAMTTransaction
import java.time.LocalDate

data class TransactionDTO(
    val postedAt: LocalDate,
    val valuedAt: LocalDate,
    val owner: AccountIdentifier,
    val partner: AccountIdentifier,
    val partnerName: String?,
    val amount: MoneyAmount,
    val purpose: String?,
    val creditor: String?,
    val mandateReference: String?
) {
    companion object {
        @JvmStatic
        fun fromCAMT(tx: CSVCAMTTransaction) = TransactionDTO(
            tx.postedAt,
            tx.valuedAt,
            tx.ownerIBAN,
            tx.partnerAccountID,
            tx.partnerName,
            tx.moneyAmount,
            tx.purpose,
            tx.creditor,
            tx.mandateReference
        )
    }
}