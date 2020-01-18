package org.exchange

import org.exchange.logic.repo.impl.ratesio.model.RatesIOResponse
import org.exchange.model.ConvertInput

trait TestData {
  val exampleInput = ConvertInput(
    fromCurrency = "GBP",
    toCurrency = "EUR",
    amount = BigDecimal("102.6")
  )

  val exampleRatesIOResponse = RatesIOResponse(
    rates = Map(
      "PLN" -> 1.0,
      "GBP" -> 0.2
    ),
    base = "PLN",
    date = "random"
  )
}
