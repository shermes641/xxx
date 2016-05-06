package hmac

import java.security.NoSuchAlgorithmException
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec
import javax.xml.bind.DatatypeConverter

import models.{CallbackVerificationInfo, ConfigVars, Environment}
import play.api.libs.json.{JsValue, Json}
import play.api.{Logger, Play}

/**
  * Encapsulate parameters for hmac hash
  *
  * @param adProviderRequest  The original postback from the ad provider.
  * @param verificationInfo   Class containing information to verify the postback and create a new Completion.
  * @param adProviderUserID   The user ID provided by the adProvider
  * @return JSON containing all necessary postback params from our documentation
  */
case class HmacHashData(adProviderRequest: JsValue, verificationInfo: CallbackVerificationInfo, adProviderUserID: String) {
  val postBackData = Json.obj(
    HmacConstants.AdProviderName    -> verificationInfo.adProviderName,
    HmacConstants.OfferProfit       -> verificationInfo.offerProfit,
    HmacConstants.OriginalPostback  -> adProviderRequest,
    HmacConstants.RewardQuantity    -> verificationInfo.rewardQuantity,
    HmacConstants.TimeStamp         -> Signer.timestamp.toString,
    HmacConstants.TransactionID     -> verificationInfo.transactionID,
    HmacConstants.AdProviderUser    -> adProviderUserID
  )

  /**
    * Returns hash of the postBackData
    *
    * @param secret the shared secret used to perform the hash
    * @return Base64 encoded hash
    */
  def toHash(secret: String): Option[String] = {
    Signer.generate(secret, postBackData.toString)
  }
}

trait Signer extends ConfigVars {
  final val getNewTimestamp = None
  var algorithm = HmacConstants.DefaultAlgorithm

  Play.maybeApplication match {
    case Some(application) =>
      algorithm = ConfigVarsHmac.algorithm

    case _ => // defaults already set
  }

  /**
    * Validates a hmac hash
    *
    * @param secret    shared secret (currently the app waterfall ID)
    * @param strToHash String to hash
    * @param hash      Hash to validate
    * @return true if hash matches generated hash
    */
  def valid(secret: String, strToHash: String, hash: String): Boolean

  /**
    * Generate a hash based on hashData
    *
    * @param secret    shared secret (currently the app waterfall ID)
    * @param strToHash String to hash
    * @return generated hash
    */
  def generate(secret: String, strToHash: String): Option[String]
}

trait DefaultSigner extends Signer {
  /**
    * Validate hash against the supplied hashData
    *
    * @param secret    Shared secret
    * @param strToHash String to hash
    * @param hash      The hash to check against
    * @return true if generated hash matches hash handed in
    */
  def valid(secret: String, strToHash: String, hash: String): Boolean = {
      generate(secret, strToHash).contains(hash)
  }

  /**
    *
    * @return number of seconds from epoch
    */
  def timestamp = System.currentTimeMillis / 1000

  /**
    * Used to account for potential server / client time sync issues,
    * and second rollover between generation and validation checks
    *
    * @param timestamp number of seconds from the epoch
    * @return ts minus 1 seconds
    */
  def previousTimestamp(timestamp: Long) = timestamp - 1

  /**
    * Create hash using the supplied parameters
    *
    * @param secret    Shared secret
    * @param strToHash String to hash
    * @return Base64 encoded hash
    * @throws NoSuchAlgorithmException if <tt>algorithm</tt> is not supported.
    * @throws IllegalStateException    if mav.final fails
    */
  def generate(secret: String, strToHash: String): Option[String] = {
    generate(secret, strToHash.getBytes)
  }

  def generate(hmacSecret: String, bytesToHash: Array[Byte]) = {
    if (hmacSecret.isEmpty)
      None
    else {
      if (Environment.isDev || Environment.isStaging || Environment.isTest)
        Logger.info(s"Distributor hmac POST:\n${new String(bytesToHash)}")

      val mac = Mac.getInstance(algorithm)
      mac.init(new SecretKeySpec(hmacSecret.getBytes, algorithm))
      val sig = DatatypeConverter.printBase64Binary(mac.doFinal(bytesToHash))
      Some(sig)
    }
  }
}

object Signer extends DefaultSigner

