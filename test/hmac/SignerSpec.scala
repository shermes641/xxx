package hmac

import org.specs2.mock.Mockito
import play.api.Play

class SignerSpec extends BaseSpec with Mockito{

  val validUris = Table(
    ("secret",
      "uri"),
    ("123wsdmnsd,mnt959zx,czm534598676&^^&%&", "https://x.com"),
    ("hobinonk%^&%^34docmv321)&*7", "https://x with spaces.com"),
    ("^&*^*^@#!@@SDVMXVxvdklssgkdn5464GEDgsET4t3r2", "http://steve.net/goodTimes/"),
    ("34%346%^*r67(76uregSDfsEWRq#r!@#r@#T34$%7%7%&(67Iu5RYEWGsd", "https://x.com"),
    ("XZCZCgbdhdfRThdhThrHr6U567ii^&8E4tr@#E2qEqeFwry$%y", "https://x.com"),
    ("695923dzcmzd$32@35@#rwef90zuvzcaef@#RQ?T@ QW$%WGSAETJ56u6%565^*65*^*%$^#", "https://x.com")
  )

  val multipleValidUris = Table(
    ("s1",
      "u1",
      "s2",
      "u2"),
    ("123wsdmnsd,mnt959zx,czm534598676&^^&%&", "https://x.com", "hobinonk%^&%^34docmv321)&*7", "https://x with spaces.com"),
    ("^&*^*^@#!@@SDVMXVxvdklssgkdn5464GEDgsET4t3r2", "http://steve.net/goodTimes/", "34%346%^*r67(76uregSDfsEWRq#r!@#r@#T34$%7%7%&(67Iu5RYEWGsd", "https://x.com"),
    ("XZCZCgbdhdfRThdhThrHr6U567ii^&8E4tr@#E2qEqeFwry$%y", "https://x.com", "695923dzcmzd$32@35@#rwef90zuvzcaef@#RQ?T@ QW$%WGSAETJ56u6%565^*65*^*%$^#", "https://x.com")
  )

  property("generates the same timestamps") {

    /**
      * Validate timestamp generation
      *
      * @return
      */
    def validateTimestamp = {
      val t1 = Signer.timestamp
      val t2 = Signer.timestamp
      val t3 = Signer.previousTimestamp(t2)
      t1 should (equal(t2) or equal(t3))
    }

    0.to(1000).foreach(x => validateTimestamp)
  }

  property("validate HmacHashData with only required parameters") {
    forAll {
      (uri: String,
       rewardQuantity: Int,
       estimatedOfferProfit: Option[Double]) =>
        val res = HmacHashData(uri = uri, rewardQuantity = rewardQuantity, estimatedOfferProfit = estimatedOfferProfit)
        res.uri should be(uri)
        res.adProviderName should be(Constants.DefaultAdProviderName)
        res.rewardQuantity should be(rewardQuantity)
        res.estimatedOfferProfit should be(estimatedOfferProfit)
        res.transactionId should be(Constants.DefaultTransactionId)
    }
  }

  property("validate HmacHashData with all parameters") {
    forAll {
      (uri: String,
       adProviderName: String,
       rewardQuantity: Int,
       estimatedOfferProfit: Option[Double],
       transactionId: String) =>
        val res = HmacHashData(uri, adProviderName, rewardQuantity, estimatedOfferProfit, transactionId)
        res.uri should be(uri)
        res.adProviderName should be(adProviderName)
        res.rewardQuantity should be(rewardQuantity)
        res.estimatedOfferProfit should be(estimatedOfferProfit)
        res.transactionId should be(transactionId)
    }
  }

  val testUri = "http://somwwhere.com/"
  val ts = Signer.timestamp

  property("generates the same hashes") {
    forAll(validUris) { (secret: String, uri: String) =>
      val hd = HmacHashData(uri = uri)
      Signer.generate(secret, hd, Constants.DefaultNonce, ts) shouldEqual
        Signer.generate(secret, hd, Constants.DefaultNonce, ts)
    }
  }

  property("generates unique hashes") {
    forAll { (s1: String, u1: String, s2: String, u2: String) =>
      whenever(notEmpty(s1, u1, s2, u2) && s1 != s2 && u1 != u2) {
        Signer.generate(s1, HmacHashData(uri = testUri + u1), Constants.DefaultNonce, ts) should not be
          Signer.generate(s2, HmacHashData(uri = testUri + u2), Constants.DefaultNonce, ts)
      }
    }
  }

  property("validates equals hashes") {
    val ts = Signer.timestamp
    forAll(validUris) { (secret: String, uri: String) =>
      val hash = Signer.generate(secret, HmacHashData(uri = uri), Constants.DefaultNonce, ts)
      Signer.valid(secret, HmacHashData(uri = uri), Constants.DefaultNonce, ts, hash.get) should be(true)
    }
  }


  property("validates different hashes") {
    val ts = Signer.timestamp
    forAll(multipleValidUris) { (s1: String, u1: String, s2: String, u2: String) =>
      val hash = Signer.generate(s1, HmacHashData(uri = u1), Constants.DefaultNonce, ts)
      Signer.valid(s2, HmacHashData(uri = u2), Constants.DefaultNonce, ts, hash.get) should be(false)
    }
  }

  property("valid hashes") {
    val ts = Signer.timestamp
    forAll {
      (adProviderName: String,
       rewardQuantity: Int,
       estimatedOfferProfit: Option[Double]) =>
        val hd = HmacHashData("http://some uri", adProviderName, rewardQuantity, estimatedOfferProfit)
        val hash = Signer.generate("asflaskfs43^&6ydgdf&*69tTds=", hd, Constants.DefaultNonce, ts)
        Signer.valid("asflaskfs43^&6ydgdf&*69tTds=", hd, Constants.DefaultNonce, ts, hash.get) should be(true)
    }
  }

  property("invalid hashes") {
    forAll {
      (uri: String,
       adProviderName: String,
       rewardQuantity: Int,
       estimatedOfferProfit: Option[Double]) =>
        val secret = ""
        val hd = HmacHashData(uri, adProviderName, rewardQuantity, estimatedOfferProfit)
        Signer.generate(secret, hd, Constants.DefaultNonce, Signer.timestamp) should be(None)
    }
  }
}




