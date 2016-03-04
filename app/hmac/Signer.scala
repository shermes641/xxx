package hmac

import java.security.NoSuchAlgorithmException
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec
import javax.xml.bind.DatatypeConverter

import com.netaporter.uri.QueryString
import com.netaporter.uri.dsl._
import oauth.signpost.OAuth.percentEncode
import play.api.Play

/**
  * Encapsulate parameters for hmac hash
  *
  * @param uri                  the uri we are sending the request to
  * @param adProviderName       name of the ad provider
  * @param rewardQuantity       amount of the reward
  * @param estimatedOfferProfit estimated profit
  * @param transactionId        unique transaction ID
  */
case class HmacHashData(uri: String,
                        adProviderName: String = Constants.DefaultAdProviderName,
                        rewardQuantity: Long = 0L,
                        estimatedOfferProfit: Option[Double] = None,
                        transactionId: String = Constants.DefaultTransactionId) {

  val paramSeq = estimatedOfferProfit match {
    case Some(estimated) =>
      Seq("adProviderName" -> adProviderName,
        "rewardQuantity" -> rewardQuantity.toString,
        "transactionId" -> transactionId,
        "estimatedOfferProfit" -> estimatedOfferProfit.get.toString)

    case _ =>
      Seq("adProviderName" -> adProviderName,
        "rewardQuantity" -> rewardQuantity.toString,
        "transactionId" -> transactionId)
  }

  def toQueryParamMap(timestamp: Option[Long], nonce: String, hmacSecret: Option[String]): Seq[(String, String)] = {
    val ts = timestamp.getOrElse(Signer.timestamp)
    val secret = hmacSecret.getOrElse(Constants.DefaultSecret)
    Seq("timestamp" -> ts.toString,
      "nonce" -> nonce,
      "hmac" -> toHash(ts, nonce, secret))
  }

  /**
    * Returns hash of all combined parameters
    *
    * @return
    */
  def toHash(ts: Long, nonce: String, secret: String): String = {
    Signer.generate(secret, this, nonce, ts) match {
      case Some(hash) => hash
      case _ => ""
    }
  }
}

trait Signer {
  final val getNewTimestamp = None
  var algorithm = Constants.DefaultAlgorithm
  var separator = Constants.DefaultSeparator
  var tolerance = Constants.DefaultTolerance.toDouble

  Play.maybeApplication match {
    case Some(application) =>
      algorithm = application.configuration.getString("signerAlgorithm").getOrElse(Constants.DefaultAlgorithm)
      separator = application.configuration.getString("signerSeparator").getOrElse(Constants.DefaultSeparator)
      tolerance = application.configuration.getString("signerTolerance").getOrElse(Constants.DefaultTolerance).toString.toDouble
    case _ => // defaults already set
  }

  /**
    * Validates a hmac hash
    *
    * @param hash     Hash to vlidate
    * @param secret   shared secret (currently the app waterfall ID)
    * @param hashData Data to hash
    * @return true if hash matches generated hash
    */
  def valid(secret: String, hashData: HmacHashData, nonce: String, ts: Long, hash: String): Boolean

  /**
    * Generate a hash based on hashData
    *
    * @param secret    shared secret (currently the app waterfall ID)
    * @param hashData  Data to hash
    * @param timestamp seconds since epoch or truncated to tens or hundreds of seconds for larger time windows
    * @return generated hash
    */
  def generate(secret: String, hashData: HmacHashData, nonce: String, timestamp: Long): Option[String]
}

trait DefaultSigner extends Signer {

  /**
    * Validate hash against the supplied hashData
    *
    * @param hash     The hash to check
    * @param secret   Shared secret
    * @param hashData Data to hash
    * @return true if data was hashed with the current timestamp, or the previous timestamp
    */
  def valid(secret: String, hashData: HmacHashData, nonce: String, ts: Long, hash: String): Boolean = {
    if
    (generate(secret, hashData, nonce, ts).contains(hash)) true
    else
      generate(secret, hashData, nonce, previousTimestamp(ts)).contains(hash)
  }

