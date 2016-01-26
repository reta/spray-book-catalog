package org.packtpublishing.persistence

import org.joda.time.LocalDate
import slick.driver.H2Driver.api._
import com.github.tototoshi.slick.H2JodaSupport._
import org.packtpublishing.persistence.Publishers._
import org.packtpublishing.model.Book

class Books(tag: Tag) extends Table[Book](tag, "BOOKS") {
  def isbn = column[String]("ISBN", O.PrimaryKey, O.Length(14))
  def title = column[String]("TITLE", O.Length(512))
  def author = column[String]("AUTHOR", O.Length(256))
  def publishingDate = column[LocalDate]("PUBLISHING_DATE")
  
  def publisherId = column[Long]("PUBLISHER_ID")
  def publisher = foreignKey("PUBLISHER_FK", publisherId, publishers)(_.id, onUpdate=ForeignKeyAction.Restrict, onDelete=ForeignKeyAction.Cascade)
  
  def * = (isbn, title, author, publishingDate, publisherId) <> (Book.tupled, Book.unapply)
}

object Books {
  val books = TableQuery[Books]
}