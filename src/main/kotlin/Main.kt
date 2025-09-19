import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.runBlocking
import org.conscrypt.Conscrypt
import java.security.KeyStore
import javax.net.ssl.KeyManagerFactory
import javax.net.ssl.SSLContext

fun main() {
  runBlocking {
    // Run unencrypted web server on port 8080.
    startWebServer(
      this,
      8080,
      null,
      mapOf(
        "/" to DemoRequestHandler,
        "/mjpeg" to MotionJPEGHandler()
      )
    )

    // Run TLS web server on port 8443.
    val sslContext: SSLContext = buildSSLContext()
    startWebServer(
      this,
      8443,
      sslContext,
      mapOf(
        "/" to DemoRequestHandler,
        "/mjpeg" to MotionJPEGHandler()
      )
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
