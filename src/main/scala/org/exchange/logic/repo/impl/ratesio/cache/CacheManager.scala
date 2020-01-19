package org.exchange.logic.repo.impl.ratesio.cache

import cats.effect.IO
import org.exchange.model.{Currency, Rate}
import scalacache.caffeine.CaffeineCache
import scalacache.{Cache, Mode}

import scala.concurrent.duration.FiniteDuration

trait CacheReader {
  def read(base: Currency): IO[Map[Currency, Rate]]
}

trait CacheWriter {
  def write(base: Currency, rates: Map[Currency, Rate]): IO[Unit]
}

trait CacheManager extends CacheReader with CacheWriter

class CaffeineCacheManager(ttl: Option[FiniteDuration]) extends CacheManager {

  implicit private val mode: Mode[IO] = scalacache.CatsEffect.modes.async
  private val cache: Cache[Map[Currency, Rate]] = CaffeineCache[Map[Currency, Rate]]

  override def write(base: Currency, rates: Map[Currency, Rate]): IO[Unit] = {
    cache.put(base)(rates, ttl).map(_ => ())
  }

  override def read(base: Currency): IO[Map[Currency, Rate]] = {
    cache.get(base).map(_.fold(Map.empty[Currency, Rate])(identity))
  }
}
