package hmac

import models.CallbackVerificationInfo
import org.specs2.mock.Mockito
import resources.AdProviderRequests

class SignerSpec extends BaseSpec with AdProviderRequests with Mockito {
  val adProviderUserID = "user-id"

  val validData = Table(
    ("secret", "user-id", "estimatedOfferProfit"),
    ("123wsdmnsd,mnt959zx,czm534598676&^^&%&", "user-id1", 12.44),
    ("hobinonk%^&%^34docmv321)&*7", "user-id2", 0.00),
    ("^&*^*^@#!@@SDVMXVxvdklssgkdn5464GEDgsET4t3r2", "user-id3", 94.70),
    ("34%346%^*r67(76uregSDfsEWRq#r!@#r@#T34$%7%7%&(67Iu5RYEWGsd", "user-id4", 77.66),
    ("XZCZCgbdhdfRThdhThrHr6U567ii^&8E4tr@#E2qEqeFwry$%y", "user-id5", 01.76),
    ("695923dzcmzd$32@35@#rwef90zuvzcaef@#RQ?T@ QW$%WGSAETJ56u6%565^*65*^*%$^#", "user-id6", 3.14)
  )

  val validData1 = Table(
    ("adProviderName", "adProviderUserID", "rewardQuantity", "estimatedOfferProfit", "transactionId"),
    ("name1", "user-id", 1, 1, "1212m,m-sad-4545"),
    ("name2", "user-id1", 2, 11, "1212m,m-sad-4545"),
    ("name3", "user-id2", 3, 12, "dalks;k34234sdv,sf.,"),
    ("name4", "user-id3", 4, 13, "rtyirpovbncxzm, q.23420"),
    ("name5", "user-id4", 5, 14, "45,64mt,.df0c,en23eov"),
    ("name6", "user-id5", 6, 15, "rt0-96,gmdqw,.em12eos"),
    ("name7", "user-id6", 7, 16, "121.,xl89m90tfgdksje")
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
      val t3 = Signer.previousTimestamp(t1)
      t1 should (equal(t2) or equal(t3))
    }
    0.to(1000).foreach(x => validateTimestamp)
  }

  property("previousTimestamp is 1 sec less than timestamp") {
    /**
      * Validate timestamp generation
      *
      * @return
      */
    def validateTimestamp = {

      val t1 = Signer.timestamp
      val t2 = Signer.previousTimestamp(t1)
      t1 should equal(t2 + 1)
    }
    0.to(1000).foreach(x => validateTimestamp)
  }

  property("validate HmacHashData with all parameters") {
    forAll(validData1) { (adProviderName: String, adProviderUserID: String, rewardQuantity: Int, estimatedOfferProfit: Int, transactionId: String) =>
      val ep = estimatedOfferProfit.toDouble + 0.73
      val vi = CallbackVerificationInfo(isValid = true,
        adProviderName = adProviderName,
        transactionID = transactionId,
        appToken = "1234",
        offerProfit = Some(ep),
        rewardQuantity = rewardQuantity,
        adProviderRewardInfo = None)
      val pbd = HmacHashData(
        adProviderUserID = adProviderUserID,
        adProviderRequest = adColonyRequest,
        verificationInfo = vi).postBackData

      pbd.value.get(HmacConstants.OriginalPostback).get.equals(adColonyRequest) should be(true)
      pbd.value.get(HmacConstants.AdProviderName).get.toString.contains(adProviderName.toString) should be(true)
      pbd.value.get(HmacConstants.AdProviderUser).get.toString.contains(adProviderUserID.toString) should be(true)
      pbd.value.get(HmacConstants.RewardQuantity).get.toString.equals(rewardQuantity.toString) should be(true)
      pbd.value.get(HmacConstants.OfferProfit).get.toString.toDouble.equals(ep) should be(true)
      pbd.value.get(HmacConstants.TransactionID).get.toString.contains(transactionId.toString) should be(true)
    }
  }

  property("validate HmacHashData with only required parameters") {
    forAll { (rewardQuantity: Int, estimatedOfferProfit: Int) =>
      val ep = estimatedOfferProfit.toDouble + 0.95
      val res = HmacHashData(
        adProviderUserID = adProviderUserID,
        adProviderRequest = adColonyRequest,
        verificationInfo = CallbackVerificationInfo(isValid = true,
          adProviderName = HmacConstants.DefaultAdProviderName,
          transactionID = HmacConstants.DefaultTransactionId,
          appToken = "1234",
          offerProfit = Some(ep),
          rewardQuantity = rewardQuantity,
          adProviderRewardInfo = None))

      res.postBackData.value.get(HmacConstants.OriginalPostback).get.equals(adColonyRequest) should be(true)
      res.postBackData.value.get(HmacConstants.AdProviderName).get.toString.contains(HmacConstants.DefaultAdProviderName) should be(true)
      res.postBackData.value.get(HmacConstants.AdProviderUser).get.toString.contains(adProviderUserID.toString) should be(true)
      res.postBackData.value.get(HmacConstants.RewardQuantity).get.toString.equals(rewardQuantity.toString) should be(true)
      res.postBackData.value.get(HmacConstants.OfferProfit).get.toString.toDouble.equals(ep) should be(true)
      res.postBackData.value.get(HmacConstants.TransactionID).get.toString.contains(HmacConstants.DefaultTransactionId) should be(true)
    }
  }

  val ts = Signer.timestamp

  property("generates the same hashes") {
    forAll(validData) { (secret: String, userID: String, estimatedOfferProfit: Double) =>
      val hd = HmacHashData(
        adProviderUserID = userID,
        adProviderRequest = adColonyRequest,
        verificationInfo = CallbackVerificationInfo(isValid = true,
          adProviderName = "name",
          transactionID = "123456",
          appToken = "1234",
          offerProfit = Some(estimatedOfferProfit),
          rewardQuantity = 1L,
          adProviderRewardInfo = None))

      Signer.generate(secret, hd.toHash(HmacConstants.DefaultSecret).get) shouldEqual Signer.generate(secret, hd.toHash(HmacConstants.DefaultSecret).get)
    }
  }

  property("generates unique hashes") {
    forAll { (s1: String, u1: String, u2: String) =>
      whenever(notEmpty(s1, u1, u2) && u1.length != u2.length) {
        val h1 = HmacHashData(
          adProviderUserID = u1,
          adProviderRequest = adColonyRequest,
          verificationInfo = CallbackVerificationInfo(isValid = true,
            adProviderName = "name",
            transactionID = "123456",
            appToken = "1234",
            offerProfit = Some(1.1),
            rewardQuantity = 1L,
            adProviderRewardInfo = None))

        val h2 = HmacHashData(
          adProviderUserID = u2,
          adProviderRequest = adColonyRequest,
          verificationInfo = CallbackVerificationInfo(isValid = true,
            adProviderName = "name",
            transactionID = "123456",
            appToken = "1234",
            offerProfit = Some(1.1),
            rewardQuantity = 1L,
            adProviderRewardInfo = None))

        Signer.generate(s1, h1.toHash(HmacConstants.DefaultSecret).get) should not be Signer.generate(s1, h2.toHash(HmacConstants.DefaultSecret).get)
      }
    }
  }

  property("validates equals hashes") {
    forAll() { (secret: String, userID: String) =>
      whenever(notEmpty(secret, userID)) {
        val h1 = HmacHashData(
          adProviderUserID = userID,
          adProviderRequest = adColonyRequest,
          verificationInfo = CallbackVerificationInfo(isValid = true,
            adProviderName = "name",
            transactionID = "123456",
            appToken = "1234",
            offerProfit = Some(3.3),
            rewardQuantity = 2L,
            adProviderRewardInfo = None))

        val hash = Signer.generate(secret, h1.postBackData.toString)
        Signer.valid(secret, h1.postBackData.toString, hash.get) should be(true)
      }
    }
  }

  property("valid hashes") {
    forAll {
      (adProviderName: String,
       adProviderUserID: String,
       rewardQuantity: Int,
       estimatedOfferProfit: Double) =>
        whenever(notEmpty(adProviderName, adProviderUserID)) {
          val h1 = HmacHashData(
            adProviderUserID = adProviderUserID,
            adProviderRequest = adColonyRequest,
            verificationInfo = CallbackVerificationInfo(isValid = true,
              adProviderName = adProviderName,
              transactionID = "123456",
              appToken = "1234",
              offerProfit = Some(estimatedOfferProfit),
              rewardQuantity = rewardQuantity,
              adProviderRewardInfo = None))

          val hash = Signer.generate("asflaskfs43^&6ydgdf&*69tTds=", h1.postBackData.toString)
          Signer.valid("asflaskfs43^&6ydgdf&*69tTds=", h1.postBackData.toString, hash.get) should be(true)
        }
    }
  }

  property("invalid hashes") {
    forAll {
      (adProviderName: String,
       adProviderUserID: String,
       rewardQuantity: Int,
       estimatedOfferProfit: Double) =>
        whenever(notEmpty(adProviderName, adProviderUserID)) {
          val secret = ""
          val h1 = HmacHashData(
            adProviderUserID = adProviderUserID,
            adProviderRequest = adColonyRequest,
            verificationInfo = CallbackVerificationInfo(isValid = true,
              adProviderName = adProviderName,
              transactionID = "123456",
              appToken = "1234",
              offerProfit = Some(estimatedOfferProfit),
              rewardQuantity = rewardQuantity,
              adProviderRewardInfo = None))
          Signer.generate(secret, h1.postBackData.toString) should be(None)
        }
    }
  }
}
