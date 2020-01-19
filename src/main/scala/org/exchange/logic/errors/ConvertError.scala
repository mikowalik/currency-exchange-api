package org.exchange.logic.errors

import org.exchange.model.Currency

sealed trait ConvertError

case object ExampleError extends ConvertError
final case class RatesIOResponseError(t: Throwable) extends ConvertError
final case class UriBuildingError(stringUri: String, from: Currency) extends ConvertError
final case class ToCurrencyNotSupportedError(currency: Currency) extends ConvertError
final case class BaseCurrencyNotSupportedError(currency: Currency) extends ConvertError
