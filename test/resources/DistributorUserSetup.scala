package resources

import models.{DistributorService, DistributorUserService, Distributor, DistributorUser}

/**
 * Enables creation of new Distributors and DistributorUsers in tests.
 */
trait DistributorUserSetup extends DefaultUserValues {
  val userService: DistributorUserService
  val distributorModel: DistributorService
  /**
   * Helper function to create a new Distributor and DistributorUser.
   * @param email The email for the new DistributorUser.
   * @param password The password for the new DistributorUser.
   * @param companyName The name of the new Distributor.
   * @return A tuple containing the DistributorUser and Distributor.
   */
  def newDistributorUser(email: String = email, password: String = password, companyName: String = companyName): (DistributorUser, Distributor) = {
    val userID = userService.create(email, password, companyName).get
    val user = userService.findByEmail(email).get
    val distributor = distributorModel.find(user.distributorID.get).get
    (user, distributor)
  }
}
