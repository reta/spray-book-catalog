package org.packtpublishing.web

import spray.routing.HttpServiceActor
import akka.actor.ActorLogging
import spray.routing._
import Directives._
import org.packtpublishing.service.{BookService, PublisherService}
import org.packtpublishing.service.PersistenceService
import org.packtpublishing.model.Book
import org.packtpublishing.model.Publisher
import org.joda.time.LocalDate
import spray.http.HttpResponse
import org.h2.jdbc.JdbcSQLException
import scala.util.Success
import scala.util.Failure
import spray.http.StatusCodes._
import spray.http.HttpHeaders._
import spray.http.Uri
import scala.concurrent.Future
import spray.routing.authentication.BasicAuth

class BookRestService(val persistence: PersistenceService) extends HttpServiceActor with ActorLogging with BookRestServiceRoutes {
  val bookService = new BookService(persistence)
  val publisherService = new PublisherService(persistence)
  
  def receive = runRoute {
    routes
  }
}

trait BookRestServiceRoutes {
  val bookService: BookService
  val publisherService: PublisherService
  
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
  
  def root(path: Uri.Path): Uri.Path = 
    Uri.Path(path.toString.split("/").take(3).mkString("/"))
  
  val bookRoutes = pathPrefix("books") {
    pathEnd {
      get {
        complete {
          bookService.findAll map { 
            _ map { case (book, publisher) => BookResource(book, publisher) }
          }
        }
      } 
    } ~
    path("""\d{3}[-]\d{10}""".r) { isbn =>
      get {
        rejectEmptyResponse {
          complete {
            bookService findByIsbn isbn map {
              _ map { case (book, publisher) => BookResource(book, publisher) }
            }          
          }
        }
      } ~
      put {
        entity(as[BookUpdatePayload]) { payload =>
          rejectEmptyResponse {
            complete {
              bookService updateByIsbn(isbn, payload.title, payload.author, payload.publishingDate) map {
                _ map { case (book, publisher) => BookResource(book, publisher) }
              }
            }
          }
        }
      } ~
      delete {
        complete {
          bookService deleteByIsbn isbn map {
            case true => NoContent
            case _ => NotFound
          }
        }
      }
    }
  }
  
  val publisherRoutes = pathPrefix("publishers") {
    pathEnd {
      get {
        complete {
          publisherService.findAll
        }
      } ~
      post {
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
    } ~
    pathPrefix(LongNumber) { id =>
      path("books") {
        get {
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
        } ~
        post {
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
      } ~
      pathEnd {
        get { 
          rejectEmptyResponse {
            complete {
              publisherService findById id
            }
          }
        } ~
        put { 
          entity(as[PublisherPayload]) { payload =>
            onComplete(publisherService.updateById(id, payload.name)) {
              case Success(publisher) => rejectEmptyResponse { complete(OK, publisher) }
              case Failure(ex) => complete(Conflict)
            }
          }
        } ~
        delete {
          complete {
            publisherService deleteById id map {
              case true => NoContent
              case _ => NotFound
            }
          }
        }
      }
    }
  }

  val routes = pathPrefix("api" / "v1") { 
    bookRoutes ~
    publisherRoutes
  }
}