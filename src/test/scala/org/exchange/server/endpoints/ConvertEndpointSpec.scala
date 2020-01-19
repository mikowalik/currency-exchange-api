package org.exchange.server.endpoints

import cats.data.EitherT
import cats.effect.IO
import io.circe.Decoder.Result
import io.circe.parser._
import io.circe.{Decoder, Encoder, HCursor, Json}
import org.exchange.TestData
import org.exchange.logic.ConvertService
import org.exchange.logic.errors.{ConvertError, ExampleError}
import org.exchange.model.{Amount, ConvertInput, ConvertOutput, Rate}
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
  with Http4sClientDsl[IO]
  with TestData {

  test("Return correct result from service") {

    val expectedConvertOutput = exampleConvertOutput

    val mockService = new ConvertService {
      override def convert(input: ConvertInput): EitherT[IO, ConvertError, ConvertOutput] = EitherT.rightT(expectedConvertOutput)
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

    val serviceOutput = IO(Left(ExampleError))
    val expectedStatus: Status = InternalServerError
    val expectedResponse: String = ConvertEndpointMessages.ToDoError

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
    exchange = Rate(BigDecimal("1.11")),
    amount = Amount(BigDecimal("113.886")),
    original = Amount(BigDecimal("102.6"))
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

  implicit val outputDecoder: Decoder[ConvertOutput] = new Decoder[ConvertOutput] {
    override def apply(c: HCursor): Result[ConvertOutput] = {
      for {
        exchange <- c.downField("exchange").as[BigDecimal]
        amount <- c.downField("amount").as[BigDecimal]
        original <- c.downField("original").as[BigDecimal]
      } yield ConvertOutput(
        exchange = Rate(exchange),
        amount = Amount(amount),
        original = Amount(original)
      )
    }
  }

  implicit val inputEncoder: Encoder[ConvertInput] = new Encoder[ConvertInput] {
    override def apply(o: ConvertInput): Json = {
      Json.obj(
        "fromCurrency" -> Json.fromString(o.fromCurrency.value),
        "toCurrency" -> Json.fromString(o.toCurrency.value),
        "amount" -> Json.fromBigDecimal(o.amount.value)
      )
    }
  }

  private def stringBasedTest(
    serviceOutput: IO[Either[ConvertError, ConvertOutput]],
    expectedStatus: Status,
    expectedResponse: String,
    usedJson: String = correctExampleInputJson,
  ) = {

    val mockService = new ConvertService {
      override def convert(input: ConvertInput): EitherT[IO, ConvertError, ConvertOutput] = EitherT(serviceOutput)
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
