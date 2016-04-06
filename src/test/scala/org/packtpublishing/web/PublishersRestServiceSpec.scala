package org.packtpublishing.web

import scala.concurrent.Await
import scala.concurrent.duration._
import scala.language.postfixOps

import spray.json._
import org.scalatest._
import spray.testkit.ScalatestRouteTest
import spray.http._
import spray.http.StatusCodes.{OK, Created, Unauthorized, Forbidden, NotFound, Conflict, NoContent}
import spray.http.HttpHeaders.Location
import spray.http.HttpHeaders.`WWW-Authenticate`
import spray.httpx.SprayJsonSupport._
import spray.httpx.unmarshalling._
import spray.routing.HttpService
import org.packtpublishing.service._
import org.packtpublishing.model.Publisher
import org.packtpublishing.model.Permissions._
import org.packtpublishing.web.BooksJsonProtocol._
import scala.util.Success

class PublishersRestServiceSpec extends FlatSpec 
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
    Await.result(
      persistence.truncate() andThen { case Success(_) =>
        userService.createUser("unauthorized", "passw0rd", Seq())
        userService.createUser("admin", "passw0rd", Seq(MANAGE_BOOKS, MANAGE_PUBLISHERS))
      }, 1 second
    )
  }
    
  it should "return empty publishers list" in {
    Get("/api/v1/publishers") ~> routes ~> check {
      status shouldBe OK
      body.as[Seq[Publisher]] shouldBe Right(Seq())  
    }
  }
  
  it should "not return publisher" in {
    Get("/api/v1/publishers/1") ~> sealRoute(routes) ~> check {
      status shouldBe NotFound
    }
  }

  it should "not create new publisher without authentication" in {
    Post("/api/v1/publishers", PublisherPayload("Packt Publishing")) ~> sealRoute(routes) ~> check {
      status shouldBe Unauthorized
      header[`WWW-Authenticate`].map(_.challenges) shouldBe Some(Seq(HttpChallenge("Basic", "Book Catalog")))
    }
  }
  
  it should "not create new publisher without permissions" in {
    val credentials = addCredentials(BasicHttpCredentials("unauthorized", "passw0rd")) 
    Post("/api/v1/publishers", PublisherPayload("Packt Publishing")) ~> credentials ~> sealRoute(routes) ~> check {
      status shouldBe Forbidden
    }
  }
  
  it should "create new publisher" in {
    val credentials = addCredentials(BasicHttpCredentials("admin", "passw0rd")) 
    Post("/api/v1/publishers", PublisherPayload("Packt Publishing")) ~> credentials ~> routes ~> check {
      status shouldBe Created      
      
      inside(body.as[Publisher]) { 
        case Right(Publisher(Some(id), name)) =>
          name should be ("Packt Publishing")
          id should be > (0L)
          
          inside(header[Location].map(_.value)) {
            case Some(url) => url should endWith (s"/api/v1/publishers/$id") 
          }
      }
    }
  }
  
  it should "create not created duplicate publisher" in {
    val credentials = addCredentials(BasicHttpCredentials("admin", "passw0rd"))
    val payload = PublisherPayload("Packt Publishing")    
    Post("/api/v1/publishers", payload) ~> credentials ~> routes ~> check {
      status shouldBe Created   
    }    
    Post("/api/v1/publishers", payload) ~> credentials ~> routes ~> check {
      status shouldBe Conflict
    }
  }
  
  it should "update publisher" in {
    val credentials = addCredentials(BasicHttpCredentials("admin", "passw0rd"))
    Post("/api/v1/publishers", PublisherPayload("Packt")) ~> credentials ~> routes ~> check {
      status shouldBe Created
      
      val location = header[Location].map(_.value).get
      val payload = PublisherPayload("Packt Publishing")
      Put(location, payload) ~> credentials ~> routes ~> check {
        status shouldBe OK
        
        inside(body.as[Publisher]) { 
          case Right(Publisher(Some(id), name)) =>
            name should be ("Packt Publishing")
        }
      }
    }
  }
  
  it should "delete publisher" in {
    val credentials = addCredentials(BasicHttpCredentials("admin", "passw0rd"))
    Post("/api/v1/publishers", PublisherPayload("Packt")) ~> credentials ~> routes ~> check {
      status shouldBe Created
      
      val location = header[Location].map(_.value).get
      Delete(location) ~> credentials ~> routes ~> check {
        status shouldBe NoContent
      }
    }
  }
}