package org.packtpublishing.web

import org.scalatest._
import spray.testkit.ScalatestRouteTest
import org.packtpublishing.service._
import spray.http.StatusCodes

class BookRestServiceTest extends FlatSpec with ScalatestRouteTest with Matchers with BookRestApiRoutes {
  implicit def executionContext = scala.concurrent.ExecutionContext.Implicits.global
  
  val persistence = new PersistenceService
  persistence.createSchema() onSuccess { 
    case _ => persistence.createDataset()
  }
  
  val bookService = new BookService(persistence)
  val publisherService = new PublisherService(persistence)
  val userService = new UserService(persistence)

  it should "return all books" in {
    Get("/api/v1/books") ~> routes ~> check {
      response.status shouldBe StatusCodes.OK
    }
  }
}