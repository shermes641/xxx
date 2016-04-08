package hmac

import play.api.Logger
import play.api.mvc.RequestHeader

import scala.collection.mutable
import scala.util.parsing.json.JSON
import scala.util.{Failure, Success, Try}

/**
  * Verifies HMAC requests
  *
  * This object's functionality is to verify a HMAC signed request
  * We currently have no need for this functionality except in functional tests
  * If / when we have this requirement, we will move this package into the runtime code
  */
object HmacRequestVerifier {

  final val verifyHmac = "hmac"
  final val quote = "\""

  /**
    * Verify that the given request is a valid server to server request given the hmacSecret
    *
    * @param requestHeader The http/https request header
    * @param body          The http/https request body (if any)
    * @param hmacSecret    The shared HMAC secret
    * @param signer        Shared instance of the Signer class
    * @return True if hash is valid
    */
  def verifyRequest(requestHeader: RequestHeader,
                    body: Array[Byte],
                    hmacSecret: String,
                    signer: Signer): Boolean = {

    // Get the expected HMAC query parameters
    Try(Map(verifyHmac -> requestHeader.getQueryString(verifyHmac).get)) match {
      case Failure(ex) =>
        // This is not a failure, it just means the sender did not HMAC sign the request,
        // or signed it incorrectly, so we are done
        // TODO If / When we care about signing we will reject the request
        Logger.warn(s"Request had a missing or invalid HMAC signature: ${ex.getLocalizedMessage}")
        HmacConstants.MissingOrInvalidHmacSignature

      case Success(res) =>
        val bodyMap = JSON.parseFull(new String(body, "UTF-8")).get.asInstanceOf[Map[String, String]]
        val bodyStr = new String(body)
        val timeStamp = bodyMap.get(HmacConstants.TimeStamp).get.toString.toLong

        // value: Map(reward_quantity -> 2.0, estimated_offer_profit -> 0.5, ad_provider -> no provider name, user_id -> user-id, transaction_id -> no trans id, original_postback -> Map())
        //based on test data TODO hand in the values to validate
        bodyMap.get(HmacConstants.AdProviderName).get == HmacConstants.DefaultAdProviderName &&
          bodyMap.get(HmacConstants.AdProviderUser).get == "user-id" &&
          bodyMap.getOrElse(HmacConstants.RewardQuantity, 0).toString.toDouble.toLong == 2.0 &&
          bodyMap.getOrElse(HmacConstants.OfferProfit, 0.0).toString.toDouble == 0.5 &&
          bodyMap.get(HmacConstants.TransactionID).get == HmacConstants.DefaultTransactionId &&
          Math.abs((System.currentTimeMillis / 1000) - timeStamp) < 5000 match {
          case false =>
            Logger.error(s"Body parameter check failed body: $bodyStr")
            false

          case _ =>
            requestHeader.queryString.size match {
              case 2 =>
                if (requestHeader.queryString.head._1.equals("hmac")) {
                  val sig = signer.generate(hmacSecret, body)
                  requestHeader.queryString.head._2 == mutable.Buffer(sig.get)
                } else {
                  Logger.warn(s"'hmac' missing from query string: ${requestHeader.queryString} ")
                  false
                }
		
              case _ =>
                Logger.warn(s"Wrong number of query string params: ${requestHeader.queryString} ")
                false
            }
        }
    }
  }
}


