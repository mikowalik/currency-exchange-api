package org.exchange.logic.errors

sealed trait ConvertError

case object ExchangeRatesNotAvailableError extends ConvertError
