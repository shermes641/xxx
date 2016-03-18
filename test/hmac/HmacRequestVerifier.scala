package hmac

/**
  * Created by shermes on 1/28/16.
  *
  * This object's functionality is to verify a HMAC signed request
  * We currently have no need for this functionality except in functional tests
  * If / when we have this requirement, we will move this package into the runtime code
  */

import play.api.Logger
import play.api.mvc.RequestHeader

import scala.util.parsing.json.JSON
import scala.util.{Failure, Success, Try}

/**
  * Verifies HMAC requests
  *
  * This is provided to test the HMAC query string we generate on server to server callbacks
  * Distributers could use it as an example on how to validate the signed POST's we send them
  *
  */
object HmacRequestVerifier {

  final val verifyTs = "ts"
  final val verifyNonce = "nonce"
  final val verifyHmac = "hmac"
  final val quote = "\""

  /**
    * Parse body into the POST parameters
    *
    * @param body request body -- assumes it is json
    * @return Map of POST parameters
    */
  def requestBodyToMap(body: Array[Byte]) = {
    JSON.parseFull(new String(body, "UTF-8")).get.asInstanceOf[Map[String,String]]
  }

  /**
    * Verify that the given request is a valid server to server request given the hmacSecret
    *
    * @param requestHeader The http/https request header
    * @param body          The http/https request body (if any)
    * @param hostUrl       The http/https request host
    * @param hmacSecret    The shared HMAC secret
    * @return True if hash is valid
    */
  def verifyRequest(requestHeader: RequestHeader,
                    body: Array[Byte],
                    hostUrl: String,
                    nonce: String,
                    hmacSecret: String): Boolean = {

    // Get the expected HMAC query parameters
    Try(Map(verifyTs -> requestHeader.getQueryString("timestamp").get.toString.toLong,
      verifyNonce -> requestHeader.getQueryString(verifyNonce).get,
      verifyHmac -> requestHeader.getQueryString(verifyHmac).get)) match {
      case Failure(ex) =>
        // This is not a failure, it just means the sender did not HMAC sign the request,
        // or signed it incorrectly, so we are done
        // TODO If / When we care about signing we will reject the request
        Logger.warn(s"Request had a missing or invalid HMAC signature: ${ex.getLocalizedMessage}")
        Constants.missingOrInvalidHmacSignature

      case Success(res) =>
        if (res.get(verifyTs).get.toString.toLong > (System.currentTimeMillis() / 1000 - Utils.toleranceToSecs())) {
          Logger.warn(s"Timestamp on  HMAC signature too old")
          Constants.timestampTooOld
        } else {
          val baseUri = hostUrl
          val bodyMap = requestBodyToMap(body)

          val queryParamMap = HmacHashData(
            uri = baseUri,
            adProviderName = bodyMap.getOrElse(Constants.adProviderName, Constants.DefaultAdProviderName),
            //TODO rewardQuantity json is converted as X.X, hence the need for the extra .toDouble conversion
            rewardQuantity = bodyMap.getOrElse(Constants.rewardQuantity, Constants.DefaultRewardQuantity).toString.toDouble.toLong,
            estimatedOfferProfit = Some(bodyMap.getOrElse(Constants.offerProfit, None).toString.toDouble),
            transactionId = bodyMap.getOrElse(Constants.transactionID, Constants.DefaultTransactionId)
          ).toQueryParamMap(Signer.getNewTimestamp, nonce, Some(hmacSecret)).toMap

          queryParamMap map {
            case (key, value) =>
              if (requestHeader.getQueryString(key).toString != Some(value).toString)
                s"$key does not match\n ${requestHeader.getQueryString(key)} \n ${Some(value)}\n"
          } match {
            case List((), (), ()) => true

            case em =>
              em.filterNot(_.toString == "()").foreach((msg: Any) => Logger.warn(msg.toString))
              false
          }
        }
    }
  }
}


