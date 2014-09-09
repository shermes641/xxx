/**
 * Creates seed data to bootstrap the database.  users and appNames array determine the number of users/apps created.
 */

import models.DistributorUser
import models.App
import models.AdProvider

// The "correct" way to start the app
new play.core.StaticApplication(new java.io.File("."))

// List of users to created.  "@jungroup" will be appended for email and the distributor will be have "Distributor" appended
var users = List("dcullen", "jellison", "tdepplito", "jlunn", "dwood")

// App Names to use for each user
var appNames = List("Game App", "News App", "Casino App")

// Create ad providers
AdProvider.create("Ad Colony", "{\"required_params\":[\"appId\"]}")
AdProvider.create("Vungle", "{\"required_params\":[\"appId\",\"zoneIds\"]}")
AdProvider.create("HyprMX", "{\"required_params\":[\"distributorId\",\"propertyId\"]}")

/**
 * Creates an app using name and distributorID
 * @param name Name of App to be created
 * @param distributorID Distributor Id to use
 */
def createApp(name: String, distributorID: Long) = {
  App.create(distributorID, name)
}

/**
 * Creates a user using a name string
 * @param name Name to be used as a base for the email and distributor
 */
def createUser(name: String) = {
  DistributorUser.create(name + "@jungroup.com", "testtest", name + " Distributor") match {
    case Some(userID: Long) => {
      val user = DistributorUser.find(userID).get
      appNames.foreach((name: String) => createApp(name, user.distributorID.get))
    }
    case _ => false
  }
}

// Kick off the data seeding
users.foreach((name: String) => createUser(name))
