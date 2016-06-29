package admin

import anorm._
import anorm.SqlParser._
import java.sql.Connection
import javax.inject.Inject

import play.api.db.{DB, Database}
import play.api.Play.current

import scala.language.postfixOps

/**
 * Class that encapsulates the id and name fields from the roles table
 * @param roleID   The id field in the roles table
 * @param roleName The name field in the roles table
 */
case class SecurityRole(roleID: Int, roleName: String) extends be.objectify.deadbolt.core.models.Role {
  def getRoleID: Int = roleID
  def getName: String = roleName
}

/**
 * Encapsulates a DistributorUser with role information
 * @param distributorUserID     The ID of the DistributorUser
 * @param email                 The email of the DistributorUser
 * @param name                  The name of the role
 * @param roleID                The ID of the role
 * @param distributorUserRoleID The ID from the distributor_users_roles table
 */
case class UserWithRole(distributorUserID: Long,
                        email: String,
                        name: Option[String],
                        roleID: Option[Int],
                        distributorUserRoleID: Option[Int])

class SecurityRoleService @Inject() (db: Database) {
  val userRoleParser: RowParser[UserWithRole] = {
    get[Long]("distributor_user_id") ~
    get[String]("email") ~
    get[Option[String]]("name") ~
    get[Option[Int]]("role_id") ~
    get[Option[Int]]("id") map {
      case distributor_user_id ~ email ~ name ~ role_id ~ distributor_user_role_id => UserWithRole(distributor_user_id, email, name, role_id, distributor_user_role_id)
    }
  }

  val roleParser: RowParser[SecurityRole] = {
    get[Int]("roles.id") ~
      get[String]("roles.name") map {
      case id ~ name => SecurityRole(id, name)
    }
  }

  /**
   * Finds all available roles for a DistributorUser
   * @param email         The email of the DistributorUser
   * @param distributorID The ID of the Distributor to which the DistributorUser belongs
   * @return              A list of Roles that have been assigned to the DistributorUser
   */
  def availableRoles(email: String, distributorID: Long): List[SecurityRole] = {
    db.withConnection { implicit connection =>
      SQL(
        """
          SELECT roles.*
          FROM roles
          JOIN distributor_users_roles ON distributor_users_roles.role_id = roles.id
          JOIN distributor_users ON distributor_users.id = distributor_users_roles.distributor_user_id
          WHERE distributor_users.email = {email}
          AND distributor_users.distributor_id = {distributor_id}
        """
      )
        .on("email" -> email, "distributor_id" -> distributorID)
        .as(roleParser*)
    }
  }

  /**
   * Checks if a DistributorUser has a specific role (e.g. Admin)
   * @param role The role to check for in the database
   * @return     An instance of the role if one is found; otherwise, None
   */
  def exists(role: DeadboltRole): Boolean = {
    availableRoles(role.userEmail, role.distributorID)
      .count(existingRole => existingRole.getRoleID == role.RoleID) > 0
  }

  /**
   * Returns all roles in the database
   * @return A list of existing SecurityRoles
   */
  def allRoles: List[SecurityRole] = {
    db.withConnection { implicit connection =>
      SQL(
        """
          SELECT roles.*
          FROM roles;
        """
      ).as(roleParser*)
    }
  }

  /**
   * Assigns a specific role to a user
   * @param distributorUserID The ID of the DistributorUser to which the role will be assigned
   * @param roleID            The ID of the role (maps to the id column in the roles table)
   * @param connection        A shared database connection
   * @return                  An instance of the UserWithRole class if the role was assigned properly; otherwise, None.
   */
  def addUserRole(distributorUserID: Long, roleID: Int)(implicit connection: Connection): Option[UserWithRole] = {
    val result = SQL(
      """
         INSERT INTO distributor_users_roles (distributor_user_id, role_id)
         VALUES ({distributor_user_id}, {role_id});
      """
    ).on("distributor_user_id" -> distributorUserID, "role_id" -> roleID).executeInsert()
    result match {
      case Some(id: Long) => {
        val query = SQL(
          """
             SELECT distributor_users.id as distributor_user_id, distributor_users.email, roles.name, roles.id as role_id, distributor_users_roles.id
             FROM distributor_users_roles
             JOIN distributor_users on distributor_users.id = distributor_users_roles.distributor_user_id
             JOIN roles on roles.id = distributor_users_roles.role_id
             WHERE distributor_users_roles.id = {id};
          """
        ).on("id" -> id)
        query.as(userRoleParser*) match {
          case List(user) => Some(user)
          case List() => None
        }
      }
      case None => None
    }
  }

  /**
   * Removes the role assignment for a DistributorUser
   * @param userRoleID The id field from the distributor_users_roles table
   * @param connection A shared database connection
   * @return           The number of role assignments deleted
   */
  def deleteUserRole(userRoleID: Int)(implicit connection: Connection): Long = {
    SQL(
      """
         DELETE FROM distributor_users_roles
         WHERE id = {id};
      """
    ).on("id" -> userRoleID).executeUpdate()
  }

  /**
   * Finds all users with or without roles. If a user has multiple roles, these will be included in the list.
   * If a user has no roles, the user will be included with a blank role_id field.
   * @return A list of DistributorUsers with roles
   */
  def findAllUsersWithRoles: List[UserWithRole] = {
    db.withConnection { implicit connection =>
      SQL(
        """
          SELECT distributor_users.id as distributor_user_id, distributor_users.email, roles.name, roles.id as role_id, distributor_users_roles.id
          FROM distributor_users
          LEFT OUTER JOIN distributor_users_roles on distributor_users_roles.distributor_user_id = distributor_users.id
          LEFT OUTER JOIN roles on roles.id = distributor_users_roles.role_id
          WHERE LOWER(distributor_users.email) LIKE LOWER('%@hyprmx.com%') OR LOWER(distributor_users.email) LIKE LOWER('%@jungroup.com%');
        """
      ).as(userRoleParser*)
    }
  }
}
