package org.packtpublishing.stub

import akka.actor.{ActorSystem, Props}
import akka.io.IO
import akka.pattern._
import akka.util.Timeout

import scala.concurrent.Await
import scala.concurrent.duration._

import spray.can.Http
import spray.can.server.ServerSettings
import spray.routing._
import Directives._

class GoogleBooksApiStub(val route: Route) {
  implicit val system = ActorSystem("google-books-api")
  implicit val timeout: Timeout = 3 seconds 

  val settings = ServerSettings(system).copy(sslEncryption = false)
  val handler = system.actorOf(Props(new GoogleBooksRestService(route)), name = "handler")

  def start(port: Int) = 
    Await.ready(IO(Http) ? Http.Bind(handler, 
      interface = "localhost", port = port, settings = Some(settings)), timeout.duration)
      
  def stop() = {
    IO(Http) ? Http.CloseAll
    system.stop(handler)
  }
}

sealed class GoogleBooksRestService(val route: Route) extends HttpServiceActor {
  def receive = runRoute {
    route
  }
}