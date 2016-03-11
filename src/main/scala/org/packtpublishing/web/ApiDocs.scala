package org.packtpublishing.web

import com.wordnik.swagger.model.ApiInfo
import com.gettyimages.spray.swagger.SwaggerHttpService
import scala.reflect.runtime.universe.typeOf
import akka.actor.Actor
import spray.http.HttpRequest

trait ApiDocs {
  this: Actor =>

  val apiDocsRoutes = new SwaggerHttpService { 
    override def apiTypes = Seq(typeOf[BookRestServiceRoutes], typeOf[PublisherRestServiceRoutes], typeOf[BookSearchServiceRoutes])
    override def apiVersion = "1.0"
    override def baseUrl = "/"
    override def docsPath = "api-docs"
    override def actorRefFactory = context
    
    override def apiInfo = Some(
      new ApiInfo(
        "Book Catalog", 
        "Book Catalog REST(ful) web API", 
        "", 
        "support@packtpub.com", 
        "Apache License Version 2.0", 
        "http://www.apache.org/licenses/LICENSE-2.0"
      )
    )
  }.routes
}