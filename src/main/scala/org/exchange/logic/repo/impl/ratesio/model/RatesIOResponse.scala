package org.exchange.logic.repo.impl.ratesio.model

import io.circe.Decoder.Result
import io.circe.{Decoder, HCursor}
import org.exchange.model.{Currency, Rate}

case class RatesIOResponse(
  rates: Map[Currency, Rate],
  base: Currency,
  date: String
)

object RatesIOResponse {
  implicit val decoder: Decoder[RatesIOResponse] = new Decoder[RatesIOResponse] {
    override def apply(c: HCursor): Result[RatesIOResponse] = {
      for {
        rates <- c.downField("rates").as[Map[String, BigDecimal]]
        base <- c.downField("base").as[String]
        date <- c.downField("date").as[String]
      } yield RatesIOResponse(
        rates = rates.map { case(k, v) => Currency(k) -> Rate(v)},
        base = Currency(base),
        date = date
      )
    }
  }
}
