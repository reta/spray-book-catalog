package org.packtpublishing.persistence

import slick.driver.H2Driver.api._
import org.packtpublishing.persistence.Users._
import org.packtpublishing.model.Permissions
import org.packtpublishing.model.Permission

class UserPermissions(tag: Tag) extends Table[Permission](tag, "USER_PERMISSIONS") {
  import UserPermissions._
  
  def username = column[String]("USERNAME", O.Length(128))
  def permission = column[Permissions.Permission]("PERMISSION", O.Length(64))
  
  def user = foreignKey("USER_FK", username, users)(_.username, onUpdate=ForeignKeyAction.Restrict, onDelete=ForeignKeyAction.Cascade)
  def pk = primaryKey("USER_PERMISSION_PK", (username, permission))
  
  def * = (username, permission) <> (Permission.tupled, Permission.unapply)
}

object UserPermissions {
  val userPermissions = TableQuery[UserPermissions]
  
  implicit val permissionColumnType = MappedColumnType.base[Permissions.Permission, String](
    { permission => permission.toString }, // map Permission to String
    { str => Permissions.withName(str) } // map String to Permission
  )
}