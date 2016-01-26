package org.packtpublishing.service

import scala.concurrent.Future
import org.packtpublishing.model.Publisher
import scala.util.Try
import scala.util.Success
import scala.util.Failure
import scala.util.control.NonFatal

class PublisherService(val persistence: PersistenceService) {
  import scala.concurrent.ExecutionContext.Implicits.global
  
  def findAll(): Future[Seq[Publisher]] = persistence.findAllPublishers
  def add(name: String): Future[Publisher] = persistence.persistPublisher(name)
  def findById(id: Long): Future[Option[Publisher]] = persistence.findPublisherById(id)
  def deleteById(id: Long): Future[Boolean] = persistence.deletePublisherById(id)
  
  def updateById(id: Long, name: String): Future[Option[Publisher]] = 
    persistence.updatePublisherById(id, name) map {
      case true => Some(Publisher(Some(id), name))
      case _ => None
    }
}