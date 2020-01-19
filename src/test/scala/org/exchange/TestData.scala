package org.exchange

import org.exchange.logic.repo.impl.ratesio.model.RatesIOResponse
import org.exchange.model.{Amount, ConvertInput, Currency, Rate}

trait TestData {
  val exampleInput = ConvertInput(
    fromCurrency = Currency("GBP"),
    toCurrency = Currency("EUR"),
    amount = Amount(BigDecimal("102.6"))
  )

  val exampleRatesIOResponse = RatesIOResponse(
    rates = Map(
      Currency("PLN") -> Rate(1.0),
        Currency("GBP") -> Rate(0.2)
    ),
    base = Currency("PLN"),
    date = "random"
  )
}
