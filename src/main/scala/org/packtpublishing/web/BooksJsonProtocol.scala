package org.packtpublishing.web

import spray.json._
import org.packtpublishing.model.Book
import org.joda.time.format.ISODateTimeFormat
import org.packtpublishing.model.Publisher
import org.joda.time.LocalDate

case class BookResource(isbn: String, title: String, author: String, 
  publishingDate: LocalDate, publisher: Publisher)

case class BookUpdatePayload(title: String, author: String, 
  publishingDate: LocalDate)

case class BookCreatePayload(isbn: String, title: String, author: String, 
  publishingDate: LocalDate)
  
case class PublisherPayload(name: String)
   
object BookResource {
  def apply(book: Book, publisher: Publisher): BookResource = 
    BookResource(book.isbn, book.title, book.author, book.publishingDate, publisher) 
}
   
object BooksJsonProtocol extends DefaultJsonProtocol {
  implicit object LocalDateJsonProtocol extends JsonFormat[LocalDate] {
    private val formatter = ISODateTimeFormat.date()
    
    def write(date: LocalDate) = JsString(date.toString(formatter))
    
    def read(value: JsValue) = value match { 
      case JsString(date) => formatter.parseLocalDate(date)
      case _ => deserializationError("String value expected")
    }
  }
  
  implicit def publisherFormat = jsonFormat2(Publisher.apply)
  implicit def publisherPayloadFormat = jsonFormat1(PublisherPayload.apply)
  implicit def bookResourceFormat = jsonFormat5(BookResource.apply)
  implicit def bookUpdatePayloadFormat = jsonFormat3(BookUpdatePayload.apply)
  implicit def bookCreatePayloadFormat = jsonFormat4(BookCreatePayload.apply)
}