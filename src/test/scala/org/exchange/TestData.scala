package org.exchange

import org.exchange.model.ConvertInput

trait TestData {
  val exampleInput = ConvertInput(
    fromCurrency = "GBP",
    toCurrency = "EUR",
    amount = BigDecimal("102.6")
  )
}
