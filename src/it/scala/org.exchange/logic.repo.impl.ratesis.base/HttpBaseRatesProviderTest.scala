package org.exchange.logic.repo.impl.ratesio.base

import cats.effect.{ContextShift, IO}
import org.http4s.client.{Client, JavaNetClientBuilder}
import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.should.Matchers

import scala.concurrent.ExecutionContext.Implicits.global


class HttpBaseRatesProviderTest extends AnyFunSuite with Matchers {

  test("Getting data from api.exchangeratesapi.io") {

    implicit val cs: ContextShift[IO] = IO.contextShift(global)

    val httpClient: Client[IO] = JavaNetClientBuilder[IO](global).create

    val provider = new HttpBaseRatesProvider(httpClient)

    val result = provider.getRates("PLN").value.unsafeRunSync()

    result.isRight shouldEqual true
    result.getOrElse(???).rates.size should be > 30
  }
}
