package org.packtpublishing.persistence

import org.joda.time.LocalDate
import slick.driver.H2Driver.api._
import com.github.tototoshi.slick.H2JodaSupport._
import org.packtpublishing.model.Publisher

class Publishers(tag: Tag) extends Table[Publisher](tag, "PUBLISHERS") {
  def id = column[Long]("ID", O.PrimaryKey, O.AutoInc)
  def name = column[String]("NAME", O.Length(256))
  def uniqueName = index("NAME_IDX", name, true)

  def * = (id.?, name) <> (Publisher.tupled, Publisher.unapply)
}

object Publishers {
  val publishers = TableQuery[Publishers]
}  