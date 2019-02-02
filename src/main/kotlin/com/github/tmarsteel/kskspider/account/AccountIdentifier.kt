package com.github.tmarsteel.kskspider.account

import com.github.tmarsteel.kskspider.isNumeric
import java.math.BigInteger

sealed class AccountIdentifier {
    companion object {
        @JvmStatic
        fun fromString(identifier: String): AccountIdentifier {
            try {
                return IBAN.fromString(identifier)
            }
            catch (ex: InvalidIBANException) {
                return UnknownAccountIdentifier(identifier)
            }
        }
    }
}

data class IBAN(
    val countryCode: CountryCode,
    val branchID: String,
    val accountNumber: String
) : AccountIdentifier() {
    init {
        if (!branchID.isNumeric) {
            throw InvalidIBANException("The branch identifier must be numeric")
        }

        if (branchID.length != countryCode.branchIDLength) {
            throw InvalidIBANException("For cc=$countryCode, the branch identifier must be ${countryCode.branchIDLength} digits in length")
        }

        if (!accountNumber.isNumeric) {
            throw InvalidIBANException("The account number must be numeric")
        }

        if (accountNumber.length != countryCode.accountNumberLength) {
            throw InvalidIBANException("For cc=$countryCode, the account number must be ${countryCode.accountNumberLength} digits in length")
        }
    }

    val checksum: String by lazy {
        val number = branchID + accountNumber + countryCode.toDigitsForChecksumCalculation() + "00"
        val mod97 = BigInteger(number).mod(BigInteger("97")).toInt()
        val checksum = (98 - mod97).toString()

        checksum.padStart(2, '0')
    }

    override fun toString(): String {
        return countryCode.name + checksum + branchID + accountNumber
    }

    companion object {
        @JvmStatic
        fun fromString(str: String): IBAN {
            val normalized = str.replace(Regex("\\s"), "").toUpperCase()

            val countryCode = try {
                CountryCode.valueOf(normalized.substring(0, 2))
            } catch (ex: IllegalArgumentException) {
                throw InvalidIBANException("Did not recognize country code", ex)
            }

            val branchId = normalized.substring(4, 4 + countryCode.branchIDLength)
            val accountNumber = normalized.substring(4 + countryCode.branchIDLength)

            val iban = IBAN(countryCode, branchId, accountNumber)
            if (iban.checksum != normalized.substring(2..3)) {
                throw InvalidIBANException("Invalid checksum")
            }

            return iban
        }
    }

    enum class CountryCode(val branchIDLength: Int, val accountNumberLength: Int) {
        DE(8, 10);

        init {
            if (name.length != 2) throw IllegalStateException()
        }

        internal fun toDigitsForChecksumCalculation(): String {
            return "${name[0].indexInLatinAlphabet + 10}${name[1].indexInLatinAlphabet + 10}"
        }
    }
}

data class UnknownAccountIdentifier(val identifier: String) : AccountIdentifier() {
    override fun toString() = identifier
}

class InvalidIBANException(message: String? = null, cause: Throwable? = null) : RuntimeException(message, cause)

private val Char.indexInLatinAlphabet: Int
    get() {
        val uc = toUpperCase()
        if (uc < 'A' || uc > 'Z') {
            throw IllegalArgumentException("$this is not a letter in the latin alphabet.")
        }

        return uc - 'A'
    }