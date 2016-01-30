package org.packtpublishing.web

import spray.routing.HttpService

trait ApiDocsUi {
  this: HttpService =>
    
  import spray.http.StatusCodes
  import spray.routing._
  import Directives._
  
  val swaggerUiPath = "META-INF/resources/webjars/swagger-ui/2.1.4"
  
  val apiDocsUiRoutes = get {
    pathEndOrSingleSlash {
      requestUri { uri =>
        parameters('url.?) { 
          case Some(url) => getFromResource(s"$swaggerUiPath/index.html")
          case None => redirect(uri.copy(query = uri.query.+:("url", "/api-docs")), StatusCodes.PermanentRedirect)
        }
      }
    } ~ getFromResourceDirectory(s"$swaggerUiPath/")
  }
}