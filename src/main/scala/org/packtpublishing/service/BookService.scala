package org.packtpublishing.service

import scala.concurrent.Future
import org.packtpublishing.model.Book
import org.packtpublishing.model.Publisher
import org.joda.time.LocalDate

class BookService(val persistence: PersistenceService) {
  import scala.concurrent.ExecutionContext.Implicits.global
  
  def findAll(): Future[Seq[(Book, Publisher)]] = persistence.findAllBooks
  def findByIsbn(isbn: String): Future[Option[(Book, Publisher)]] = persistence.findBookByIsbn(isbn)
  def findByPublisher(id: Long): Future[Seq[Book]] = persistence.findBooksByPublisherId(id)
  def add(book: Book): Future[Book] = persistence.persistBook(book)
  
  def deleteByIsbn(isbn: String): Future[Boolean] = persistence.deleteBookByIsbn(isbn)
  def updateByIsbn(isbn: String, title: String, author: String, publishingDate: LocalDate): Future[Option[(Book, Publisher)]] = 
    persistence.updateBookByIsbn(isbn, title, author, publishingDate) flatMap {
      case true => persistence.findBookByIsbn(isbn)
      case _ => Future.successful(None)
    }
}