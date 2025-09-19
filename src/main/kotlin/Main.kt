import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.runBlocking
import java.security.KeyStore

fun main() {
  runBlocking {
    val keyStore = loadKeyStore()
    startWebServer(
      this, keyStore, mapOf("/" to DemoRequestHandler)
    )
    awaitCancellation()
  }
}

fun loadKeyStore(): KeyStore? {
  val keyStore = KeyStore.getInstance("PKCS12")
  object {}::class.java.getResourceAsStream("/keyStore.p12").use {
    keyStore.load(it, "password".toCharArray())
  }
  return keyStore
}