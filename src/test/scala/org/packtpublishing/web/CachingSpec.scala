package org.packtpublishing.web

import spray.json._
import org.scalatest._
import spray.testkit.ScalatestRouteTest
import spray.http._
import spray.http.StatusCodes.{OK, NotFound, Created, NoContent, NotModified}
import spray.http.HttpHeaders.{Location, ETag, `If-None-Match`}
import spray.httpx.SprayJsonSupport._
import spray.httpx.unmarshalling._
import spray.routing.HttpService
import org.packtpublishing.service._
import org.packtpublishing.model.Publisher
import org.packtpublishing.model.Permissions._
import org.packtpublishing.web.BooksJsonProtocol._
import org.joda.time.LocalDate

class CachingSpec extends FlatSpec 
    with BeforeAndAfter
    with Inside
    with ScalatestRouteTest 
    with Matchers 
    with BookRestApiRoutes 
    with HttpService {
  
  implicit def executionContext = scala.concurrent.ExecutionContext.Implicits.global
  def actorRefFactory = system
  
  val persistence = new PersistenceService
  persistence.createSchema()
  
  val bookService = new BookService(persistence)
  val publisherService = new PublisherService(persistence)
  val userService = new UserService(persistence)
  
  before {
    persistence.truncate() onSuccess { case _ =>
      userService.createUser("admin", "passw0rd", Seq(MANAGE_BOOKS, MANAGE_PUBLISHERS))
    }
  }
  
  it should "create, delete and try to get the publisher" in {
    val credentials = addCredentials(BasicHttpCredentials("admin", "passw0rd"))
    Post("/api/v1/publishers", PublisherPayload("Packt")) ~> credentials ~> routes ~> check {
      status shouldBe Created
      val location = header[Location].map(_.value).get
      
      Get(location) ~> routes ~> check {
        status shouldBe OK
      }

      Delete(location) ~> credentials ~> routes ~> check {
        status shouldBe NoContent
      }
      
      Get(location) ~> sealRoute(routes) ~> check {
        status shouldBe NotFound
      }
    }
  }
  
  it should "create publisher and book and use ETag to get the book" in {
    val credentials = addCredentials(BasicHttpCredentials("admin", "passw0rd"))
    Post("/api/v1/publishers", PublisherPayload("Packt")) ~> credentials ~> routes ~> check {
      status shouldBe Created
      val location = header[Location].map(_.value).get
      
      val payload = BookCreatePayload("978-1119267225", "Professional Scala", 
        "Janek Bogucki", new LocalDate(2016, 6, 14))
          
      Post(location + "/books", payload) ~> credentials ~> routes ~> check {
        status shouldBe Created            
      }
        
      Get(s"/api/v1/books/${payload.isbn}") ~> routes ~> check {
        status shouldBe OK
        header[`ETag`].map(_.value) shouldBe Some("\"ff403e91a5a8e67057c96b3777a95f86\"")        
      }
      
      val headers = addHeader(`If-None-Match`(EntityTag("ff403e91a5a8e67057c96b3777a95f86")))
      Get(s"/api/v1/books/${payload.isbn}") ~> headers ~> routes ~> check {
        status shouldBe NotModified
      }
    }
  }
}