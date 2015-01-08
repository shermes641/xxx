package resources

import models.{Distributor, DistributorUser}

/**
 * Enables creation of new Distributors and DistributorUsers in tests.
 */
trait DistributorUserSetup extends DefaultUserValues {
  /**
   * Helper function to create a new Distributor and DistributorUser.
   * @param email The email for the new DistributorUser.
   * @param password The password for the new DistributorUser.
   * @param companyName The name of the new Distributor.
   * @return A tuple containing the DistributorUser and Distributor.
   */
  def newDistributorUser(email: String = email, password: String = password, companyName: String = companyName): (DistributorUser, Distributor) = {
    val userID = DistributorUser.create(email, password, companyName).get
    val user = DistributorUser.findByEmail(email).get
    DistributorUser.setActive(user)
    val distributor = Distributor.find(user.distributorID.get).get
    (user, distributor)
  }
}
