package org.packtpublishing.web

import spray.caching.LruCache
import spray.caching.Cache

trait Caching {
  import spray.routing.directives.CachingDirectives._
  
  val responseCache: Cache[RouteResponse] = LruCache(maxCapacity = 1000)
}