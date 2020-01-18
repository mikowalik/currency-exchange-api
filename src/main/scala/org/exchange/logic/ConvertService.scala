package org.exchange.logic

import cats.effect.IO
import cats.data.EitherT
import org.exchange.logic.errors.ConvertError
import org.exchange.logic.repo.RatesProvider
import org.exchange.model.{ConvertInput, ConvertOutput}

trait ConvertService {
  def convert(input: ConvertInput): EitherT[IO, ConvertError, ConvertOutput]
}

class ConvertServiceImpl(ratesProvider: RatesProvider) extends ConvertService {
  override def convert(input: ConvertInput): EitherT[IO, ConvertError, ConvertOutput] = {
    for {
      rate <- ratesProvider.getRate(input.fromCurrency, input.toCurrency)
      output <- EitherT.liftF(IO.pure(ConvertOutput(
        exchange = rate,
        amount = input.amount * rate,
        original = input.amount
      )))
    } yield output
  }
}
