package org.packtpublishing.web

import spray.json._
import org.packtpublishing.model.Book
import org.joda.time.format.ISODateTimeFormat
import org.packtpublishing.model.Publisher
import org.joda.time.LocalDate
import scala.beans.BeanProperty
import com.wordnik.swagger.annotations.ApiModelProperty
import javax.xml.bind.annotation.XmlElement
import com.fasterxml.jackson.annotation.JsonIgnoreProperties

@JsonIgnoreProperties(Array("bn"))
case class BookResource(@BeanProperty isbn: String, title: String, author: String, 
  publishingDate: LocalDate, publisher: Publisher)

case class BookUpdatePayload(title: String, author: String, 
  publishingDate: LocalDate)

case class BookVolumeInfo(
  title: String,
  publisher: Option[String],
  publishedDate: Option[String],
  authors: Option[List[String]],
  description: Option[String],
  pageCount: Option[Int],
  language: Option[String]
)
case class BookSearchResultItem(volumeInfo: BookVolumeInfo)
case class BookSearchResult(totalItems: Int, items: List[BookSearchResultItem])

@JsonIgnoreProperties(Array("bn"))
case class BookCreatePayload(@BeanProperty isbn: String, title: String, author: String, 
  publishingDate: LocalDate)
  
case class PublisherPayload(name: String)
case class PublisherResource(id: Long, name: String)
   
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
 
  implicit def bookVolumeInfoFormat = jsonFormat7(BookVolumeInfo.apply)
  implicit def bookSearchResultItemFormat = jsonFormat1(BookSearchResultItem.apply)
  implicit def bookSearchResultFormat = jsonFormat2(BookSearchResult.apply)
 
}