package org.exchange

import cats.effect.{ExitCode, IO, IOApp}
import cats.implicits._
import org.exchange.config.Conf

object Main extends IOApp with Logging {
  def run(args: List[String]): IO[ExitCode] = {
    (for {
      _ <- IO(logger.info("STARTING SERVER..."))
      conf <- IO.pure(Conf.load())
      server <- new CompositionRoot(conf).server
    } yield server)
      .as(ExitCode.Success)
  }
}
