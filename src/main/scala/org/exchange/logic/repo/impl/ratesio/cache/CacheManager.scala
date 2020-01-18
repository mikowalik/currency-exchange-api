package org.exchange.logic.repo.impl.ratesio.cache

import cats.effect.IO

trait CacheReader {
  def read(base: String): IO[Map[String, BigDecimal]]
}

trait CacheWriter {
  def write(base: String, rates: Map[String, BigDecimal]): IO[Unit]
}

trait CacheManager extends CacheReader with CacheWriter
