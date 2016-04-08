package admin

import models.DistributorUser
import play.api.db.DB
import resources.SpecificationWithFixtures

class AdminSpec extends SpecificationWithFixtures {
  "Admin.find" should {
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
      DB.withTransaction { implicit connection =>
        securityRoleService.addUserRole(user.id.get, adminUser.RoleID)
      }
      val adminRole = adminService.find(user.email, user.distributorID.get).get

      adminRole.userEmail must beEqualTo(adminUser.userEmail)
      adminRole.distributorID must beEqualTo(adminUser.distributorID)
      adminRole.RoleID must beEqualTo(adminUser.RoleID)
    }
  }
}
