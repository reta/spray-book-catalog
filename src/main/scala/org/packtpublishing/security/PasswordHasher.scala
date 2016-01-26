package org.packtpublishing.security

import javax.crypto.spec.PBEKeySpec
import javax.crypto.SecretKeyFactory
import java.security.SecureRandom

object PasswordHasher { 
  lazy val random = SecureRandom.getInstance("SHA1PRNG")
  
  def hash(password: String): (Array[Byte], Array[Byte]) = {
    val salt = random.generateSeed(32)
    (hash(password, salt), salt)
  }
  
  def hash(password: String, salt: Array[Byte]) = {
    val secretKeyFactory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA512")
    val keySpec = new PBEKeySpec(password.toCharArray(), salt, 1000, 256)
    val secretKey = secretKeyFactory.generateSecret(keySpec)
    secretKey.getEncoded()
  }    
}