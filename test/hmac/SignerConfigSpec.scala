package hmac

import resources.SpecificationWithFixtures

/**
  * Created by shermes on 3/3/16.
  */
class SignerConfigSpec extends SpecificationWithFixtures {

  "Signer" should {
    "use default config parameters" in {
      Signer.algorithm must_== Constants.DefaultAlgorithm
      Signer.separator must_== Constants.DefaultSeparator
      Signer.tolerance must_== Constants.DefaultTolerance
    }

    "use parameters from config file" in new WithDB {
      object MySigner extends DefaultSigner
      MySigner.algorithm must_== app.configuration.getString("signerAlgorithm").getOrElse("should never happen")
      MySigner.separator must_== app.configuration.getString("signerSeparator").getOrElse("should never happen")
      MySigner.tolerance must_== app.configuration.getString("signerTolerance").getOrElse("should never happen").toDouble
    }
  }
}

