package org.exchange.logic.repo.impl.ratesio

import cats.data.EitherT
import cats.effect.IO
import org.exchange.TestData
import org.exchange.logic.errors.{ConvertError, RatesIOResponseError, ToCurrencyNotSupportedError}
import org.exchange.logic.repo.impl.ratesio.base.BaseRatesProvider
import org.exchange.logic.repo.impl.ratesio.model.RatesIOResponse
import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.should.Matchers

class OnDemandRateProviderSpec extends AnyFunSuite with Matchers with TestData {

  private val exampleFrom = "PLN"
  private val exampleTo = "GBP"

  test("Correct gathering data") {

    val mockBaseRatesProvider = new BaseRatesProvider {
      override def getRates(from: String): EitherT[IO, ConvertError, RatesIOResponse] = EitherT.rightT(exampleRatesIOResponse)
    }
    val service = new OnDemandRateProvider(mockBaseRatesProvider)

    val result = service.getRate(exampleFrom, exampleTo).value.unsafeRunSync()

    result shouldEqual Right(BigDecimal("0.2"))
  }

  test("Not present currency") {

    val mockBaseRatesProvider = new BaseRatesProvider {
      override def getRates(from: String): EitherT[IO, ConvertError, RatesIOResponse] = EitherT.rightT(exampleRatesIOResponse.copy(rates = Map.empty))
    }
    val service = new OnDemandRateProvider(mockBaseRatesProvider)

    val result = service.getRate(exampleFrom, exampleTo).value.unsafeRunSync()

    result shouldEqual Left(ToCurrencyNotSupportedError(exampleTo))
  }

  test("Forward expected error from provider") {

    val forwardedError = RatesIOResponseError(new Exception("boom"))

    val mockBaseRatesProvider = new BaseRatesProvider {
      override def getRates(from: String): EitherT[IO, ConvertError, RatesIOResponse] = EitherT.leftT(forwardedError)
    }
    val service = new OnDemandRateProvider(mockBaseRatesProvider)

    val result = service.getRate(exampleFrom, exampleTo).value.unsafeRunSync()

    result shouldEqual Left(forwardedError)
  }

  test("Forward unexpected error from provider") {

    val e = new Exception("boom")

    val mockBaseRatesProvider = new BaseRatesProvider {
      override def getRates(from: String): EitherT[IO, ConvertError, RatesIOResponse] = EitherT.liftF(IO.raiseError(e))
    }
    val service = new OnDemandRateProvider(mockBaseRatesProvider)

    val result = service.getRate(exampleFrom, exampleTo).value.attempt.unsafeRunSync()

    result shouldEqual Left(e)
  }
}
