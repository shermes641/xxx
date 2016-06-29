package admin

import resources.SpecificationWithFixtures
import play.api.test.Helpers._

class AdminSpec extends SpecificationWithFixtures {

  val existingAdminEmail = "newAdmin1@gmail.com"
  val existingAdmin = running(testApplication) {
    val id = distributorUserService.create(existingAdminEmail, password, companyName).get
    val thisUser = distributorUserService.find(id).get
    db.withConnection { implicit connection =>
      securityRoleService.addUserRole(thisUser.id.get, 1)
    }
    Admin(thisUser.email, thisUser.id.get, securityRoleService)
  }

  "AdminService.find" should {
    "return None if a DistributorUser does not have the admin role" in new WithDB {
      val user = {
        val id = distributorUserService.create("newemail1@gmail.com", password, companyName).get
        distributorUserService.find(id).get
      }
      adminService.find(user.email, user.distributorID.get) must beNone
    }

    "return the role if one exists for the DistributorUser" in new WithDB {
      val user = {
        val id = distributorUserService.create("newemail2@gmail.com", password, companyName).get
        distributorUserService.find(id).get
      }
      val adminUser = Admin(user.email, user.id.get, securityRoleService)
      db.withTransaction { implicit connection =>
        securityRoleService.addUserRole(user.id.get, adminUser.RoleID)
      }
      val adminRole = adminService.find(user.email, user.distributorID.get).get

      adminRole.userEmail must beEqualTo(adminUser.userEmail)
      adminRole.distributorID must beEqualTo(adminUser.distributorID)
      adminRole.RoleID must beEqualTo(adminUser.RoleID)
    }
  }

  "getRoles" should {
    "return a list of available security roles for a user" in new WithDB {
      existingAdmin.getRoles.toArray.toList must contain(SecurityRole(existingAdmin.RoleID, existingAdmin.RoleName))
    }

    "return an empty list if no roles are available for a user" in new WithDB {
      Admin("some-fake-email", 0, securityRoleService).getRoles.toArray must beEmpty
    }
  }

  "getPermissions" should {
    "return an empty array because permissions have not yet been implemented" in new WithDB {
      existingAdmin.getPermissions.toArray must beEmpty
    }
  }

  "getIdentifier" should {
    "return the distributor user's email" in new WithDB {
      existingAdmin.getIdentifier must beEqualTo(existingAdminEmail)
    }
  }
}
