package org.exchange.config

import pureconfig._
import pureconfig.generic.auto._

final case class ApiConfig(
  host: String,
  port: Int
)

final case class Conf(
  apiConfig: ApiConfig
)

object Conf {
  def load(): Conf = {
    ConfigSource.default.loadOrThrow[Conf]
  }
}
