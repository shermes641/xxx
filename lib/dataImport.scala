/**
 * Creates seed data to bootstrap the database.
 */

import models.AdProvider
import play.api.Logger

// The "correct" way to start the app
new play.core.StaticApplication(new java.io.File("."))

AdProvider.loadAll()
