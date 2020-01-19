package org.exchange.logic.repo

import cats.data.EitherT
import cats.effect.IO
import org.exchange.logic.errors.ConvertError
import org.exchange.model.{Currency, Rate}

trait RateProvider {
  def getRate(from: Currency, to: Currency): EitherT[IO, ConvertError, Rate]
}
