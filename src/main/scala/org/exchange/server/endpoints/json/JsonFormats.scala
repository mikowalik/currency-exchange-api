package org.exchange.server.endpoints.json

import io.circe.Decoder.Result
import io.circe.{Decoder, Encoder, HCursor, Json}
import org.exchange.model.{Amount, ConvertInput, ConvertOutput, Currency}

object JsonFormats {
  implicit val inputDecoder: Decoder[ConvertInput] = new Decoder[ConvertInput] {
    override def apply(c: HCursor): Result[ConvertInput] = {
      for {
        fromCurrency <- c.downField("fromCurrency").as[String]
        toCurrency <- c.downField("toCurrency").as[String]
        amount <- c.downField("amount").as[BigDecimal]
      } yield ConvertInput(
        fromCurrency = Currency(fromCurrency),
        toCurrency = Currency(toCurrency),
        amount = Amount(amount)
      )
    }
  }

  implicit val outputEncoder: Encoder[ConvertOutput] = new Encoder[ConvertOutput] {
    override def apply(o: ConvertOutput): Json = {
      Json.obj(
        "exchange" -> Json.fromBigDecimal(o.exchange.value),
        "amount" -> Json.fromBigDecimal(o.amount.value),
        "original" -> Json.fromBigDecimal(o.original.value)
      )
    }
  }
}
