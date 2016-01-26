package org.packtpublishing

import akka.actor.{ActorSystem, Props}
import akka.io.IO
import akka.event.Logging
import spray.can.Http
import org.packtpublishing.service.PersistenceService
import org.packtpublishing.web.BookRestService
import org.packtpublishing.security.SslSupport

object BootApp extends App with SslSupport {
  import scala.concurrent.ExecutionContext.Implicits.global  
  
  implicit val system = ActorSystem("spray-intro")
  val log = Logging(system, getClass)
     
  val persistence = new PersistenceService
  persistence.createSchema() onSuccess { 
    case _ => persistence.createDataset()
  }
  
  val listener = system.actorOf(Props(new BookRestService(persistence)), name = "book-rest-service")
  IO(Http) ! Http.Bind(listener, interface = "localhost", port = 9001)
} 