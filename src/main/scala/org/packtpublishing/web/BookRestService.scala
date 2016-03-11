package org.packtpublishing.web

import spray.routing.HttpServiceActor
import akka.actor.ActorLogging
import spray.routing._
import spray.routing.directives.CachingDirectives._
import Directives._
import org.packtpublishing.service.{BookService, PublisherService, UserService, ETagHash}
import org.packtpublishing.service.PersistenceService
import org.packtpublishing.model.Book
import org.packtpublishing.model.Publisher
import org.joda.time.LocalDate
import spray.http.HttpResponse
import org.h2.jdbc.JdbcSQLException
import scala.util.Success
import scala.util.Failure
import spray.http.EntityTag
import spray.http.StatusCodes._
import spray.http.HttpHeaders.Location
import spray.http.HttpHeaders.ETag
import spray.http.Uri
import spray.http.DateTime
import scala.concurrent.Future
import spray.routing.authentication.BasicAuth
import com.wordnik.swagger.annotations._
import javax.ws.rs.Path
import spray.caching._

class BookRestService(val persistence: PersistenceService) 
    extends HttpServiceActor 
    with ActorLogging 
    with BookRestApiRoutes
    with ApiDocs
    with ApiDocsUi {
  
  val bookService = new BookService(persistence)
  val publisherService = new PublisherService(persistence)
  val userService = new UserService(persistence)
  
  def receive = runRoute {
     routes ~ apiDocsRoutes ~ apiDocsUiRoutes
  }
}

trait BookRestApiRoutes extends BookRestServiceRoutes with PublisherRestServiceRoutes with BookSearchServiceRoutes {
  this: HttpService =>

  val routes = pathPrefix("api" / "v1") { 
    bookRoutes ~ publisherRoutes ~ searchRoutes
  }
}

@Api(value = "/api/v1/publishers", description = "Book Catalog: publishers management")
trait PublisherRestServiceRoutes extends Caching {
  this: HttpService =>
  
  val bookService: BookService
  val publisherService: PublisherService
  val userService: UserService

  import spray.httpx.SprayJsonSupport._
  import spray.httpx.marshalling._
  
  import scala.concurrent.ExecutionContext.Implicits.global
  import spray.routing.directives.CachingDirectives._
  import org.packtpublishing.web.BooksJsonProtocol._
  
  def root(path: Uri.Path): Uri.Path = Uri.Path(path.toString.split("/").take(3).mkString("/")) 
  
  @ApiOperation(
    value = "Get All Publishers", httpMethod = "GET", 
    produces = "application/json; charset=UTF-8", 
    response = classOf[Publisher], responseContainer="List"
  )
  def getAllPublishers = get {
    complete {
      publisherService.findAll
    }
  }
  
  @ApiOperation(
    value = "Add New Publisher", httpMethod = "POST", 
    produces = "application/json; charset=UTF-8", consumes = "application/json; charset=UTF-8", 
    response = classOf[PublisherResource]
  )
  @ApiImplicitParams(Array(
    new ApiImplicitParam(required = true, dataType = "org.packtpublishing.web.PublisherPayload", paramType = "body")
  ))
  @ApiResponses(Array(
    new ApiResponse(code = 201, message = "Publisher has been created", response = classOf[PublisherResource]),
    new ApiResponse(code = 401, message = "User is not authenticated"),
    new ApiResponse(code = 403, message = "User is not authorized"),
    new ApiResponse(code = 409, message = "Publisher with such name already exists")
  ))  
  def addPublisher = post {
    authenticate(BasicAuth(userService.authenticate _, realm = "Book Catalog")) { user =>
      authorize(userService canManagePublishers user) {        
        requestUri { uri =>
          entity(as[PublisherPayload]) { payload =>
            onComplete(publisherService.add(payload.name)) {
              case Success(publisher) => 
                respondWithHeader(Location(uri.withPath(uri.path / publisher.id.mkString))) { 
                  complete(Created, publisher)
                }
              case Failure(ex) => complete(Conflict)
            }
          }
        }
      }
    }
  }
  
  import spray.routing.directives._
  
  @ApiOperation(
    value = "Find Publisher in catalog", httpMethod = "GET", 
    produces = "application/json; charset=UTF-8",
    response = classOf[PublisherResource]
  )
  @ApiImplicitParams(Array(
    new ApiImplicitParam(name = "id", value = "Publisher's ID", required = true, dataType = "Long", paramType = "path")
  ))
  @ApiResponses(Array(
    new ApiResponse(code = 404, message = "Publisher does not exist")
  ))  
  def getPublisher(id: Long) = get { 
    rejectEmptyResponse {
      cache(responseCache) {
        complete {
          publisherService findById id
        }
      }
    }
  }
  
