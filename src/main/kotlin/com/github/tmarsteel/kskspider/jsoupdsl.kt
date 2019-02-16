package com.github.tmarsteel.kskspider

import org.jsoup.Connection
import org.jsoup.Jsoup
import org.jsoup.helper.Validate
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import org.jsoup.nodes.FormElement

data class LoadedPage(
    val response: Connection.Response,
    val document: Document
)

fun Element.parent(cssSelector: String): Element? {
    var pivot: Element? = this

    do {
        pivot = pivot?.parent()
    }
    while (pivot != null && !pivot.`is`(cssSelector))

    return pivot
}

/**
 * Like [FormElement.submit] but ignores `input[type=submit]s` other than the
 * given.
 */
fun FormElement.submitByClicking(submitBtn: Element): Connection {
    val action = if (hasAttr("action")) absUrl("action") else baseUri()
    Validate.notEmpty(
        action,
        "Could not determine a form action URL for submit. Ensure you set a base URI when parsing."
    )

    val method = if (attr("method").toUpperCase() == "POST")
        Connection.Method.POST
    else
        Connection.Method.GET

    val formData = formData()

    formData.removeIf { kv ->
        val element = this@submitByClicking.elements().firstOrNull { it.attr("name") == kv.key() }

        return@removeIf element != null && element.`is`("input[type=submit]") && element != submitBtn
    }

    return Jsoup.connect(action)
        .data(formData)
        .method(method)
}