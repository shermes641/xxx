package hmac

import models.ConfigVars
import resources.SpecificationWithFixtures

class SignerConfigSpec extends SpecificationWithFixtures with ConfigVars {
  sequential
  "Signer" should {
    "use default config parameters" in {
      Signer.algorithm must_== HmacConstants.DefaultAlgorithm
    }

    "use parameters from config file" in new WithDB {
      object MySigner extends DefaultSigner
      MySigner.algorithm must_== ConfigVarsHmac.algorithm
    }
  }
}