  @ApiOperation(
    value = "Update Publisher", httpMethod = "PUT", 
    produces = "application/json; charset=UTF-8", consumes = "application/json; charset=UTF-8",
    response = classOf[PublisherResource]
  )
   @ApiImplicitParams(Array(
    new ApiImplicitParam(required = true, dataType = "org.packtpublishing.web.PublisherPayload", paramType = "body")
  ))
  @ApiResponses(Array(
    new ApiResponse(code = 401, message = "User is not authenticated"),
    new ApiResponse(code = 403, message = "User is not authorized"),
    new ApiResponse(code = 404, message = "Publisher does not exist"),
    new ApiResponse(code = 409, message = "Publisher with such name already exists")
  ))
  def updatePublisher(id: Long) = put { 
    authenticate(BasicAuth(userService.authenticate _, realm = "Book Catalog")) { user =>
      authorize(userService canManagePublishers user) {
        requestUri { uri =>
          entity(as[PublisherPayload]) { payload =>
            onComplete(publisherService.updateById(id, payload.name)) {
              case Success(publisher) => rejectEmptyResponse { 
                complete(responseCache.remove(uri).fold(OK)(_ => OK), publisher) 
              }
              case Failure(ex) => complete(Conflict)
            }
          }
        }
      }
    }
  }
  
  @ApiOperation(value = "Delete Publisher from the catalog", httpMethod = "DELETE")
  @ApiImplicitParams(Array(
    new ApiImplicitParam(name = "id", value = "Publisher's ID", required = true, dataType = "Long", paramType = "path")
  ))  
  @ApiResponses(Array(
    new ApiResponse(code = 204, message = "Publisher has been deleted"),
    new ApiResponse(code = 401, message = "User is not authenticated"),
    new ApiResponse(code = 403, message = "User is not authorized"),
    new ApiResponse(code = 404, message = "Publisher was not found")
  ))  
  def deletePublisher(id: Long) = delete {
    authenticate(BasicAuth(userService.authenticate _, realm = "Book Catalog")) { user =>
      authorize(userService canManagePublishers user) {
        requestUri { uri => 
          complete {
            publisherService deleteById id map {
              case true => responseCache.remove(uri).fold(NoContent)(_ => NoContent)
              case _ => NotFound
            }
          }
        }
      }
    }
  }
  
  @ApiOperation(
    value = "Get all publisher books", httpMethod = "GET", 
    produces = "application/json; charset=UTF-8",
    response = classOf[BookResource], responseContainer = "List"
  )
  @ApiImplicitParams(Array(
    new ApiImplicitParam(name = "id", value = "Publisher's ID", required = true, dataType = "Long", paramType = "path")
  ))
  @ApiResponses(Array(
    new ApiResponse(code = 404, message = "Publisher does not exist")
  ))  
  @Path("/{id}/books")
  def getPublisherBooks(id: Long) = get {
    rejectEmptyResponse {
      complete {
        publisherService findById id map { 
          _ map { publisher =>
            bookService.findByPublisher(id) map {
              _ map { BookResource(_, publisher) }
            }
          }
        }
      }
    }
  }
  
  @ApiOperation(
    value = "Add New Book", httpMethod = "POST", 
    produces = "application/json; charset=UTF-8", consumes = "application/json; charset=UTF-8", 
    response = classOf[BookResource]
  )
  @ApiImplicitParams(Array(
    new ApiImplicitParam(name = "id", value = "Publisher's ID", required = true, dataType = "Long", paramType = "path"),
    new ApiImplicitParam(required = true, dataType = "org.packtpublishing.web.BookCreatePayload", paramType = "body")
  ))
  @ApiResponses(Array(
    new ApiResponse(code = 201, message = "Book has been added", response = classOf[PublisherResource]),
    new ApiResponse(code = 401, message = "User is not authenticated"),
    new ApiResponse(code = 403, message = "User is not authorized"),
    new ApiResponse(code = 409, message = "Books with such ISBN already exists")
  ))
  @Path("/{id}/books")
  def addPublisherBook(id: Long) = post {
    authenticate(BasicAuth(userService.authenticate _, realm = "Book Catalog")) { user =>
      authorize(userService canManageBooks user) {                  
        requestUri { uri =>
          entity(as[BookCreatePayload]) { payload =>
            val result = publisherService.findById(id) flatMap {  
              case Some(publisher) =>
                val newBook = Book(payload.isbn, payload.title, payload.author, payload.publishingDate, id)
                bookService.add(newBook) map { book => Some((publisher, book)) }
              case None => Future.successful(None)
            } 
            
            onComplete(result) {
              case Success(Some((publisher, book))) => 
                respondWithHeader(Location(uri.withPath(root(uri.path) / "books" / payload.isbn))) { 
                  complete(Created, BookResource(book, publisher))
                }
              case Success(_) => complete(NotFound)
              case Failure(ex) => complete(Conflict)
            }
          }
        }
      }
    }
  }
  
