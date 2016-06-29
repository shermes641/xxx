package admin

import javax.inject.Inject

import be.objectify.deadbolt.scala.DeadboltActions
import be.objectify.deadbolt.scala.cache.HandlerCache
import play.libs.Scala

import scala.language.postfixOps

/**
 * The DeadboltRole class is extended by all role-based child classes (e.g. Admin)
 * The child classes will implement the necessary Deadbolt functions (e.g. getRoles, getPermissions, etc).
 */
abstract class DeadboltRole extends be.objectify.deadbolt.core.models.Subject {
  val RoleID: Int
  val userEmail: String
  val distributorID: Long
}

/**
 * Encapsulates user information for the Admin role
 * @param email The email of the DistributorUser
 * @param id    The ID of the Distributor to which the DistributorUser belongs
 */
case class Admin(email: String, id: Long, securityRoleService: SecurityRoleService) extends DeadboltRole {
  val RoleID: Int = 1
  val RoleName = "ADMIN"
  val userEmail: String = email
  val distributorID: Long = id

  /**
   * Must be implemented for Deadbolt
   * @return A list of SecurityRoles for a DistributorUser's email
   */
  def getRoles: java.util.List[SecurityRole] = {
    Scala.asJava(
      securityRoleService.availableRoles(email, distributorID)
    )
  }

  /**
   * Must be implemented for Deadbolt.
   * @return An empty list for now since we don't have a need for permissions.
   */
  def getPermissions = Scala.asJava(List())

  /**
   * Must be implemented for Deadbolt
   * @return The DistributorUser's email
   */
  def getIdentifier: String = email
}

class AdminService @Inject() (securityRoleService: SecurityRoleService) {
  /**
   * Finds an instance of the Admin class
   * @param email         The email of the DistributorUser
   * @param distributorID The ID of the Distributor to which the DistributorUser belongs
   * @return              An instance of the Admin class if the role exists; otherwise, None
   */
  def find(email: String, distributorID: Long): Option[DeadboltRole] = {
    val role = Admin(email, distributorID, securityRoleService)
    if(securityRoleService.exists(role)) Some(role) else None
  }
}
