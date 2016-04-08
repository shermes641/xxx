package hmac

import org.specs2.mock.Mockito
import resources.SpecificationWithFixtures

/**
  * Created by shermes on 3/3/16.
  */
class SignerConfigSpec extends SpecificationWithFixtures with Mockito {
  "Signer" should {
    "use default config parameters" in {
      val signer = new Signer(configVars)
      signer.algorithm must_== HmacConstants.DefaultAlgorithm
    }

    "use parameters from config file" in new WithDB {
      val signer = new Signer(configVars)
      signer.algorithm must_== configVars.ConfigVarsHmac.algorithm
    }
  }
}
