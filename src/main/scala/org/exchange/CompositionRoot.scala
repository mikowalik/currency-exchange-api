package org.exchange

import cats.data.EitherT
import cats.effect.{ContextShift, IO, Timer}
import org.exchange.config.Conf
import org.exchange.logic.ConvertServiceImpl
import org.exchange.logic.errors.ConvertError
import org.exchange.logic.repo.RatesProvider
import org.exchange.server.endpoints.{ConvertEndpoint, StatusEndpoint}
import org.http4s.implicits._
import org.http4s.server.Router
import org.http4s.server.blaze.BlazeServerBuilder

class CompositionRoot(conf: Conf)(implicit cd: ContextShift[IO], timer: Timer[IO]) {

  private val httpApp = {
    val statusEndpoint = new StatusEndpoint
    val convertEndpoint = {

      val mockRatesProvider = new RatesProvider {
        override def getRate(from: String, to: String): EitherT[IO, ConvertError, BigDecimal] = EitherT.rightT(BigDecimal("2.0"))
      }
      val convertService = new ConvertServiceImpl(mockRatesProvider)

      new ConvertEndpoint(convertService)
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
