import models.DistributorUser
import models.App
import models.AdProvider

new play.core.StaticApplication(new java.io.File("."))

var users = List("dcullen@jungroup.com", "jellison@jungroup.com", "tdepplito@jungroup.com")
users.foreach(createUser)

var distributorUser = DistributorUser.findByEmail("dcullen@jungroup.com")
                                                                                                 n
AdProvider.create("Ad Colony", "")
AdProvider.create("Vungle", "")
AdProvider.create("HyprMX", "")

def createUser(email: String) = {
  DistributorUser.create(email, "testtest", "David Cullen Distributor") match {
    case Some(userID:Long) => {
      App.create(userID, "Dcullen App Name 1")
      App.create(userID, "Dcullen App Name 2")
      App.create(userID, "Dcullen App Name 3")
    }
    case _ => false
  }
}


//
//"insert into ad_providers (name, configuration_data) values ('Ad Colony’, '{\"required_params\":[\"key1\"]}');"
//
//"insert into ad_providers (name, configuration_data) values (‘Vungle’, '{\"required_params\":[\"key1\", \"key2\"]}');"
//
//"insert into ad_providers (name, configuration_data) values (’HyprMX’, '{\"required_params\":[\"key2\", \"key3\", \"key4\"]}');"
