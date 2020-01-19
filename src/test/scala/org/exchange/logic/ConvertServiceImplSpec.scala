package org.exchange.logic

import cats.data.EitherT
import cats.effect.IO
import org.exchange.TestData
import org.exchange.logic.errors.{ConvertError, ExampleError}
import org.exchange.logic.repo.RateProvider
import org.exchange.model.{Amount, ConvertOutput, Currency, Rate}
import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.should.Matchers

class ConvertServiceImplSpec extends AnyFunSuite with Matchers with TestData {

  test("Correct calculation") {

    val rate = Rate(BigDecimal("2.0"))

    val provider = new RateProvider {
      override def getRate(from: Currency, to: Currency): EitherT[IO, ConvertError, Rate] = EitherT.rightT(rate)
    }

    val service = new ConvertServiceImpl(provider)

    val result = service.convert(exampleInput).value.unsafeRunSync()

    result shouldEqual Right(ConvertOutput(
      exchange = rate,
      amount = Amount(BigDecimal("205.2")),
      original = exampleInput.amount
    ))
  }

  test("Forward expected error from provider") {

    val provider = new RateProvider {
      override def getRate(from: Currency, to: Currency): EitherT[IO, ConvertError, Rate] = EitherT.leftT(ExampleError)
    }

    val service = new ConvertServiceImpl(provider)

    val result = service.convert(exampleInput).value.unsafeRunSync()

    result shouldEqual Left(ExampleError)
  }

  test("Forward unexpected error from provider") {

    val e = new Exception("Unexpected exception")

    val provider = new RateProvider {
      override def getRate(from: Currency, to: Currency): EitherT[IO, ConvertError, Rate] = EitherT.liftF(IO.raiseError(e))
    }

    val service = new ConvertServiceImpl(provider)

    val result = service.convert(exampleInput).value.attempt.unsafeRunSync()

    result shouldEqual Left(e)
  }
}
