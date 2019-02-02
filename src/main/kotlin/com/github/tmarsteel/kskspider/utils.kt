package com.github.tmarsteel.kskspider

val String.isNumeric: Boolean
    get() = all(Char::isDigit)