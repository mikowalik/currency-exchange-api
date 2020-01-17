package org.exchange.server.endpoints

import cats.effect._
import org.http4s.HttpRoutes
import org.http4s.dsl.io._

class StatusEndpoint {
  def route(): HttpRoutes[IO] = {
    HttpRoutes.of[IO] {
      case GET -> Root / "status" =>
        Ok("UP")
    }
  }
}
