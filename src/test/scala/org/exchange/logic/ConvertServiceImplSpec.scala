package org.exchange.logic

import cats.data.EitherT
import cats.effect.IO
import org.exchange.TestData
import org.exchange.logic.errors.{ConvertError, ExchangeRatesNotAvailableError}
import org.exchange.logic.repo.RatesProvider
import org.exchange.model.ConvertOutput
import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.should.Matchers

class ConvertServiceImplSpec extends AnyFunSuite with Matchers with TestData {

  test("Correct calculation") {

    val rate = BigDecimal("2.0")

    val provider = new RatesProvider {
      override def getRate(from: String, to: String): EitherT[IO, ConvertError, BigDecimal] = EitherT.rightT(rate)
    }

    val service = new ConvertServiceImpl(provider)

    val result = service.convert(exampleInput).value.unsafeRunSync()

    result shouldEqual Right(ConvertOutput(
      exchange = rate,
      amount = BigDecimal("205.2"),
      original = exampleInput.amount
    ))
  }

  test("Forward expected error from provider") {

    val provider = new RatesProvider {
      override def getRate(from: String, to: String): EitherT[IO, ConvertError, BigDecimal] = EitherT.leftT(ExchangeRatesNotAvailableError)
    }

    val service = new ConvertServiceImpl(provider)

    val result = service.convert(exampleInput).value.unsafeRunSync()

    result shouldEqual Left(ExchangeRatesNotAvailableError)
  }

  test("Forward unexpected error from provider") {

    val e = new Exception("Unexpected exception")

    val provider = new RatesProvider {
      override def getRate(from: String, to: String): EitherT[IO, ConvertError, BigDecimal] = EitherT.liftF(IO.raiseError(e))
    }

    val service = new ConvertServiceImpl(provider)

    val result = service.convert(exampleInput).value.attempt.unsafeRunSync()

    result shouldEqual Left(e)
  }
}
