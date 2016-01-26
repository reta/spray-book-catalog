package org.packtpublishing.model

import org.joda.time.LocalDate

case class Book(isbn: String, title: String, author: String, 
  publishingDate: LocalDate, publisherId: Long)
    
case class Publisher(id: Option[Long], name: String)

