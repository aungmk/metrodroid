/*
 * TransitCurrency.java
 *
 * Copyright 2018 Google
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package au.id.micolous.metrodroid.transit

import au.id.micolous.metrodroid.multi.Parcelable
import au.id.micolous.metrodroid.multi.Parcelize
import au.id.micolous.metrodroid.util.Preferences
import au.id.micolous.metrodroid.multi.FormattedString
import au.id.micolous.metrodroid.util.getCurrencyDescriptorByCode
import kotlin.jvm.JvmStatic
import kotlin.random.Random

internal expect fun formatCurrency(value: Int, divisor: Int, currencyCode: String, isBalance: Boolean): FormattedString

@Parcelize
open class TransitCurrency (
        private val mCurrency: Int,
        /**
         * 3 character currency code (eg: AUD) per ISO 4217.
         */
        private val mCurrencyCode: String,
        /**
         * Value to divide by to get that currency's value in non-fractional parts.
         *
         * If the value passed is in cents, then divide by 100 to get dollars. This is the default.
         *
         * If the currency has no fractional part (eg: IDR, JPY, KRW), then the divisor should be 1,
         */
        private val mDivisor: Int
): TransitBalance, Parcelable {

    override val balance: TransitCurrency
        get() = this

    class TransitCurrencyDesc(val currencyCode: String,
                              val defaultDivisor: Int,
                              val name: String?)

    /**
     * Builds a new TransitCurrency, used to represent a monetary value on a transit card.
     *
     * For the [.TransitCurrency] constructor, the default {@param divisor}
     * parameter is [.DEFAULT_DIVISOR] (100).
     *
     * For *all other* constructors without a {@param divisor} parameter, this is looked up
     * dynamically using [Currency.getDefaultFractionDigits]. If the currency is unknown,
     * then [.DEFAULT_DIVISOR] (100) is used instead.
     *
     * The [.TransitCurrency] and [.TransitCurrency]
     * constructors do not perform additional lookups at constructor call time.
     *
     * Constructors taking a numeric ISO 4217 {@param currencyCode} will accept unknown currency
     * codes, replacing them with [.UNKNOWN_CURRENCY_CODE] (XXX).
     *
     * Constructors taking a [TransitCurrencyDesc] parameter are intended for
     * internal (to [TransitCurrency]) use. Do not use them outside of this class.
     *
     * Style note: If the [TransitData] only ever supports a single currency, prefer to use
     * one of the static methods of [TransitCurrency] (eg: [.AUD]) to build values,
     * rather than calling this constructor with a constant {@param currencyCode} string.
     *
     * @param currency The amount of currency
     * @param currencyCode An ISO 4217 textual currency code, eg: "AUD".
     * @throws IllegalArgumentException On invalid {@param currencyCode} passed as a string.
     */
    constructor(currency: Int, currencyCode: String) : this(currency, currencyCode, DEFAULT_DIVISOR)

    /**
     * @inheritDoc
     *
     * @param currencyCode An ISO 4217 numeric currency code
     */
    constructor(currency: Int, currencyCode: Int) : this(currency, getCurrencyDescriptorByCode(currencyCode))

    /**
     * @inheritDoc
     *
     * @param currencyCode An ISO 4217 numeric currency code
     */
    constructor(currency: Int, currencyCode: Int, divisor: Int) : this(currency, getCurrencyDescriptorByCode(currencyCode), divisor)

    /**
     * @inheritDoc
     *
     * @param currencyDesc A [TransitCurrencyDesc] instance for the currency.
     */
    private constructor(currency: Int, currencyDesc: TransitCurrencyDesc?,
                        divisor: Int = currencyDesc?.defaultDivisor ?: DEFAULT_DIVISOR) : this(currency,
            currencyDesc?.currencyCode ?: UNKNOWN_CURRENCY_CODE,
            divisor)


    override fun equals(other: Any?): Boolean {
        if (other !is TransitCurrency)
            return false

        if (mCurrencyCode != other.mCurrencyCode) {
            return false
        }

        if (mDivisor == other.mDivisor) {
            return mCurrency == other.mCurrency
        }

        // Divisors don't match -- coerce to a common denominator
        return mCurrency * other.mDivisor == other.mCurrency * mDivisor
    }

    private fun obfuscate(fareOffset: Int, fareMultiplier: Double): TransitCurrency {
        var cur = ((mCurrency + fareOffset) * fareMultiplier).toInt()

        // Match the sign of the original fare
        if (cur > 0 && mCurrency < 0 || cur < 0 && mCurrency >= 0) {
            cur *= -1
        }

        return TransitCurrency(cur, mCurrencyCode, mDivisor)
    }

    fun obfuscate(): TransitCurrency {
        return obfuscate(Random.nextInt(100) - 50,
                Random.nextDouble() * 0.4 + 0.8)
    }

    /**
     * This handles Android-specific issues:
     *
     *
     * - Some currency formatters return too many or too few fractional amounts. (issue #34)
     * - Markup with TtsSpan.MoneyBuilder, for accessibility tools.
     *
     * @param isBalance True if the value being passed is a balance (ie: don't format credits in a
     * special way)
     * @return Formatted currency string
     */
    open fun formatCurrencyString(isBalance: Boolean): FormattedString {
        return formatCurrency(mCurrency, mDivisor, mCurrencyCode, isBalance)
    }

    fun maybeObfuscateBalance(): TransitCurrency {
        return if (!Preferences.obfuscateBalance) {
            this
        } else obfuscate()

    }

    fun maybeObfuscateFare(): TransitCurrency {
        return if (!Preferences.obfuscateTripFares) {
            this
        } else obfuscate()

    }

    fun negate() = TransitCurrency(-mCurrency, mCurrencyCode, mDivisor)

    /**
     * String representation of a TransitCurrency.
     *
     * This should only ever be used by debug logs and unit tests. It does not handle any
     * localisation or formatting.
     *
     * @return String representation of the value, eg: "TransitCurrency.AUD(1234)" for AUD 12.34.
     */
    override fun toString() = "$TAG.$mCurrencyCode($mCurrency, $mDivisor)"

    override fun hashCode(): Int {
        var result = mCurrencyCode.hashCode()
        result = 31 * result + mCurrency * 100 / mDivisor
        return result
    }

    companion object {
        /**
         * Invalid or no currency information, per ISO 4217.
         */
        internal const val UNKNOWN_CURRENCY_CODE = "XXX"
        private const val DEFAULT_DIVISOR = 100
        private const val TAG = "TransitCurrency"

        @JvmStatic
        fun AUD(cents: Int) = TransitCurrency(cents, "AUD")
        @JvmStatic
        fun BRL(centavos: Int) = TransitCurrency(centavos, "BRL")
        @JvmStatic
        fun CAD(cents: Int) = TransitCurrency(cents, "CAD")
        @JvmStatic
        fun CNY(fen: Int) = TransitCurrency(fen, "CNY")
        @JvmStatic
        fun DKK(ore: Int) = TransitCurrency(ore, "DKK")
        @JvmStatic
        fun EUR(cents: Int) = TransitCurrency(cents, "EUR")
        @JvmStatic
        fun HKD(cents: Int) = TransitCurrency(cents, "HKD")
        @JvmStatic
        fun IDR(cents: Int) = TransitCurrency(cents, "IDR", 1)
        @JvmStatic
        fun ILS(agorot: Int) = TransitCurrency(agorot, "ILS")
        @JvmStatic
        fun JPY(yen: Int) = TransitCurrency(yen, "JPY", 1)
        @JvmStatic
        fun KRW(won: Int) = TransitCurrency(won, "KRW", 1)
        @JvmStatic
        fun RUB(kopeyka: Int) = TransitCurrency(kopeyka, "RUB")
        @JvmStatic
        fun SGD(cents: Int) = TransitCurrency(cents, "SGD")
        @JvmStatic
        fun TWD(cents: Int) = TransitCurrency(cents, "TWD", 1)
        @JvmStatic
        fun USD(cents: Int) = TransitCurrency(cents, "USD")

        /**
         * Constructor for use with unknown currencies.
         */
        @JvmStatic
        fun XXX(cents: Int) = TransitCurrency(cents, UNKNOWN_CURRENCY_CODE)
        @JvmStatic
        fun XXX(cents: Int, divisor: Int) = TransitCurrency(cents, UNKNOWN_CURRENCY_CODE, divisor)
    }
}