# Currency exchange api

Goal of this project is to write application allowing user to calculate conversion rate from one currency to another.
Rates are loaded from api.exchangeratesapi.io.

## How to run
```
sbt run
```

## Calculate conversion
```
curl -i -X POST -H "Content-Type: application/json" -d '{"fromCurrency":"PLN", "toCurrency": "GBP", "amount": 202.6}' http://localhost:8080/api/convert
```

## Technologies
1. Http4s - web server and http client
2. cats-effect - basing on IO effect
3. circe - json handling
4. Caffeine - cache

### Configuration
application.conf contains configuration - among standard properties it is possible to turn off caching.

## Tests
Thanks to single responsibilities most logic is tested with standard unit tests. There is one integration test fro getting data from api.exchangeratesapi.io.
```
sbt it:test
```

## Possible improvements
0. Wrapping raw types - introducing ADT
1. Retrying requests to api.exchangeratesapi.io
2. Better error handling
3. Tagless final? I didn't use Tagless final due to Premature Indirection - i know i wont be changing my effects, but even now implementing tagless final should'n be difficult. 
4. Better README :)
