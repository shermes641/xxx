package admin

import models._
import play.api.libs.json.{JsValue, JsArray, Json}
import play.api.test.Helpers._
import play.api.test.{FakeHeaders, FakeRequest}
import resources.{SpecificationWithFixtures, DistributorUserSetup}

class AdminControllerSpec  extends SpecificationWithFixtures with DistributorUserSetup with AppCreationHelper {
  val users: List[DistributorUser] = running(testApplication) {
    val usersAndDistributors = (1 to 3).map(index => newDistributorUser(email = "newUser" + index + "@jungroup.com", password = password, companyName = "company" + index))
    usersAndDistributors.map((userInfo) => setUpApp(distributorID = userInfo._2.id.get, appName = Some(userInfo._2.name + " App")))
    usersAndDistributors.map(_._1).toList
  }

  val (admin, adminRole) = running(testApplication) {
    val user = newDistributorUser(email = "newAdminUser@jungroup.com")._1
    val role = Admin(user.email, user.id.get, securityRoleService)
    database.withTransaction { implicit connection =>
      securityRoleService.addUserRole(user.id.get, role.RoleID)
    }
    setUpApp(distributorID = user.distributorID.get, appName = Some("New App"))
    (user, role)
  }

  "AdminController.morph" should {
    "not allow a non-admin user to morph into another user" in new WithFakeBrowser {
      val user = users(0)
      logInUser(user.email)
      browser.url() must beEqualTo(controllers.routes.AnalyticsController.show(user.distributorID.get, None, None).url)
      goToAndWaitForAngular(controllers.routes.AdminController.morph(Some(users(1).distributorID.get)).url)
      // Return a 404
      browser.url() must beEqualTo(controllers.routes.Application.notFound().url)
    }

    "log in the admin as the user they are morphing into" in new WithFakeBrowser {
      val user = users.head
      logInUser(admin.email)
      browser.url() must beEqualTo(controllers.routes.AnalyticsController.show(admin.distributorID.get, None, None).url)
      goToAndWaitForAngular(controllers.routes.AdminController.morph(Some(user.distributorID.get)).url)
      // Redirect to the target user's analytics page
      browser.url() must beEqualTo(controllers.routes.AnalyticsController.show(user.distributorID.get, None, None).url)
    }
  }

  "AdminController.distributorInfo" should {
    "return a list of all Distributors and Apps" in new WithFakeBrowser {
      logInUser(admin.email)
      val request = FakeRequest(
        GET,
        controllers.routes.AdminController.distributorInfo().url,
        FakeHeaders(Seq("Content-type" -> "application/json")),
        Json.obj()
      )
      val Some(result) = route(request.withSession("distributorID" -> admin.distributorID.get.toString, "username" -> admin.email))
      status(result) must equalTo(200)
      val jsonResponse = Json.parse(contentAsString(result))
      (jsonResponse \ "distributors").as[JsArray].as[List[JsValue]].length must beEqualTo(distributorService.findAll().length)
      (jsonResponse \ "apps").as[JsArray].as[List[JsValue]].length must beEqualTo(appService.findAll().length)
    }
  }

  "AdminController.roleInfo" should {
    "return a list of all DistributorUsers and Roles" in new WithFakeBrowser {
      logInUser(admin.email)
      val request = FakeRequest(
        GET,
        controllers.routes.AdminController.roleInfo().url,
        FakeHeaders(Seq("Content-type" -> "application/json")),
        Json.obj()
      )
      val Some(result) = route(request.withSession("distributorID" -> admin.distributorID.get.toString, "username" -> admin.email))
      status(result) must equalTo(200)
      val jsonResponse = Json.parse(contentAsString(result))
      (jsonResponse \ "distributor_users").as[JsArray].as[List[JsValue]].length must beEqualTo(securityRoleService.findAllUsersWithRoles.length)
      (jsonResponse \ "roles").as[JsArray].as[List[JsValue]].length must beEqualTo(securityRoleService.allRoles.length)
    }
  }

  "AdminController.addRole" should {
    "add a role for a specific user" in new WithFakeBrowser {
      logInUser(admin.email)
      val nonAdminUser = users(2)
      val body = Json.obj(
        "distributor_user_id" -> nonAdminUser.id.get,
        "role_id" -> adminRole.RoleID
      )
      val request = FakeRequest(
        POST,
        controllers.routes.AdminController.addRole().url,
        FakeHeaders(Seq("Content-type" -> "application/json")),
        body
      )
      val Some(result) = route(request.withSession("distributorID" -> admin.distributorID.get.toString, "username" -> admin.email))
      status(result) must equalTo(201)
      securityRoleService.exists(Admin(nonAdminUser.email, nonAdminUser.id.get, securityRoleService)) must beTrue
    }
  }

  "AdminController.edit" should {
    "render the main admin page if the user is an admin" in new WithFakeBrowser {
      logInUser(admin.email)
      val request = FakeRequest(
        GET,
        controllers.routes.AdminController.edit().url,
        FakeHeaders(Seq("Content-type" -> "application/json")),
        Json.obj()
      )
      val Some(result) = route(request.withSession("distributorID" -> admin.distributorID.get.toString, "username" -> admin.email))
      status(result) must equalTo(200)
    }

    "return a 404 if the user is not an admin" in new WithFakeBrowser {
      val nonAdminUser = users.head
      logInUser(nonAdminUser.email)
      val request = FakeRequest(
        POST,
        controllers.routes.AdminController.edit().url,
        FakeHeaders(Seq("Content-type" -> "application/json")),
        Json.obj()
      )
      val Some(result) = route(request.withSession("distributorID" -> nonAdminUser.distributorID.get.toString, "username" -> nonAdminUser.email))
      status(result) must equalTo(404)
    }
  }
}
