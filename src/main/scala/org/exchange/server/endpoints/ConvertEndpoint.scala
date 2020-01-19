package org.exchange.server.endpoints

import cats.effect.IO
import org.exchange.Logging
import org.exchange.logic.ConvertService
import org.exchange.logic.errors.{BaseCurrencyNotSupportedError, ConvertError, RatesIOResponseError, ToCurrencyNotSupportedError}
import org.exchange.model.{ConvertInput, ConvertOutput, Currency}
import org.exchange.server.endpoints.json.JsonFormats._
import org.http4s.HttpRoutes
import org.http4s.circe.CirceEntityCodec._
import org.http4s.dsl.io._

object ConvertEndpointMessages {
  val RatesIOResponseErrorMessage = "An error occurred during communication with exchangeratesapi.io. Please try again later..."
  def CurrencyNotSupportedErrorMessage(currency: Currency) = s"Requested currency: '${currency.value}' is not supported"
  val ToDoError = """ToDo error occurred, sorry ¯\_(ツ)_/¯"""
  val UnexpectedError = "Unexpected error..."
}

class ConvertEndpoint(convertService: ConvertService) extends Logging {
  def route(): HttpRoutes[IO] = {
    HttpRoutes.of[IO] {
      case req @ POST -> Root / "api" / "convert" =>
        val action: IO[Either[ConvertError, ConvertOutput]] = for {
          input <- req.as[ConvertInput]
          result <- convertService.convert(input).value
        } yield result

          action
            .attempt
            .flatMap {
              case Right(expectedOutput) => expectedOutput match {
                case Right(output) => Ok(output)
                case Left(BaseCurrencyNotSupportedError(currency)) =>
                  val _ = logger.error(s"$BaseCurrencyNotSupportedError: $currency")
                  BadRequest(ConvertEndpointMessages.CurrencyNotSupportedErrorMessage(currency))
                case Left(ToCurrencyNotSupportedError(currency)) =>
                  val _ = logger.error(s"$ToCurrencyNotSupportedError: $currency")
                  BadRequest(ConvertEndpointMessages.CurrencyNotSupportedErrorMessage(currency))
                case Left(RatesIOResponseError(t)) =>
                  val _ = logger.error("RatesIOResponseError: ", t)
                  ServiceUnavailable(ConvertEndpointMessages.RatesIOResponseErrorMessage)
                case Left(e) =>
                  val _ = logger.error(e.toString)
                  InternalServerError(ConvertEndpointMessages.ToDoError)
              }
              case Left(throwable) =>
                val _ = logger.error("Something went pretty bad", throwable)
                InternalServerError(ConvertEndpointMessages.UnexpectedError)
        }
    }
  }
}
