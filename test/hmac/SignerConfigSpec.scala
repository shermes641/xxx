package hmac

import models.ConfigVars
import resources.SpecificationWithFixtures

/**
  * Created by shermes on 3/3/16.
  */
class SignerConfigSpec extends SpecificationWithFixtures with ConfigVars {

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

