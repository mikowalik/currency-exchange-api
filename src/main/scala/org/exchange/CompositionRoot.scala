package org.exchange

import java.util.concurrent.Executors

import cats.effect.{ContextShift, IO, Timer}
import org.exchange.config.Conf
import org.exchange.logic.ConvertServiceImpl
import org.exchange.logic.repo.impl.ratesio.base.HttpBaseRatesProvider
import org.exchange.logic.repo.impl.ratesio.cache.CaffeineCacheManager
import org.exchange.logic.repo.impl.ratesio.{CacheBasedRateProvider, OnDemandRateProvider}
import org.exchange.server.endpoints.{ConvertEndpoint, StatusEndpoint}
import org.http4s.client.{Client, JavaNetClientBuilder}
import org.http4s.implicits._
import org.http4s.server.Router
import org.http4s.server.blaze.BlazeServerBuilder

import scala.concurrent.ExecutionContext

class CompositionRoot(conf: Conf)(implicit cd: ContextShift[IO], timer: Timer[IO]) {

  private val httpApp = {

    val blockingEC = ExecutionContext.fromExecutorService(Executors.newFixedThreadPool(conf.httpConfig.poolSize))
    val httpClient: Client[IO] = JavaNetClientBuilder[IO](blockingEC)
      .withConnectTimeout(conf.httpConfig.connectTimeout)
      .withReadTimeout(conf.httpConfig.readTimeout)
      .create

    val statusEndpoint = new StatusEndpoint
    val convertEndpoint = {

      val baseRatesProvider = new HttpBaseRatesProvider(httpClient)

      val rateProvider = {
        if (conf.cacheConfig.use) {
          val cacheManager = new CaffeineCacheManager(conf.cacheConfig.ttl)
          new CacheBasedRateProvider(baseRatesProvider, cacheManager)
        }
        else new OnDemandRateProvider(baseRatesProvider)
      }
      val convertService = new ConvertServiceImpl(rateProvider)

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
