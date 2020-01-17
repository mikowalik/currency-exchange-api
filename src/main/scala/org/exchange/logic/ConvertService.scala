package org.exchange.logic

import cats.effect.IO
import org.exchange.logic.errors.ConvertError
import org.exchange.model.{ConvertInput, ConvertOutput}

trait ConvertService {
  def convert(input: ConvertInput): IO[Either[ConvertError, ConvertOutput]]
}
