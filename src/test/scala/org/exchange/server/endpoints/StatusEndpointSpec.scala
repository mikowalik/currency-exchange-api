package org.exchange.server.endpoints

import cats.effect.IO
import org.http4s._
import org.http4s.client.dsl.Http4sClientDsl
import org.http4s.dsl.Http4sDsl
import org.http4s.implicits._
import org.http4s.server.Router
import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.should.Matchers

class StatusEndpointSpec extends AnyFunSuite
  with Matchers
  with Http4sDsl[IO]
  with Http4sClientDsl[IO] {

  test("Status UP") {

    val endpoint = new StatusEndpoint().route()
    val routes = Router("/" -> endpoint).orNotFound

    (for {
      req <- GET(uri"/status")
      resp <- routes.run(req)
      respBody <- resp.as[String]
    } yield {
      resp.status shouldEqual Ok
      respBody shouldEqual "UP"
    }).unsafeRunSync
  }
}
