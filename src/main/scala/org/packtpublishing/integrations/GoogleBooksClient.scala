package org.packtpublishing.integrations

import akka.actor.ActorRefFactory
import scala.language.postfixOps

import org.packtpublishing.web.BookSearchResult
import spray.http._
import spray.http.StatusCodes.{OK, BadRequest}
import spray.client.pipelining._
import scala.concurrent.Future
import scala.concurrent.duration.Duration._
import scala.concurrent.duration._
import akka.util.Timeout

class GoogleBooksClient(implicit actorRefFactory: ActorRefFactory) {
  import scala.concurrent.ExecutionContext.Implicits.global
  
  import spray.httpx.SprayJsonSupport._
  import org.packtpublishing.web.BooksJsonProtocol._

  implicit val timeout: Timeout = 5 seconds
 
  val pipeline: HttpRequest => Future[BookSearchResult] =
    sendReceive ~>
    unmarshal[BookSearchResult]
    
  def uri(query: String) = 
    Uri("https://www.googleapis.com/books/v1/volumes")
      .withQuery(("q", query))
    
  def findBooks(query: String) = {
    pipeline(Get(uri(query))) map(_.items.map(_.volumeInfo))
  }
}