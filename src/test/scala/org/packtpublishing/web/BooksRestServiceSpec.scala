package org.packtpublishing.web

import scala.concurrent.Await
import scala.concurrent.duration._

import spray.routing.HttpService
import spray.testkit.Specs2RouteTest
import org.specs2.mutable.{Specification, Before}
import spray.http._
import spray.http.StatusCodes.{OK, NoContent, Created, Unauthorized, Forbidden, NotFound, Conflict}
import spray.http.HttpHeaders.Location
import spray.httpx.unmarshalling._
import spray.httpx.SprayJsonSupport._
import org.packtpublishing.service._
import org.packtpublishing.model.Publisher
import org.packtpublishing.model.Permissions._
import org.packtpublishing.web.BooksJsonProtocol._
import org.joda.time.LocalDate
import org.junit.runner.RunWith
import org.specs2.runner.JUnitRunner
import scala.util.Success

@RunWith(classOf[JUnitRunner])
class BooksRestServiceSpec extends Specification 
    with Specs2RouteTest
    with Before
    with BookRestApiRoutes 
    with HttpService {
  
  implicit def executionContext = scala.concurrent.ExecutionContext.Implicits.global
  // connect the DSL to the test ActorSystem
  def actorRefFactory = system 
  
  lazy val persistence = new PersistenceService    
  lazy val bookService = new BookService(persistence)
  lazy val publisherService = new PublisherService(persistence)
  lazy val userService = new UserService(persistence)
  
  override def before = {
    Await.result(    
      persistence.createSchema() andThen { case Success(_) =>
        userService.createUser("unauthorized", "passw0rd", Seq())
        userService.createUser("admin", "passw0rd", Seq(MANAGE_BOOKS, MANAGE_PUBLISHERS))
      }, Duration(1, SECONDS)
    )
  }
  
  "The Book REST(ful) service" should {
    "return empty books list" in {
      Get("/api/v1/books") ~> routes ~> check {
        status === OK
        responseAs[Seq[BookResource]] === Seq()  
      }
    }
    
    "not return the book" in {
      Get("/api/v1/books/111-1111111111") ~> sealRoute(routes) ~> check {
        status === NotFound
      }
    }
    
    "create new book" in {
      val credentials = addCredentials(BasicHttpCredentials("admin", "passw0rd")) 
      Post("/api/v1/publishers", PublisherPayload("Packt Publishing")) ~> credentials ~> routes ~> check {
        status === Created
        
        val publisher = responseAs[Publisher]
        val location = header[Location].map(_.value)
        location should beSome     
        
        val payload = BookCreatePayload("978-1783281411", "Learning Concurrent Programming in Scala", 
          "Aleksandar Prokopec", new LocalDate(2014, 11, 25))
        
        Post(location.get + "/books", payload) ~> credentials ~> routes ~> check {
          status === Created  
          header[Location].map(_.value) must beSome.which(_ endsWith "/api/v1/books/978-1783281411")
          responseAs[BookResource] === BookResource("978-1783281411", 
            "Learning Concurrent Programming in Scala", "Aleksandar Prokopec", 
              new LocalDate(2014, 11, 25), publisher)
        }
      }
    }
    
    "create new book and list all publisher books" in {
      val credentials = addCredentials(BasicHttpCredentials("admin", "passw0rd")) 
      Post("/api/v1/publishers", PublisherPayload("Artima")) ~> credentials ~> routes ~> check {
        status === Created
        
        val publisher = responseAs[Publisher]
        val location = header[Location].map(_.value)
        
        val payload = BookCreatePayload("978-0981531649", "Programming in Scala", 
          "Martin Odersky", new LocalDate(2011, 1, 4))
        
        Post(location.get + "/books", payload) ~> credentials ~> routes ~> check {
          status === Created  
        }

        Get(location.get + "/books", payload) ~> credentials ~> routes ~> check {
          status === OK
          
          responseAs[Seq[BookResource]] === Seq(BookResource("978-0981531649", 
            "Programming in Scala", "Martin Odersky", new LocalDate(2011, 1, 4), publisher))
        }
      }
    }
    
    "delete book" in {
      val credentials = addCredentials(BasicHttpCredentials("admin", "passw0rd")) 
      Post("/api/v1/publishers", PublisherPayload("Manning Publications")) ~> credentials ~> routes ~> check {
        status === Created
        
        val location = header[Location].map(_.value)
        
        val payload = BookCreatePayload("978-1935182757", "Scala in Action", 
          "Nilanjan Raychaudhuri", new LocalDate(2013, 4, 13))
        
        Post(location.get + "/books", payload) ~> credentials ~> routes ~> check {
          status === Created            
        }
        
        Delete("/api/v1/books/978-1783281411") ~> credentials ~> routes ~> check {
          status === NoContent            
        }
      }
    }
    
    "update book" in {
      val credentials = addCredentials(BasicHttpCredentials("admin", "passw0rd")) 
      Post("/api/v1/publishers", PublisherPayload("Wrox")) ~> credentials ~> routes ~> check {
        status === Created
        
        val publisher = responseAs[Publisher]
        val location = header[Location].map(_.value)
        
        val createPayload = BookCreatePayload("978-1119267225", "Professional Scala", 
          "Janek Bogucki", new LocalDate(2016, 6, 14))
        
        Post(location.get + "/books", createPayload) ~> credentials ~> routes ~> check {
          status === Created            
        }

        val updatePayload = BookUpdatePayload("Professional Scala", 
          "Janek Bogucki", new LocalDate(2016, 6, 13))

        Put("/api/v1/books/978-1119267225", updatePayload) ~> credentials ~> routes ~> check {
          status === OK  
          
          responseAs[BookResource] === BookResource("978-1119267225", 
            "Professional Scala", "Janek Bogucki", new LocalDate(2016, 6, 13), publisher)
        }
      }
    }
  }
}