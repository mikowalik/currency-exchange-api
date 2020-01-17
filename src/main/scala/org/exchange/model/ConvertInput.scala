package org.exchange.model

final case class ConvertInput (
  fromCurrency: String,
  toCurrency: String,
  amount: BigDecimal
)
