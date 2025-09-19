import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.runBlocking
import org.conscrypt.Conscrypt
import java.security.KeyStore
import javax.net.ssl.KeyManagerFactory
import javax.net.ssl.SSLContext

fun main() {
  runBlocking {
    // This value can be set to null to use HTTP instead
    // of HTTPS.
    val sslContext: SSLContext? = buildSSLContext()

    startWebServer(
      this,
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
