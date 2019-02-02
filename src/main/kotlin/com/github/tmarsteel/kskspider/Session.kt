package com.github.tmarsteel.kskspider

import com.github.tmarsteel.kskspider.account.AccountIdentifier
import com.github.tmarsteel.kskspider.account.BankAccountFinancialStatusDTO
import com.github.tmarsteel.kskspider.account.BankAccountNotFoundException
import com.github.tmarsteel.kskspider.authentication.AuthenticationException
import com.github.tmarsteel.kskspider.authentication.Credentials
import com.github.tmarsteel.kskspider.camt.csv.CSVCAMTTransaction
import com.github.tmarsteel.kskspider.transaction.TransactionDTO
import org.csveed.api.CsvClientImpl
import org.jsoup.Connection
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import org.jsoup.nodes.FormElement
import java.io.InputStreamReader
import java.net.URL
import java.nio.file.Paths
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.*

class Session private constructor(
    loginResult: LoadedPage
) : AutoCloseable {
    init {
        val error = loginResult.document.selectFirst("div.loginlogout div.msgerror")

        if (error != null) {
            throw AuthenticationException(error.selectFirst("ul li").text())
        }
    }

    private var closed = false

    private val cookies = mutableMapOf<String, String>()

    private var currentPage: LoadedPage = loginResult
        set(page) {
            cookies.putAll(page.response.cookies())
            field = page
        }

    init {
        // assure proper pageload, including cookies, ...
        currentPage = loginResult
    }

    private val currentLocale: Locale
        get() {
            val url = URL(currentPage.document.location())
            val path = Paths.get(url.path)
            return Locale(path.getName(0).toString())
        }

    private val mutex = Any()

    fun getFinancialStatus(): Collection<BankAccountFinancialStatusDTO> {
        return getFinancialStatusInternal().map { it.second }
    }

    fun getTransactionsInTimeRange(accountId: AccountIdentifier, from: LocalDate, to: LocalDate): List<TransactionDTO> {
        val accountWithTR = getFinancialStatusInternal().firstOrNull {
            it.second.accountId == accountId
        } ?: throw BankAccountNotFoundException(accountId)

        val submitForTXs = accountWithTR.first.selectFirst("td:nth-of-type(5) div:nth-of-type(1) input[type=submit]")!!
        val accountActionsForm = accountWithTR.first.parent("form") as FormElement

        currentPage = accountActionsForm.submitByClicking(submitForTXs)
            .cookies(cookies)
            .followRedirects(true)
            .load()

        val rangeSelectorContainer = currentPage.document.getElementById("zeitraumKalender")!!
        val rangeSelectorInputs = rangeSelectorContainer.select("input[type=text]")
        val dateFormatter = DateTimeFormatter.ofLocalizedDate(FormatStyle.SHORT).withLocale(currentLocale)
        rangeSelectorInputs[0].`val`(from.format(dateFormatter))
        rangeSelectorInputs[1].`val`(to.format(dateFormatter))

        val loadForm = rangeSelectorContainer.parent("form") as FormElement
        val exportToCSVCAMTBtn = loadForm.selectFirst("#exportGroup input[type=submit][value~=CSV-CAMT]")

        val csvInStream = loadForm.submitByClicking(exportToCSVCAMTBtn)
            .cookies(cookies)
            .followRedirects(true)
            .execute()
            .bodyStream()

        val txs = csvInStream.use {
            CsvClientImpl(InputStreamReader(csvInStream), CSVCAMTTransaction::class.java).readBeans()
        }

        return txs.map(TransactionDTO.Companion::fromCAMT)
    }

    /**
     * @return first: the TR in the accounts table holding the bank account info, second: the parsed information
     */
    private fun getFinancialStatusInternal(): Collection<Pair<Element, BankAccountFinancialStatusDTO>> {
        synchronized(mutex) {
            navigate("/$currentLocale/home/onlinebanking/finanzstatus.html")

            val kontoTable = currentPage.document.selectFirst("caption#kontoTable").parent()
            return kontoTable.select("tbody tr").mapNotNull { tr ->
                val data = financialStatus_extractAccountDataFrom(tr)
                data?.let { Pair(tr, data)}
            }
        }
    }

    private fun financialStatus_extractAccountDataFrom(tr: Element): BankAccountFinancialStatusDTO? {
        synchronized(mutex) {
            val accountIDEl = tr.selectFirst(".finaccount .iban") ?: return null
            val accountId = AccountIdentifier.fromString(accountIDEl.text())

            val balanceEl = tr.selectFirst(".balance span:not(.balance-predecimal):not(.balance-decimal)") ?: return null
            val balanceText = balanceEl.text()
            val balanceCurrency = Currency.getInstance(balanceText.substring(balanceText.length - 3).trim())
            val balanceAmount = balanceText.substring(0, balanceText.length - 3).trim().replace(Regex("[^\\d]"), "").toLong()

            return BankAccountFinancialStatusDTO(
                accountId,
                MoneyAmount(balanceAmount, balanceCurrency)
            )
        }
    }

    override fun close() {
        synchronized(mutex) {
            if (closed) return

            val logoutForm = currentPage.document.selectFirst(".loginlogout .logout")!!.parent("form") as FormElement
            val logoutButton = logoutForm.selectFirst(".logout input[type=submit]")

            currentPage = logoutForm.submit()
                .data(logoutButton.attr("name"), logoutButton.attr("value"))
                .cookies(currentPage.response.cookies())
                .followRedirects(true)
                .load()


            closed = true
        }
    }

    /**
     * Follows the first link selected by the given selector. Updates
     * [currentPage].
     *
     * @return the parsed page
     */
    private fun followAnchor(linkSelector: String): Document {
        val element = currentPage.document.selectFirst(linkSelector)!!

        if (element.tagName() != "a") {
            throw IllegalArgumentException("Selector $linkSelector did not resolve to an <a> Tag!")
        }

        val href = element.absUrl("href")
        if (href == null) {
            if (element.attr("href") == null) {
                throw IllegalArgumentException("Selector $linkSelector did not resolve to an <a>")
            } else {
                throw RuntimeException("Could not convert attribute href of $element to an absolute URL.")
            }
        }

        return navigate(href)
    }

    private fun navigate(target: String, force: Boolean = false): Document {
        synchronized(mutex) {
            val currentURL = URL(currentPage.document.location())

            var absTarget = target
            if (!absTarget.startsWith("http://") && !absTarget.startsWith("https://")) {
                absTarget = "https://" + currentURL.host
                if (target.startsWith("/")) {
                    absTarget += target
                } else {
                    absTarget = currentURL.path + "/" + target
                }
            }

            val targetURL = URL(absTarget)

            if (targetURL.host != currentURL.host) {
                throw RuntimeException("Will not navigate off the bank host website! (Trying to navigate to ${targetURL.host})")
            }

            if (currentURL.path == targetURL.path && !force) {
                return currentPage.document
            }

            println("navigating... cookies of current response: ${currentPage.response.cookies()}")
            currentPage = Jsoup.connect(absTarget)
                .cookies(currentPage.response.cookies())
                .followRedirects(true)
                .method(Connection.Method.GET)
                .load()

            println("navigated to $targetURL. cookies after navigate: ${currentPage.response.cookies()}")

            return currentPage.document
        }
    }

    companion object {
        @JvmStatic
        fun <T> doWith(credentials: Credentials, actions: Session.() -> T): T {
            val session = Session.open(credentials)

            try {
                return session.actions()
            }
            finally {
                session.close()
            }
        }

        fun open(credentials: Credentials): Session {
            val loginFormResponse = Jsoup.connect("http://${credentials.host}")
                .header("Accept-Language", Locale.getDefault().toString() + "; en; de-de; de")
                .followRedirects(true)
                .method(Connection.Method.GET)
                .execute()

            val loginFormDoc = loginFormResponse.parse()

            val loginForm = loginFormDoc.selectFirst("div.loginlogout form") as FormElement
            loginForm.selectFirst("input[type=text]:not([size=1])").`val`(credentials.username)
            loginForm.selectFirst("input[type=password]").`val`(credentials.pin)

            val loginResult = loginForm.submit()
                .cookies(loginFormResponse.cookies())
                .followRedirects(true)
                .load()

            return Session(loginResult)
        }
    }
}