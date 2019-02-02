package com.github.tmarsteel.kskspider.account

class BankAccountNotFoundException(identifier: AccountIdentifier, cause: Throwable? = null) : RuntimeException("Account with identifier $identifier not found.", cause)