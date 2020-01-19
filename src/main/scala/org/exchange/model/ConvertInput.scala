package org.exchange.model

final case class ConvertInput (
  fromCurrency: Currency,
  toCurrency: Currency,
  amount: Amount
)
