import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.runBlocking
import org.conscrypt.Conscrypt
import java.security.KeyStore
import javax.net.ssl.KeyManagerFactory
import javax.net.ssl.SSLContext

fun main() {
  val handlers = mapOf(
    "/" to TimestampHandler,
    "/mjpeg" to MotionJPEGHandler(),
    "/body" to BodyHandler
  )

  runBlocking {
    // Run unencrypted web server on port 8080.
    startWebServer(
      this,
      8080,
      null,
      handlers
    )

    // Run TLS web server on port 8443.
    val sslContext: SSLContext = buildSSLContext()
    startWebServer(
      this,
      8443,
      sslContext,
      handlers
    )
    awaitCancellation()
  }
}

private fun buildSSLContext(): SSLContext {
  val keyStorePath = "/keyStore.p12"
  val keyStorePassword = "password".toCharArray()

  val keyStore = KeyStore.getInstance("PKCS12")
  object {}::class.java.getResourceAsStream(keyStorePath).use {
    keyStore.load(it, keyStorePassword)
  }

  return SSLContext.getInstance("TLSv1.2", Conscrypt.newProvider()).apply {
    val keyManagerFactory = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm())
    keyManagerFactory.init(keyStore, keyStorePassword)
    init(keyManagerFactory.keyManagers, null, null)
  }
}
