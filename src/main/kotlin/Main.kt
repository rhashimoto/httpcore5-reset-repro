import org.apache.hc.core5.function.Supplier
import org.apache.hc.core5.http.*
import org.apache.hc.core5.http.impl.routing.RequestRouter
import org.apache.hc.core5.http.nio.AsyncEntityConsumer
import org.apache.hc.core5.http.nio.AsyncRequestConsumer
import org.apache.hc.core5.http.nio.AsyncServerExchangeHandler
import org.apache.hc.core5.http.nio.AsyncServerRequestHandler
import org.apache.hc.core5.http.nio.entity.AsyncEntityProducers
import org.apache.hc.core5.http.nio.entity.DiscardingEntityConsumer
import org.apache.hc.core5.http.nio.support.AsyncResponseBuilder
import org.apache.hc.core5.http.nio.support.BasicRequestConsumer
import org.apache.hc.core5.http.nio.support.BasicServerExchangeHandler
import org.apache.hc.core5.http.protocol.HttpContext
import org.apache.hc.core5.http.protocol.HttpProcessorBuilder
import org.apache.hc.core5.http2.HttpVersionPolicy
import org.apache.hc.core5.http2.config.H2Config
import org.apache.hc.core5.http2.impl.nio.bootstrap.H2ServerBootstrap
import org.apache.hc.core5.http2.protocol.H2RequestConformance
import org.apache.hc.core5.http2.protocol.H2RequestConnControl
import org.apache.hc.core5.http2.protocol.H2RequestContent
import org.apache.hc.core5.http2.ssl.ConscryptServerTlsStrategy
import org.apache.hc.core5.io.CloseMode
import org.apache.hc.core5.reactor.IOReactorConfig
import org.apache.hc.core5.util.TimeValue
import org.conscrypt.Conscrypt
import java.net.InetSocketAddress
import java.security.KeyStore
import java.util.logging.Logger
import javax.net.ssl.KeyManagerFactory
import javax.net.ssl.SSLContext

private val logger = Logger.getLogger("WebServer")

//TIP To <b>Run</b> code, press <shortcut actionId="Run"/> or
// click the <icon src="AllIcons.Actions.Execute"/> icon in the gutter.
fun main() {
  val port = 8443
  val sslContext = buildSSLContext(
    "/keyStore.p12",
    "password".toCharArray()
  )

  val server = H2ServerBootstrap.bootstrap()
    .setCanonicalHostName("localhost")
    .setIOReactorConfig(IOReactorConfig.DEFAULT)
    .setH2Config(H2Config.DEFAULT)
    .setHttpProcessor(HttpProcessorBuilder.create()
      .add(H2RequestConformance.INSTANCE)
      .add(H2RequestConnControl.INSTANCE)
      .add(H2RequestContent.INSTANCE)
      .build())
    .setExceptionCallback {
      logger.info(it.stackTraceToString())
    }
    .setTlsStrategy(ConscryptServerTlsStrategy(sslContext))
    .setRequestRouter(
      RequestRouter.builder<Supplier<AsyncServerExchangeHandler>>()
        .resolveAuthority(RequestRouter.LOCAL_AUTHORITY_RESOLVER)
        .addRoute(RequestRouter.LOCAL_AUTHORITY, "*") {
          BasicServerExchangeHandler(CustomServerRequestHandler())
        }.build())
    .create()

  server.start()
  val endpoint = server.listen(
    InetSocketAddress(port),
    URIScheme.HTTPS,
    HttpVersionPolicy.NEGOTIATE,
    null).get()
  logger.info("Listening on ${endpoint.address}")

  Runtime.getRuntime().addShutdownHook(Thread() {
    server.close(CloseMode.GRACEFUL)
  })
  server.awaitShutdown(TimeValue.ofDays(Long.MAX_VALUE))
}

private fun buildSSLContext(
  keyStorePath: String,
  keyStorePassword: CharArray
): SSLContext {
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

private class CustomServerRequestHandler : AsyncServerRequestHandler<Message<HttpRequest, Void>> {
  override fun prepare(
    request: HttpRequest, entityDetails: EntityDetails?, context: HttpContext
  ): AsyncRequestConsumer<Message<HttpRequest, Void>> {
    val dataConsumer: AsyncEntityConsumer<Void>? = if (entityDetails != null) DiscardingEntityConsumer() else null
    return BasicRequestConsumer(dataConsumer)
  }

  override fun handle(
    requestObject: Message<HttpRequest, Void>,
    responseTrigger: AsyncServerRequestHandler.ResponseTrigger,
    context: HttpContext
  ) {
    logger.info("handle $context")
    val response = AsyncResponseBuilder.create(HttpStatus.SC_OK)
      .setEntity(
        AsyncEntityProducers.create(
          "Hello, World!\n", ContentType.TEXT_PLAIN
        )
      ).build()
    responseTrigger.submitResponse(response, context)
  }
}