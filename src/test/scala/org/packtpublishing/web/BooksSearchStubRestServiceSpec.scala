package org.packtpublishing.web

import scala.language.postfixOps

import org.packtpublishing.integrations.GoogleBooksClient
import org.packtpublishing.stub.GoogleBooksApiStub

import org.packtpublishing.web.BooksJsonProtocol._
import org.scalatest.FlatSpec
import org.scalatest.Matchers
import org.scalatest.BeforeAndAfter

import spray.routing.HttpService
import spray.http.StatusCodes.OK
import spray.http._

import spray.httpx.unmarshalling._
import spray.httpx.SprayJsonSupport._

import spray.testkit.ScalatestRouteTest

class BooksSearchStubRestServiceSpec extends FlatSpec
    with ScalatestRouteTest 
    with BeforeAndAfter
    with Matchers 
    with BookSearchServiceRoutes 
    with HttpService {
  
  import spray.json.DefaultJsonProtocol._
  import spray.json.{JsNumber, JsString, JsValue, JsArray}
  
  implicit def executionContext = scala.concurrent.ExecutionContext.Implicits.global  
  def actorRefFactory = system // connect the DSL to the test ActorSystem {
  val port = 54332
  
  override val client = new GoogleBooksClient {
    override def uri(query: String) = Uri(s"http://localhost:$port/books/v1/volumes") 
  }  
  
  val googleBooksApiStub = new GoogleBooksApiStub(
    path("books" / "v1" / "volumes") {
      get {
        complete {
          Map[String, JsValue](
            "kind" -> JsString("books#volumes"), 
            "totalItems" -> JsNumber(0),
            "items" -> JsArray()
          )
        }
      }
    }
  )
  
  before {
    googleBooksApiStub start port
  }
 
  after {
    googleBooksApiStub stop
  }

  it should "return empty books volume list" in {
    Get(Uri("/search").withQuery(("query","scala"))) ~> searchRoutes ~> check {
      status shouldBe OK
      body.as[Seq[BookVolumeInfo]] shouldBe Right(Seq())
    }
  }
}