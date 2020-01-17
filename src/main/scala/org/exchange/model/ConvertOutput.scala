package org.exchange.model

final case class ConvertOutput(
  exchange: BigDecimal,
  amount: BigDecimal,
  original: BigDecimal
)