  /**
    * Trim current ts by tolerance
    * A tolerance of 3.0 makes the timestamp the number of seconds from epoch
    *
    * @return
    */
  def timestamp = System.currentTimeMillis / Math.pow(10, tolerance).toLong

  /**
    * Used to account for potential server / client time sync issues,
    * and establish a time window based on the timestamp resolution
    *
    * @param timestamp number of milliseconds, seconds, 10's of seconds, etc....
    * @return ts minus 1
    */
  def previousTimestamp(timestamp: Long) = timestamp - 1

  /**
    * Create hash using the supplied parameters
    *
    * @param secret    Shared secret
    * @param hashData  Data to hash
    * @param nonce     a unique string
    * @param timestamp timestamp for hash
    * @return Base64 encoded hash
    * @throws NoSuchAlgorithmException if <tt>algorithm</tt> is not supported.
    * @throws IllegalArgumentException if <tt>secret</tt> is null.
    * @throws IllegalStateException    if mav.final fails
    */
  def generate(secret: String, hashData: HmacHashData, nonce: String, timestamp: Long): Option[String] = {
    ParseUri(hashData.uri ? ("nonce" -> nonce) & ("timestamp" -> timestamp.toString)).parsed match {
      case Some(encodedUri) =>
        //@formatter:off
        val strForHashing = timestamp + separator +     // seconds since epoch for request
          nonce + separator +                           // any unique string (we use transaction_id)
          encodeParams(hashData.paramSeq) + separator + // data sent with POST
          Constants.DefaultMethod + separator +         // this is always a POST
          percentEncode(hashData.uri) + separator +     // encoded callback uri
          encodedUri.port + separator +                 // port for the http / https request
          encodedUri.query + separator +                // encoded query string nonce & timestamp
          separator                                     // ending empty line
      //@formatter:on

        val mac = Mac.getInstance(algorithm)
        mac.init(new SecretKeySpec(secret.getBytes, algorithm))
        Some(DatatypeConverter.printBase64Binary(mac.doFinal(strForHashing.getBytes)))

      case _ =>
        None
    }
  }

  /**
    * Sort the param data for server to server calls
    *
    * @param paramSeq Sequence of parameters
    * @return String of encoded and sorted parameters
    */
  def encodeParams(paramSeq: Seq[(String, String)]): String = {
    paramSeq.map {
      case (key, value) => percentEncode(key) -> percentEncode(value)
    }.sorted.map {
      case (key, value) => s"$key=$value"
    }.mkString(separator)
  }
}

/**
  * Hold Uri elements --- convience class
  *
  * @param scheme    the scheme of the uri (http https etc...)
  * @param port      port for the uri (http = 80 https = 443)
  * @param method    method  (GET POST etc.....)
  * @param hostParts the parts of the host (elements seperated by ".")
  * @param path      path of the uri
  * @param query     query string of the uri
  * @param fragment  The fragment introduced by an optional hash mark #
  */
case class UriElements(scheme: String,
                       port: Int,
                       method: String,
                       hostParts: Seq[String],
                       path: String,
                       query: QueryString,
                       fragment: Option[String])

/**
  * Parse out uri elements for convience
  *
  * @param uri    uri to parse
  * @param method the method to use
  */
case class ParseUri(uri: Serializable, method: String = "POST") {
  // url encoded version of uri
  val uriStr = uri.toString
  val parsed = if (!uriStr.isEmpty && uriStr.scheme.isDefined) {
    Some(UriElements(
      uriStr.scheme.get.toLowerCase,
      uriStr.port.getOrElse({
        if (uriStr.scheme.get.contains("https")) Constants.httpsPort else Constants.httpPort
      }),
      method.toUpperCase,
      uriStr.hostParts,
      uriStr.path,
      uriStr.query,
      uriStr.fragment
    ))
  } else {
    None
  }
}

object Signer extends DefaultSigner

