package org.exchange.logic.repo.impl.ratesio.model

case class RatesIOResponse(
  rates: Map[String, BigDecimal],
  base: String,
  date: String
)
