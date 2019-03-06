package com.github.tmarsteel.kskspider.camt.csv;

import com.github.tmarsteel.kskspider.MoneyAmount;
import com.github.tmarsteel.kskspider.account.AccountIdentifier;
import org.csveed.annotations.CsvCell;
import org.csveed.annotations.CsvConverter;
import org.csveed.annotations.CsvFile;
import org.csveed.bean.ColumnIndexMapper;
import org.jetbrains.annotations.Nullable;

import java.time.LocalDate;
import java.util.Currency;

import static java.util.Objects.requireNonNull;

/**
 * Maps one line of CSV-CAMT as obtained from the export feature
 */
@CsvFile(quotingEnabled = true, separator = ';', useHeader = false, startRow = 2, quote='"', mappingStrategy = ColumnIndexMapper.class, endOfLine = '\n')
public class CSVCAMTTransaction {
    @CsvCell(columnIndex = 1, required = true)
    @CsvConverter(converter = AccountIdentifierConverter.class)
    private AccountIdentifier ownerIBAN;

    @CsvCell(columnIndex = 2, required = true)
    @CsvConverter(converter = LocalDateConverter.class)
    private LocalDate postedAt;

    @CsvCell(columnIndex = 3)
    @CsvConverter(converter = LocalDateConverter.class)
    private LocalDate valuedAt;

    @CsvCell(columnIndex = 4, required = true)
    private String comment;

    @CsvCell(columnIndex = 5)
    private String purpose;

    @CsvCell(columnIndex = 6)
    private String creditor;

    @CsvCell(columnIndex = 7)
    private String mandateReference;

    @CsvCell(columnIndex = 8)
    private String endToEndCustomerReference;

    @CsvCell(columnIndex = 12)
    private String partnerName;

    @CsvCell(columnIndex = 13)
    @CsvConverter(converter = AccountIdentifierConverter.class)
    private AccountIdentifier partnerAccountID;

    @CsvCell(columnIndex = 15)
    private String amount;

    @CsvCell(columnIndex = 16)
    private String currencyCode;

    public AccountIdentifier getOwnerIBAN() {
        return ownerIBAN;
    }

    public void setOwnerIBAN(AccountIdentifier ownerIBAN) {
        this.ownerIBAN = ownerIBAN;
    }

    public LocalDate getPostedAt() {
        return postedAt;
    }

    public void setPostedAt(LocalDate postedAt) {
        this.postedAt = postedAt;
    }

    @Nullable
    public LocalDate getValuedAt() {
        return valuedAt;
    }

    public void setValuedAt(@Nullable LocalDate valuedAt) {
        this.valuedAt = valuedAt;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    @Nullable
    public String getPurpose() {
        return purpose;
    }

    public void setPurpose(@Nullable String purpose) {
        this.purpose = purpose;
    }

    @Nullable
    public String getCreditor() {
        return creditor;
    }

    public void setCreditor(@Nullable String creditor) {
        this.creditor = creditor;
    }

    @Nullable
    public String getMandateReference() {
        return mandateReference;
    }

    public void setMandateReference(@Nullable String mandateReference) {
        this.mandateReference = mandateReference;
    }

    @Nullable
    public String getEndToEndCustomerReference() {
        return endToEndCustomerReference;
    }

    public void setEndToEndCustomerReference(@Nullable String endToEndCustomerReference) {
        this.endToEndCustomerReference = endToEndCustomerReference;
    }

    @Nullable
    public String getPartnerName() {
        return partnerName;
    }

    public void setPartnerName(@Nullable String partnerName) {
        this.partnerName = partnerName;
    }

    @Nullable
    public AccountIdentifier getPartnerAccountID() {
        return partnerAccountID;
    }

    public void setPartnerAccountID(@Nullable AccountIdentifier partnerAccountID) {
        this.partnerAccountID = partnerAccountID;
    }

    @Nullable
    public String getAmount() {
        return amount;
    }

    public void setAmount(@Nullable String amount) {
        this.amount = amount;
    }

    @Nullable
    public String getCurrencyCode() {
        return currencyCode;
    }

    public void setCurrencyCode(@Nullable String currencyCode) {
        this.currencyCode = currencyCode;
    }

    public MoneyAmount getMoneyAmount() {
        requireNonNull(amount);
        requireNonNull(currencyCode);

        long cents = Long.parseLong(amount.replaceAll("[^\\d-]", ""));
        Currency currency = Currency.getInstance(currencyCode);

        return new MoneyAmount(cents, currency);
    }
}
