package org.exchange.server.endpoints

import cats.effect.IO
import io.circe.generic.auto._
import org.exchange.Logging
import org.exchange.logic.ConvertService
import org.exchange.logic.errors.{ConvertError, ExchangeRatesNotAvailableError}
import org.exchange.model.{ConvertInput, ConvertOutput}
import org.http4s.HttpRoutes
import org.http4s.circe.CirceEntityCodec._
import org.http4s.dsl.io._

object ConvertEndpointMessages {
  val ExchangeRatesNotAvailableErrorMessage = "An error occurred during communication with exchangeratesapi.io. Please try again later..."
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
                case Left(ExchangeRatesNotAvailableError) =>
                  val _ = logger.error("ExchangeRatesNotAvailableError")
                  ServiceUnavailable(ConvertEndpointMessages.ExchangeRatesNotAvailableErrorMessage)
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
