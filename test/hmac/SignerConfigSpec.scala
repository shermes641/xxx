package hmac

import models.ConfigVars
import resources.SpecificationWithFixtures

/**
  * Created by shermes on 3/3/16.
  */
class SignerConfigSpec extends SpecificationWithFixtures with ConfigVars {

  "Signer" should {
    "use default config parameters" in {
      Signer.algorithm must_== Constants.DefaultAlgorithm
      Signer.separator must_== Constants.DefaultSeparator
      Signer.tolerance must_== Constants.DefaultTolerance
    }

    "use parameters from config file" in new WithDB {
      object MySigner extends DefaultSigner
      MySigner.algorithm must_== ConfigVarsHmac.algorithm
      MySigner.separator must_== ConfigVarsHmac.seperator
      MySigner.tolerance must_== ConfigVarsHmac.tolerance
    }
  }
}

