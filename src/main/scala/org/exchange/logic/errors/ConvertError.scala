package org.exchange.logic.errors

sealed trait ConvertError

case object ExampleError extends ConvertError
final case class RatesIOResponseError(t: Throwable) extends ConvertError
final case class UriBuildingError(stringUri: String, from: String) extends ConvertError
final case class ToCurrencyNotSupportedError(currency: String) extends ConvertError
final case class BaseCurrencyNotSupportedError(currency: String) extends ConvertError
