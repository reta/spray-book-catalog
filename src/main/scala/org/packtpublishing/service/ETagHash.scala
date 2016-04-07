package org.packtpublishing.service

import com.roundeights.hasher.Implicits._
import scala.language.postfixOps

import org.packtpublishing.model.Publisher
import org.packtpublishing.model.Book

object ETagHash {
  def apply(book: Book, publisher: Publisher) = 
    (book.title + book.author + publisher.name).md5.hex
}