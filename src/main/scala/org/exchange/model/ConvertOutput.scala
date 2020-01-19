package org.exchange.model

final case class ConvertOutput(
  exchange: Rate,
  amount: Amount,
  original: Amount
)

