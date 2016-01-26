package org.packtpublishing.security

import java.security.{SecureRandom, KeyStore}
import javax.net.ssl.{KeyManagerFactory, SSLContext, TrustManagerFactory}
import spray.io.ServerSSLEngineProvider
import resource._

trait SslSupport {
  val random = SecureRandom.getInstance("SHA1PRNG")
  
  val keyStoreLocation = "/spray-book-catalog.jks"
  val keyStorePassword = "passw0rd"

  implicit def sslContext: SSLContext = {    
    val keyStore = KeyStore.getInstance("jks")
    
    for (jks <- managed(getClass.getResourceAsStream(keyStoreLocation))) {
      keyStore.load(jks, keyStorePassword.toCharArray)
    }
    
    val keyManagerFactory = KeyManagerFactory.getInstance("SunX509")
    keyManagerFactory.init(keyStore, keyStorePassword.toCharArray)
    
    val trustManagerFactory = TrustManagerFactory.getInstance("SunX509")
    trustManagerFactory.init(keyStore)
    
    val context = SSLContext.getInstance("TLS")
    context.init(keyManagerFactory.getKeyManagers, trustManagerFactory.getTrustManagers, random)
    context
  }
   
  implicit def sslEngineProvider: ServerSSLEngineProvider = {
    ServerSSLEngineProvider { engine =>
      engine.setEnabledProtocols(Array("TLSv1", "TLSv1.1", "TLSv1.2"))
      engine
    }
  }
}