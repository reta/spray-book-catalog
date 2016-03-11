package org.packtpublishing.web

import spray.routing.HttpService
import com.wordnik.swagger.annotations._
import org.packtpublishing.integrations.GoogleBooksClient
import spray.httpx.UnsuccessfulResponseException
import scala.util.Success
import scala.util.Failure


@Api(value = "/api/v1/search", description = "Book Catalog: external search")
trait BookSearchServiceRoutes {
  this: HttpService =>
      
  import scala.concurrent.ExecutionContext.Implicits.global
  
  import spray.httpx.SprayJsonSupport._
  import spray.httpx.marshalling._

  import org.packtpublishing.web.BooksJsonProtocol._
  val client = new GoogleBooksClient
    
  @ApiOperation(
    value = "Search book", httpMethod = "GET", 
    produces = "application/json; charset=UTF-8", 
    response = classOf[BookVolumeInfo], responseContainer="List"
  )
  @ApiImplicitParams(Array(
    new ApiImplicitParam(name = "query", required = true, dataType = "String", paramType = "query")
  ))
  def searchBooks = pathPrefix("search") {
    pathEndOrSingleSlash {
      get { 
        parameters('query) { query =>
          onComplete(client.findBooks(query)) {
            case Success(results) => complete(results)
            case Failure(ex) => ex match {
              case ure: UnsuccessfulResponseException => complete(BookSearchResult(0, List()))
              case _ => complete(ex)
            }
          }
        }
      }
    }
  }
  
  val searchRoutes = searchBooks
}