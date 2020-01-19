package org.exchange.logic.repo.impl.ratesio

import cats.data.EitherT
import cats.effect.IO
import org.exchange.TestData
import org.exchange.logic.errors.{ConvertError, RatesIOResponseError, ToCurrencyNotSupportedError}
import org.exchange.logic.repo.impl.ratesio.base.BaseRatesProvider
import org.exchange.logic.repo.impl.ratesio.cache.CacheManager
import org.exchange.logic.repo.impl.ratesio.model.RatesIOResponse
import org.exchange.model.{Currency, Rate}
import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.should.Matchers

class CacheBasedRateProviderSpec extends AnyFunSuite with Matchers with TestData {

  private val exampleFrom = Currency("PLN")
  private val exampleTo = Currency("GBP")

  test("Correct gathering data from cache") {

    val mockBaseRatesProvider = new BaseRatesProvider {
      override def getRates(from: Currency): EitherT[IO, ConvertError, RatesIOResponse] = ???
    }
    val mockCacheManager = new CacheManager {
      override def write(base: Currency, rates: Map[Currency, Rate]): IO[Unit] = ???
      override def read(base: Currency): IO[Map[Currency, Rate]] = IO.pure(exampleRatesIOResponse.rates)
    }

    val service = new CacheBasedRateProvider(mockBaseRatesProvider, mockCacheManager)

    val result = service.getRate(exampleFrom, exampleTo).value.unsafeRunSync()

    result shouldEqual Right(Rate(BigDecimal("0.2")))
  }

  test("Correct behaviour when cache is empty") {

    val mockBaseRatesProvider = new BaseRatesProvider {
      override def getRates(from: Currency): EitherT[IO, ConvertError, RatesIOResponse] = EitherT.rightT(exampleRatesIOResponse)
    }
    val mockCacheManager = new CacheManager {
      override def write(base: Currency, rates: Map[Currency, Rate]): IO[Unit] = IO.pure(())
      override def read(base: Currency): IO[Map[Currency, Rate]] = IO.pure(Map.empty)
    }

    val service = new CacheBasedRateProvider(mockBaseRatesProvider, mockCacheManager)

    val result = service.getRate(exampleFrom, exampleTo).value.unsafeRunSync()

    result shouldEqual Right(Rate(BigDecimal("0.2")))
  }

  test("Not present currency") {

    val mockBaseRatesProvider = new BaseRatesProvider {
      override def getRates(from: Currency): EitherT[IO, ConvertError, RatesIOResponse] = EitherT.rightT(exampleRatesIOResponse.copy(rates = Map.empty))
    }
    val mockCacheManager = new CacheManager {
      override def write(base: Currency, rates: Map[Currency, Rate]): IO[Unit] = ???
      override def read(base: Currency): IO[Map[Currency, Rate]] = IO.pure(Map.empty)
    }

    val service = new CacheBasedRateProvider(mockBaseRatesProvider, mockCacheManager)

    val result = service.getRate(exampleFrom, exampleTo).value.unsafeRunSync()

    result shouldEqual Left(ToCurrencyNotSupportedError(exampleTo))
  }

  test("Forward expected error from provider") {

    val forwardedError = RatesIOResponseError(new Exception("boom"))

    val mockBaseRatesProvider = new BaseRatesProvider {
      override def getRates(from: Currency): EitherT[IO, ConvertError, RatesIOResponse] = EitherT.leftT(forwardedError)
    }
    val mockCacheManager = new CacheManager {
      override def write(base: Currency, rates: Map[Currency, Rate]): IO[Unit] = ???
      override def read(base: Currency): IO[Map[Currency, Rate]] = IO.pure(Map.empty)
    }

    val service = new CacheBasedRateProvider(mockBaseRatesProvider, mockCacheManager)

    val result = service.getRate(exampleFrom, exampleTo).value.unsafeRunSync()

    result shouldEqual Left(forwardedError)
  }

  test("Forward unexpected error from provider") {

    val e = new Exception("boom")

    val mockBaseRatesProvider = new BaseRatesProvider {
      override def getRates(from: Currency): EitherT[IO, ConvertError, RatesIOResponse] = EitherT.liftF(IO.raiseError(e))
    }
    val mockCacheManager = new CacheManager {
      override def write(base: Currency, rates: Map[Currency, Rate]): IO[Unit] = ???
      override def read(base: Currency): IO[Map[Currency, Rate]] = IO.pure(Map.empty)
    }

    val service = new CacheBasedRateProvider(mockBaseRatesProvider, mockCacheManager)

    val result = service.getRate(exampleFrom, exampleTo).value.attempt.unsafeRunSync()

    result shouldEqual Left(e)
  }

  test("Ignore unexpected error from reading from cache") {

    val mockBaseRatesProvider = new BaseRatesProvider {
      override def getRates(from: Currency): EitherT[IO, ConvertError, RatesIOResponse] = EitherT.rightT(exampleRatesIOResponse)
    }
    val mockCacheManager = new CacheManager {
      override def write(base: Currency, rates: Map[Currency, Rate]): IO[Unit] = IO.pure(())
      override def read(base: Currency): IO[Map[Currency, Rate]] = IO.raiseError(new Exception("boom"))
    }

    val service = new CacheBasedRateProvider(mockBaseRatesProvider, mockCacheManager)

    val result = service.getRate(exampleFrom, exampleTo).value.unsafeRunSync()

    result shouldEqual Right(Rate(BigDecimal("0.2")))
  }

  test("Ignore unexpected error from writing to cache") {

    val mockBaseRatesProvider = new BaseRatesProvider {
      override def getRates(from: Currency): EitherT[IO, ConvertError, RatesIOResponse] = EitherT.rightT(exampleRatesIOResponse)
    }
    val mockCacheManager = new CacheManager {
      override def write(base: Currency, rates: Map[Currency, Rate]): IO[Unit] = IO.raiseError(new Exception("boom"))
      override def read(base: Currency): IO[Map[Currency, Rate]] = IO.pure(Map.empty)
    }

    val service = new CacheBasedRateProvider(mockBaseRatesProvider, mockCacheManager)

    val result = service.getRate(exampleFrom, exampleTo).value.unsafeRunSync()

    result shouldEqual Right(Rate(BigDecimal("0.2")))
  }
}
