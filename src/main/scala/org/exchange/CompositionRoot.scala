package org.exchange

import cats.effect.{ContextShift, IO, Timer}
import org.exchange.config.Conf
import org.exchange.logic.ConvertService
import org.exchange.logic.errors.ConvertError
import org.exchange.model.{ConvertInput, ConvertOutput}
import org.exchange.server.endpoints.{ConvertEndpoint, StatusEndpoint}
import org.http4s.implicits._
import org.http4s.server.Router
import org.http4s.server.blaze.BlazeServerBuilder

class CompositionRoot(conf: Conf)(implicit cd: ContextShift[IO], timer: Timer[IO]) {

  private val httpApp = {
    val statusEndpoint = new StatusEndpoint
    val convertEndpoint = {

      val mockConvertService = new ConvertService {
        override def convert(input: ConvertInput): IO[Either[ConvertError, ConvertOutput]] =
          IO {
          Right(ConvertOutput(
            exchange = BigDecimal("1.11"),
            amount = BigDecimal("113.886"),
            original = BigDecimal("102.6")
          ))
        }
      }

      new ConvertEndpoint(mockConvertService)
    }

    Router[IO](
      "/" -> statusEndpoint.route(),
      "/" -> convertEndpoint.route()
    ).orNotFound
  }

  val server: IO[Unit] = BlazeServerBuilder[IO]
    .bindHttp(conf.apiConfig.port, conf.apiConfig.host)
    .withHttpApp(httpApp)
    .serve
    .compile
    .drain
}
