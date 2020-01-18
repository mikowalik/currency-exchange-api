package org.exchange.logic.repo

import cats.data.EitherT
import cats.effect.IO
import org.exchange.logic.errors.ConvertError

trait RatesProvider {
  def getRate(from: String, to: String): EitherT[IO, ConvertError, BigDecimal]
}
