package org.exchange.config

import pureconfig._
import pureconfig.generic.auto._

import scala.concurrent.duration.FiniteDuration

final case class ApiConfig(
  host: String,
  port: Int
)

final case class HttpConfig(
  poolSize: Int,
  connectTimeout: FiniteDuration,
  readTimeout: FiniteDuration
)

final case class CacheConfig(
  use: Boolean,
  ttl: Option[FiniteDuration]
)

final case class Conf(
  apiConfig: ApiConfig,
  httpConfig: HttpConfig,
  cacheConfig: CacheConfig
)

object Conf {
  def load(): Conf = {
    ConfigSource.default.loadOrThrow[Conf]
  }
}
