package org.exchange.server.endpoints

import cats.effect.IO
import io.circe.generic.auto._
import io.circe.parser._
import org.exchange.logic.ConvertService
import org.exchange.logic.errors.{ConvertError, ExchangeRatesNotAvailableError}
import org.exchange.model.{ConvertInput, ConvertOutput}
import org.http4s._
import org.http4s.circe.CirceEntityCodec._
import org.http4s.client.dsl.Http4sClientDsl
import org.http4s.dsl.Http4sDsl
import org.http4s.implicits._
import org.http4s.server.Router
import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.should.Matchers

class ConvertEndpointSpec extends AnyFunSuite
  with Matchers
  with Http4sDsl[IO]
  with Http4sClientDsl[IO] {

  test("Return correct result from service") {

    val expectedConvertOutput = exampleConvertOutput

    val exampleInput = ConvertInput(
      fromCurrency = "GBP",
      toCurrency = "EUR",
      amount = BigDecimal("102.6")
    )

    val mockService = new ConvertService {
      override def convert(input: ConvertInput): IO[Either[ConvertError, ConvertOutput]] = IO {
        Right(expectedConvertOutput)
      }
    }

    val endpoint = new ConvertEndpoint(mockService).route()
    val routes = Router("/" -> endpoint).orNotFound

    (for {
      req <- POST(uri"/api/convert")
      resp <- routes.run(req.withEntity(exampleInput))
      respBody <- resp.as[ConvertOutput]
    } yield {
      resp.status shouldEqual Ok
      respBody shouldEqual expectedConvertOutput
    }).unsafeRunSync
  }

  test("Forward expected error from service") {

    val serviceOutput = IO(Left(ExchangeRatesNotAvailableError))
    val expectedStatus: Status = ServiceUnavailable
    val expectedResponse: String = ConvertEndpointMessages.ExchangeRatesNotAvailableErrorMessage

    stringBasedTest(serviceOutput, expectedStatus, expectedResponse)
  }

  test("Forward unexpected error from service") {

    val serviceOutput = IO.raiseError(new RuntimeException("Random exception"))
    val expectedStatus: Status = InternalServerError
    val expectedResponse: String = ConvertEndpointMessages.UnexpectedError

    stringBasedTest(serviceOutput, expectedStatus, expectedResponse)
  }

  test("Bad Request for incorrect body") {

    val serviceOutput = IO.raiseError(new RuntimeException("Does'nt matter..."))
    val expectedStatus: Status = InternalServerError
    val expectedResponse: String = ConvertEndpointMessages.UnexpectedError

    stringBasedTest(serviceOutput, expectedStatus, expectedResponse, usedJson = "{}")
  }

  private val exampleConvertOutput = ConvertOutput(
    exchange = BigDecimal("1.11"),
    amount = BigDecimal("113.886"),
    original = BigDecimal("102.6")
  )

  private val correctExampleInputJson: String =
    """
      |{
      |   "fromCurrency":"GPB",
      |   "toCurrency": "EUR",
      |   "amount": 102.6
      |}
      |""".stripMargin

  private val correctExampleOutputJson: String =
    """
      |{
      |   "exchange":1.11,
      |   "amount": 113.886,
      |   "original": 102.6
      |}
      |""".stripMargin

  private def stringBasedTest(
    serviceOutput: IO[Either[ConvertError, ConvertOutput]],
    expectedStatus: Status,
    expectedResponse: String,
    usedJson: String = correctExampleInputJson,
  ) = {

    val mockService = new ConvertService {
      override def convert(input: ConvertInput): IO[Either[ConvertError, ConvertOutput]] = serviceOutput
    }

    val endpoint = new ConvertEndpoint(mockService).route()
    val routes = Router("/" -> endpoint).orNotFound

    (for {
      req <- POST(uri"/api/convert")
      resp <- routes.run(
        req.withEntity(
          parse(usedJson).getOrElse(???)
        )
      )
      respBody <- resp.as[String]
    } yield {
      resp.status shouldEqual expectedStatus
      respBody shouldEqual expectedResponse
    }).unsafeRunSync
  }
}
