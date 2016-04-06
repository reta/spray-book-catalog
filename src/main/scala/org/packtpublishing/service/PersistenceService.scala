package org.packtpublishing.service

import scala.language.postfixOps 
import slick.driver.H2Driver.api._
import com.github.tototoshi.slick.H2JodaSupport._

class PersistenceService {
  import scala.concurrent.ExecutionContext.Implicits.global
  import org.joda.time.LocalDate
  import org.packtpublishing.model._
  import org.packtpublishing.security.PasswordHasher._
  import org.packtpublishing.persistence.Publishers._
  import org.packtpublishing.persistence.Books._
  import org.packtpublishing.persistence.Users._
  import org.packtpublishing.persistence.UserPermissions._

  lazy val db = Database.forConfig("db")

  def createSchema() = db.run(
    DBIO.seq((
      books.schema ++ 
      publishers.schema ++ 
      users.schema ++ 
      userPermissions.schema).create
    ))
    
  def truncate() = db.run(
    DBIO.seq(
      books.delete, 
      publishers.delete, 
      users.delete,
      userPermissions.delete
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
      ),
      
      users ++= Seq(
        new User("admin", hash("passw0rd")),
        new User("librarian", hash("passw0rd")),
        new User("user", hash("passw0rd"))
      ),
      
      userPermissions ++= Seq(
        Permission("admin", Permissions.MANAGE_BOOKS),
        Permission("admin", Permissions.MANAGE_PUBLISHERS),
        Permission("librarian", Permissions.MANAGE_BOOKS)
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
  
  def findUserByUsername(username: String) = {
    val query = for {
      (u, p) <- users.filter(_.username === username).joinLeft(userPermissions).on(_.username === _.username)
    } yield (u, p.map(_.permission))
    
    db.run(query.result) map { result =>
      result groupBy (_._1) map { case (key, value) =>
        key.copy(permissions = result.filter(_._1.username == key.username).map(_._2).flatten) 
      } headOption 
    }
  }
  
  def persistUser(user: User) = db.run(users += user) map { _ => user }
  
  def addPermissions(user: User, permissions: Seq[Permissions.Permission]) = 
    db.run(userPermissions ++= permissions map { Permission(user.username, _) }).map { _ => 
      user.copy(permissions = user.permissions ++ permissions)
    }
}