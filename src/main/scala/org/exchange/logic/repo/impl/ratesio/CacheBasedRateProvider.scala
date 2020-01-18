package org.exchange.logic.repo.impl.ratesio

import cats.data.EitherT
import cats.effect.IO
import org.exchange.Logging
import org.exchange.logic.errors.{ConvertError, ToCurrencyNotSupportedError}
import org.exchange.logic.repo.RateProvider
import org.exchange.logic.repo.impl.ratesio.base.BaseRatesProvider
import org.exchange.logic.repo.impl.ratesio.cache.CacheManager

class CacheBasedRateProvider(
  baseRatesProvider: BaseRatesProvider,
  cacheManager: CacheManager
) extends RateProvider with Logging {
  override def getRate(from: String, to: String): EitherT[IO, ConvertError, BigDecimal] = {
    for {
      cachedRates <- getFromCache(from)
      _ <- EitherT.rightT[IO, ConvertError](logger.debug(s"Searching for base: $from in cache returned ${cachedRates.size} rates"))
      rates <- getRatesRegardingCache(cachedRates, from)
      rate <- EitherT.fromEither[IO](rates.get(to).toRight[ConvertError](ToCurrencyNotSupportedError(to)))
      _ <- saveRatesRegardingCache(cachedRates, rates, from)
    } yield rate
  }

  private def getFromCache(base: String): EitherT[IO, ConvertError, Map[String, BigDecimal]] = {
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

  private def getRatesRegardingCache(cachedRates: Map[String, BigDecimal], base: String): EitherT[IO, ConvertError, Map[String, BigDecimal]] = {
    if (cachedRates.isEmpty) baseRatesProvider.getRates(base).map(_.rates)
    else EitherT.rightT(cachedRates)
  }

  private def saveRatesRegardingCache(
    cachedRates: Map[String, BigDecimal],
    usedRates: Map[String, BigDecimal],
    base: String): EitherT[IO, ConvertError, Unit] = {

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
