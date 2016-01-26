package org.packtpublishing.service

import slick.driver.H2Driver.api._
import com.github.tototoshi.slick.H2JodaSupport._

class PersistenceService {
  import scala.concurrent.ExecutionContext.Implicits.global
  import org.joda.time.LocalDate
  import org.packtpublishing.model._
  import org.packtpublishing.persistence.Publishers._
  import org.packtpublishing.persistence.Books._
  
  lazy val db = Database.forConfig("db")

  def createSchema() = db.run(
    DBIO.seq((
      books.schema ++ 
      publishers.schema).create
    ))
      
  def createDataset() = db.run(
    DBIO.seq(
      publishers ++= Seq(
        Publisher(Some(1), "Packt Publishing"),
        Publisher(Some(2), "Manning Publications")
      ),      
      
      books ++= Seq(
        Book("978-1783281411", "Learning Concurrent Programming in Scala", "Aleksandar Prokopec", new LocalDate(2014, 11, 25), 1), 
        Book("978-1783283637", "Scala for Java Developers", "Thomas Alexandre", new LocalDate(2014, 6, 11), 1),
        Book("978-1935182757", "Scala in Action", "Nilanjan Raychaudhuri", new LocalDate(2013, 4, 13), 2) 
      )
    ))
    
  def findAllBooks = {
    val query = for {(book, publisher) <- books join publishers on (_.publisherId === _.id)} yield (book, publisher)
    db.run(query.result)
  }
  
  def findBookByIsbn(isbn: String) = {
    val query = for {(book, publisher) <- books.filter { _.isbn === isbn } join publishers on (_.publisherId === _.id)} yield (book, publisher)
    db.run(query.result.headOption)
  }
  
  def updateBookByIsbn(isbn: String, title: String, author: String, publishingDate: LocalDate) = {
    val query = for (book <- books.filter { _.isbn === isbn }) yield (book.title, book.author, book.publishingDate)
    db.run(query.update(title, author, publishingDate)) map { _ > 0 }
  }
  
  def findBooksByPublisherId(id: Long) = db.run(books.filter {_.publisherId === id } result)  
  def deleteBookByIsbn(isbn: String) = db.run( books.filter { _.isbn === isbn } delete) map { _ > 0 }
  def persistBook(book: Book) = db.run(books += book) map { _ => book }
  
  def findAllPublishers = db.run(publishers.result)
  def deletePublisherById(id: Long) = db.run(publishers.filter { _.id === id } delete) map { _ > 0 }
  
  def findPublisherById(id: Long) = {
    val query = publishers.filter { _.id === id } 
    db.run(query.result.headOption)
  }
  
  def persistPublisher(name: String) = {
    val query = publishers returning publishers.map(_.id) into ((publisher,id) => publisher.copy(id=Some(id)))
    db.run(query += Publisher(None, name))
  }
  
  def updatePublisherById(id: Long, name: String) = {
    val query = for (publisher <- publishers.filter { _.id === id }) yield (publisher.name)
    db.run(query.update(name)) map { _ > 0 }
  }
}