package admin

import models.DistributorUser
import play.api.db.DB
import resources.SpecificationWithFixtures

class SecurityRoleSpec extends SpecificationWithFixtures {
  "securityRoleService.availableRoles" should {
    "return an empty list if no roles exist for a DistributorUser" in new WithDB {
      val user = {
        val id = distributorUserService.create("newemail1@jungroup.com", password, companyName).get
        distributorUserService.find(id).get
      }
      securityRoleService.availableRoles(user.email, user.distributorID.get) must beEqualTo(List())
    }

    "return all existing roles for a DistributorUser" in new WithDB {
      val user = {
        val id = distributorUserService.create("newemail2@jungroup.com", password, companyName).get
        distributorUserService.find(id).get
      }
      val adminUser = Admin(user.email, user.id.get, securityRoleService)
      db.withTransaction { implicit connection =>
        securityRoleService.addUserRole(user.id.get, adminUser.RoleID)
      }
      val roles = securityRoleService.availableRoles(user.email, user.distributorID.get)
      roles must beEqualTo(List(SecurityRole(adminUser.RoleID, adminUser.RoleName)))
    }
  }

  "securityRoleService.addUserRole" should {
    "add to the existing roles for a DistributorUser" in new WithDB {
      val user = {
        val id = distributorUserService.create("newemail3@jungroup.com", password, companyName).get
        distributorUserService.find(id).get
      }
      val adminUser = Admin(user.email, user.id.get, securityRoleService)

      securityRoleService.availableRoles(user.email, user.distributorID.get) must beEqualTo(List())

      db.withTransaction { implicit connection =>
        securityRoleService.addUserRole(user.id.get, adminUser.RoleID)
      }
      val roles = securityRoleService.availableRoles(user.email, user.distributorID.get)

      roles must beEqualTo(List(SecurityRole(adminUser.RoleID, adminUser.RoleName)))
    }

    "return an instance of the UserWithRole class" in new WithDB {
      val user = {
        val id = distributorUserService.create("newemail4@jungroup.com", password, companyName).get
        distributorUserService.find(id).get
      }
      val adminUser = Admin(user.email, user.id.get, securityRoleService)
      db.withTransaction { implicit connection =>
        securityRoleService.addUserRole(user.id.get, adminUser.RoleID).get must haveClass[UserWithRole]
      }
    }
  }

  "securityRoleService.deleteUserRole" should {
    "remove the record in the distributor_users_roles table" in new WithDB {
      val user = {
        val id = distributorUserService.create("newemail5@jungroup.com", password, companyName).get
        distributorUserService.find(id).get
      }
      val adminUser = Admin(user.email, user.id.get, securityRoleService)
      db.withTransaction { implicit connection =>
        val role = securityRoleService.addUserRole(user.id.get, adminUser.RoleID).get
        securityRoleService.deleteUserRole(role.distributorUserRoleID.get) must beEqualTo(1)
      }
      securityRoleService.availableRoles(user.email, user.distributorID.get) must beEqualTo(List())
    }
  }

  "securityRoleService.findAllUsersWithRoles" should {
    "include users who do not have roles assigned to them" in new WithDB {
      val user = {
        val id = distributorUserService.create("newemail7@jungroup.com", password, companyName).get
        distributorUserService.find(id).get
      }
      securityRoleService.findAllUsersWithRoles.count(userRole => userRole.distributorUserID == user.id.get) must beEqualTo(1)
    }

    "include the role_id for users with roles assigned" in new WithDB {
      val user = {
        val id = distributorUserService.create("newemail8@jungroup.com", password, companyName).get
        distributorUserService.find(id).get
      }
      val adminUser = Admin(user.email, user.id.get, securityRoleService)
      db.withTransaction { implicit connection =>
        securityRoleService.addUserRole(user.id.get, adminUser.RoleID).get
      }
      val userRole = securityRoleService.findAllUsersWithRoles.filter(userRole => userRole.distributorUserID == user.id.get)(0)
      userRole.roleID.get must beEqualTo(adminUser.RoleID)
    }

    "only include users with a Jun Group or HyprMX email address" in new WithDB {
      val nonEligibleEmails = {
        val emails = List("someuser@gmail.com", "someuser@jun.com", "fakehyprmxuser@hypr.com", "user@jungroup.co")
        emails.map(distributorUserService.create(_, password, companyName).get)
        emails
      }
      val eligibleEmails = {
        val emails = List("user@jungroup.com", "user@hyprmx.com", "user2@JunGroup.com", "user2@hyprMX.com")
        emails.map(distributorUserService.create(_, password, companyName).get)
        emails
      }
      val allRoles = securityRoleService.findAllUsersWithRoles.map(_.email)

      allRoles.count(nonEligibleEmails.contains(_)) must beEqualTo(0)
      allRoles.count(eligibleEmails.contains(_)) must beEqualTo(eligibleEmails.length)
    }
  }

  "securityRoleService.allRoles" should {
    "return a list of all records in the roles table" in new WithDB {
      val roles = securityRoleService.allRoles
      tableCount("roles") must beEqualTo(roles.length)
      roles.count(role => role.getName == "ADMIN") must beEqualTo(1)
    }
  }

  "securityRoleService.exists" should {
    "return true if the role exists for a user" in new WithDB {
      val user = {
        val id = distributorUserService.create("newemail9@jungroup.com", password, companyName).get
        distributorUserService.find(id).get
      }
      val adminUser = Admin(user.email, user.id.get, securityRoleService)
      db.withTransaction { implicit connection =>
        securityRoleService.addUserRole(user.id.get, adminUser.RoleID).get
      }
      securityRoleService.exists(adminUser) must beTrue
    }

    "return false if the role does not exist for a user" in new WithDB {
      val user = {
        val id = distributorUserService.create("newemail10@jungroup.com", password, companyName).get
        distributorUserService.find(id).get
      }
      val adminUser = Admin(user.email, user.id.get, securityRoleService)
      securityRoleService.exists(adminUser) must beFalse
    }
  }
}