  val publisherRoutes = pathPrefix("publishers") {
    pathEnd {
      getAllPublishers ~ addPublisher
    } ~
    pathPrefix(LongNumber) { id =>
      path("books") {
        getPublisherBooks(id) ~ addPublisherBook(id)
      } ~
      pathEnd {
        getPublisher(id) ~ updatePublisher(id) ~ deletePublisher(id)
      }
    }
  }
}

@Api(value = "/api/v1/books", description = "Book Catalog: books management")
trait BookRestServiceRoutes {
  val bookService: BookService
  val userService: UserService
  
  import spray.httpx.SprayJsonSupport._
  import spray.httpx.marshalling._
  
  import scala.concurrent.ExecutionContext.Implicits.global
  import org.packtpublishing.web.BooksJsonProtocol._

  /**
   * GET    /api/v1/books
   * GET    /api/v1/books/{isbn}
   * PUT    /api/v1/books/{isbn}
   * DELETE /api/v1/books/{isbn}
   * 
   * GET    /api/v1/publishers
   * POST   /api/v1/publishers
   * GET    /api/v1/publishers/{id}
   * PUT    /api/v1/publishers/{id}
   * DELETE /api/v1/publishers/{id}
   * 
   * GET    /api/v1/publishers/{id}/books
   * POST   /api/v1/publishers/{id}/books
   */
  
  @ApiOperation(
    value = "Get All Books", httpMethod = "GET", 
    produces = "application/json; charset=UTF-8", 
    response = classOf[BookResource], responseContainer="List"
  )
  def getAllBooks = pathEndOrSingleSlash {
    get {
      complete {
        bookService.findAll map { 
          _ map { case (book, publisher) => BookResource(book, publisher) }
        }
      }
    } 
  }
  
  @ApiOperation(
    value = "Update Book", httpMethod = "PUT", 
    produces = "application/json; charset=UTF-8", consumes = "application/json; charset=UTF-8", 
    response = classOf[BookResource]
  )
  @ApiImplicitParams(Array(
    new ApiImplicitParam(name = "isbn", required = true, dataType = "String", paramType = "path", value = "Book ISBN"),
    new ApiImplicitParam(required = true, dataType = "org.packtpublishing.web.BookUpdatePayload", paramType = "body")
  ))
  @ApiResponses(Array(
    new ApiResponse(code = 401, message = "User is not authenticated"),
    new ApiResponse(code = 403, message = "User is not authorized"),
    new ApiResponse(code = 404, message = "Book was not found")
  ))
  def updateBook(isbn: String) = put {
    authenticate(BasicAuth(userService.authenticate _, realm = "Book Catalog")) { user =>
      authorize(userService canManageBooks user) {
        entity(as[BookUpdatePayload]) { payload =>
          rejectEmptyResponse {
            complete {
              bookService updateByIsbn(isbn, payload.title, payload.author, payload.publishingDate) map {
                _ map { case (book, publisher) => BookResource(book, publisher) }
              }
            }
          }
        }
      }
    }
  }
  
  @ApiOperation(value = "Delete Book from the catalog", httpMethod = "DELETE")
  @ApiImplicitParams(Array(
    new ApiImplicitParam(name = "isbn", required = true, dataType = "String", paramType = "path", value = "Book ISBN")
  ))  
  @ApiResponses(Array(
    new ApiResponse(code = 204, message = "Book has been deleted"),
    new ApiResponse(code = 401, message = "User is not authenticated"),
    new ApiResponse(code = 403, message = "User is not authorized"),
    new ApiResponse(code = 404, message = "Book was not found")
  ))
  def deleteBook(isbn: String) = delete {
    authenticate(BasicAuth(userService.authenticate _, realm = "Book Catalog")) { user =>
      authorize(userService canManageBooks user) {
        complete {
          bookService deleteByIsbn isbn map {
            case true => NoContent
            case _ => NotFound
          }
        }
      }
    }
  }
  
  @ApiOperation(value = "Find Book in the catalog", httpMethod = "GET", response = classOf[BookResource])
  @ApiImplicitParams(Array(
    new ApiImplicitParam(name = "isbn", required = true, dataType = "String", paramType = "path", value = "Book ISBN")
  ))
  @ApiResponses(Array(
    new ApiResponse(code = 404, message = "Book was not found")
  ))  
  def getBook(isbn: String) = get {
    onSuccess(bookService findByIsbn isbn) {
      case Some((book, publisher)) => 
        conditional(EntityTag(ETagHash(book, publisher)), DateTime.now) {
          complete(BookResource(book, publisher))
      }
      case None => complete(NotFound)
    }
  }
  
  val bookRoutes = pathPrefix("books") {
    getAllBooks ~
    path("""\d{3}[-]\d{10}""".r) { isbn =>
       getBook(isbn) ~ updateBook(isbn) ~ deleteBook(isbn)
    }
  }
}