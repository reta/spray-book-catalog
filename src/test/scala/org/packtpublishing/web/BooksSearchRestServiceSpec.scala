package org.packtpublishing.web

import scala.concurrent.Future

import spray.http._
import spray.http.Uri.Query
import spray.http.StatusCodes.OK
import spray.httpx.unmarshalling._
import spray.httpx.SprayJsonSupport._
import spray.routing.HttpService
import spray.testkit.ScalatestRouteTest

import org.packtpublishing.service._
import org.packtpublishing.web.BooksJsonProtocol._
import org.packtpublishing.integrations.GoogleBooksClient

import org.scalatest.FlatSpec
import org.scalatest.Matchers
import org.scalatest.Inside

class BooksSearchRestServiceSpec extends FlatSpec
    with ScalatestRouteTest 
    with Matchers 
    with Inside
    with BookSearchServiceRoutes 
    with HttpService {
  
  implicit def executionContext = scala.concurrent.ExecutionContext.Implicits.global
  def actorRefFactory = system // connect the DSL to the test ActorSystem

  override val client = new GoogleBooksClient {
    override val pipeline = (request: HttpRequest) => request.uri.query get "q" match {
      case Some("isbn:978-1783283637") => Future.successful(
        BookSearchResult(
          1, 
          List(
            BookSearchResultItem(
              BookVolumeInfo(
                "Scala for Java Developers", 
                Some("Packt Publishing"), 
                Some("2014"), 
                Some(List("Thomas Alexandre")), 
                None, 
                None, 
                Some("en")
              )
            )
          )
        )
      )
      
      case _ => Future.successful(BookSearchResult(0, List()))
    }
  }

  it should "return empty books volume list" in {
    Get(Uri("/search").withQuery(("query","scala"))) ~> searchRoutes ~> check {
      status shouldBe OK
      body.as[Seq[BookVolumeInfo]] shouldBe Right(Seq())
    }
  }
  
  it should "return single book volume" in {
    Get(Uri("/search").withQuery(("query","isbn:978-1783283637"))) ~> searchRoutes ~> check {
      status shouldBe OK
      
      inside(body.as[Seq[BookVolumeInfo]]) { 
        case Right(Seq(BookVolumeInfo(title, Some(publisher), 
            publishingDate, Some(authors), None, None, Some(language)))) =>
              title shouldBe "Scala for Java Developers"
      }
    }
  }
}