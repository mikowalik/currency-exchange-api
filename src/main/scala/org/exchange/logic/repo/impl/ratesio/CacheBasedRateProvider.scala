package org.exchange.logic.repo.impl.ratesio

import cats.data.EitherT
import cats.effect.IO
import org.exchange.Logging
import org.exchange.logic.errors.{ConvertError, ToCurrencyNotSupportedError}
import org.exchange.logic.repo.RateProvider
import org.exchange.logic.repo.impl.ratesio.base.BaseRatesProvider
import org.exchange.logic.repo.impl.ratesio.cache.CacheManager
import org.exchange.model.{Currency, Rate}

class CacheBasedRateProvider(
  baseRatesProvider: BaseRatesProvider,
  cacheManager: CacheManager
) extends RateProvider with Logging {

  override def getRate(from: Currency, to: Currency): EitherT[IO, ConvertError, Rate] = {
    for {
      cachedRates <- getFromCache(from)
      _ <- EitherT.rightT[IO, ConvertError](logger.debug(s"Searching for base: $from in cache returned ${cachedRates.size} rates"))
      rates <- getRatesRegardingCache(cachedRates, from)
      rate <- EitherT.fromEither[IO](rates.get(to).toRight[ConvertError](ToCurrencyNotSupportedError(to)))
      _ <- saveRatesRegardingCache(cachedRates, rates, from)
    } yield rate
  }

  private def getFromCache(base: Currency): EitherT[IO, ConvertError, Map[Currency, Rate]] = {
    EitherT.liftF(
      cacheManager
        .read(base)
        .attempt
        .map(_.fold(
          t => {
            val _ = logger.warn("Cache problems: ", t)
            Map.empty
          },
          identity
        ))
    )
  }

  private def getRatesRegardingCache(cachedRates: Map[Currency, Rate], base: Currency): EitherT[IO, ConvertError, Map[Currency, Rate]] = {
    if (cachedRates.isEmpty) baseRatesProvider.getRates(base).map(_.rates)
    else EitherT.rightT(cachedRates)
  }

  private def saveRatesRegardingCache(
    cachedRates: Map[Currency, Rate],
    usedRates: Map[Currency, Rate],
    base: Currency): EitherT[IO, ConvertError, Unit] = {

    if (cachedRates.isEmpty)
      EitherT.liftF(
        cacheManager
          .write(base, usedRates)
          .attempt
          .map(_.fold(
            t => {
              val _ = logger.warn("Cache problems: ", t)
              Map.empty
            },
            identity
          ))
      )
    else EitherT.rightT(())
  }
}
