package org.exchange

import cats.effect.{ContextShift, IO, Timer}
import org.exchange.config.Conf
import org.exchange.server.endpoints.StatusEndpoint
import org.http4s.implicits._
import org.http4s.server.Router
import org.http4s.server.blaze.BlazeServerBuilder

class CompositionRoot(conf: Conf)(implicit cd: ContextShift[IO], timer: Timer[IO]) {

  private val httpApp = {
    val statusEndpoint = new StatusEndpoint

    Router[IO](
      "/" -> statusEndpoint.route()
    ).orNotFound
  }

  val server: IO[Unit] = BlazeServerBuilder[IO]
    .bindHttp(conf.apiConfig.port, conf.apiConfig.host)
    .withHttpApp(httpApp)
    .serve
    .compile
    .drain
}
