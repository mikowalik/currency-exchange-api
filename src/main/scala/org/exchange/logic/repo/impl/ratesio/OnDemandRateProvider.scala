package org.exchange.logic.repo.impl.ratesio

import cats.data.EitherT
import cats.effect.IO
import org.exchange.logic.errors._
import org.exchange.logic.repo.RateProvider
import org.exchange.logic.repo.impl.ratesio.base.BaseRatesProvider
import org.exchange.model.{Currency, Rate}

class OnDemandRateProvider(baseRatesProvider: BaseRatesProvider) extends RateProvider {
  override def getRate(from: Currency, to: Currency): EitherT[IO, ConvertError, Rate] = {
    for {
      baseRates <- baseRatesProvider.getRates(from)
      rate <- EitherT.fromEither[IO](baseRates.rates.get(to).toRight[ConvertError](ToCurrencyNotSupportedError(to)))
    } yield rate
  }
}
