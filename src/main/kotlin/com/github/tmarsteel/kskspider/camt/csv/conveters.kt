package com.github.tmarsteel.kskspider.camt.csv

import com.github.tmarsteel.kskspider.account.AccountIdentifier
import org.csveed.bean.conversion.Converter
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoField

class LocalDateConverter : Converter<LocalDate> {
    override fun infoOnType() = "A Date formatted as dd.MM.yy"

    override fun toString(value: LocalDate?): String? = value?.format(formatter)

    override fun getType() = LocalDate::class.java

    override fun fromString(text: String?): LocalDate? {
        val ta = formatter.parse(text)
        return LocalDate.of(ta.get(ChronoField.YEAR), ta.get(ChronoField.MONTH_OF_YEAR), ta.get(ChronoField.DAY_OF_MONTH))
    }

    private companion object {
        val formatter = DateTimeFormatter.ofPattern("dd.MM.yy")!!
    }
}

class AccountIdentifierConverter : Converter<AccountIdentifier> {
    override fun infoOnType() = "Any financial account identifier, e.g. IBAN"

    override fun toString(value: AccountIdentifier?): String? = value.toString()

    override fun getType() = AccountIdentifier::class.java

    override fun fromString(text: String?): AccountIdentifier? {
        if (text == null) return null

        return AccountIdentifier.fromString(text)
    }
}