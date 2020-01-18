package org.exchange.logic.repo.impl.ratesio.cache

import cats.effect.IO
import scalacache.caffeine.CaffeineCache
import scalacache.{Cache, Mode}

import scala.concurrent.duration.FiniteDuration

trait CacheReader {
  def read(base: String): IO[Map[String, BigDecimal]]
}

trait CacheWriter {
  def write(base: String, rates: Map[String, BigDecimal]): IO[Unit]
}

trait CacheManager extends CacheReader with CacheWriter

class CaffeineCacheManager(ttl: Option[FiniteDuration]) extends CacheManager {

  implicit private val mode: Mode[IO] = scalacache.CatsEffect.modes.async
  private val cache: Cache[Map[String, BigDecimal]] = CaffeineCache[Map[String, BigDecimal]]

  override def write(base: String, rates: Map[String, BigDecimal]): IO[Unit] = {
    cache.put(base)(rates, ttl).map(_ => ())
  }

  override def read(base: String): IO[Map[String, BigDecimal]] = {
    cache.get(base).map(_.fold(Map.empty[String, BigDecimal])(identity))
  }
}
