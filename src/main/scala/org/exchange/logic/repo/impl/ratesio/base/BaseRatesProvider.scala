package org.exchange.logic.repo.impl.ratesio.base

import cats.data.EitherT
import cats.effect.IO
import io.circe.generic.auto._
import org.exchange.logic.errors.{BaseCurrencyNotSupportedError, ConvertError, RatesIOResponseError, UriBuildingError}
import org.exchange.logic.repo.impl.ratesio.RatesIO
import org.exchange.logic.repo.impl.ratesio.model.RatesIOResponse
import org.exchange.model.Currency
import org.http4s.Uri
import org.http4s.circe.CirceEntityCodec._
import org.http4s.client.Client
import RatesIOResponse.decoder

trait BaseRatesProvider {
  def getRates(from: Currency): EitherT[IO, ConvertError, RatesIOResponse]
}

class HttpBaseRatesProvider(client: Client[IO]) extends BaseRatesProvider {
  override def getRates(from: Currency): EitherT[IO, ConvertError, RatesIOResponse] = {
    for {
      uri <- EitherT.fromEither[IO](buildUri(from))
      resp <- executeAndValidateRequest(uri, from)
    } yield resp
  }

  private def executeAndValidateRequest(uri: Uri, from: Currency): EitherT[IO, ConvertError, RatesIOResponse] = {
    EitherT(client.expect[RatesIOResponse](uri).attempt)
      .leftMap {
        ex: Throwable =>
          if(ex.getMessage.contains("400 Bad Request")) BaseCurrencyNotSupportedError(from)
          else  RatesIOResponseError(ex)
      }
  }

  private def buildUri(from: Currency): Either[ConvertError, Uri] = {
    import RatesIO._
    Uri.fromString(latestUri)
      .map(_.withQueryParam("base", from.value))
      .left
      .map(_ => UriBuildingError(latestUri, from))
  }
}
