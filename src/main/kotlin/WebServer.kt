import kotlinx.coroutines.*
import org.apache.hc.core5.function.Supplier
import org.apache.hc.core5.http.*
import org.apache.hc.core5.http.impl.bootstrap.HttpAsyncServer
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
import org.apache.hc.core5.http.protocol.*
import org.apache.hc.core5.http2.impl.nio.bootstrap.H2ServerBootstrap
import org.apache.hc.core5.http2.ssl.ConscryptServerTlsStrategy
import org.apache.hc.core5.reactor.IOReactorConfig
import org.apache.hc.core5.reactor.ListenerEndpoint
import org.conscrypt.Conscrypt
import java.net.BindException
import java.net.InetSocketAddress
import java.security.KeyStore
import java.util.concurrent.ExecutionException
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.logging.Logger
import javax.net.ssl.KeyManagerFactory
import javax.net.ssl.SSLContext

private val logger = Logger.getLogger("WebServer")

fun startWebServer(
  coroutineScope: CoroutineScope,
  keyStore: KeyStore?,
  handlers: Map<String, AsyncServerRequestHandler<Message<HttpRequest, Void>>>
) {
  val executor = Executors.newSingleThreadExecutor()
  val dispatcher = executor.asCoroutineDispatcher()
  coroutineScope.launch(dispatcher) {
    var server: HttpAsyncServer? = null
    try {
      // TODO: Handle network changes.
      server = buildServer(keyStore, handlers)
      server.start()

      // During iterative development, sometimes the port is not immediately
      // available when the app is restarted. Loop until successful.
      var endpoint: ListenerEndpoint? = null
      do {
        try {
          endpoint = server.listen(
            InetSocketAddress(8080), if (keyStore != null) URIScheme.HTTPS else URIScheme.HTTP
          ).get()
        } catch (e: ExecutionException) {
          if (e.cause is BindException) {
            logger.warning("Port in use, retrying...")
            delay(5000)
          } else {
            throw e
          }
        }
      } while (endpoint == null)
      if (keyStore == null) {
        logger.warning("Web traffic is NOT encrypted")
      }
      logger.info("Listening on ${endpoint.address}")
      awaitCancellation()
    } catch (e: Exception) {
      logger.severe("Error starting server $e")
      throw e
    } finally {
      logger.info("Shutting down")
      server?.close()
      executor.shutdown()
    }
  }
}

private fun buildServer(
  keyStore: KeyStore?, handlers: Map<String, AsyncServerRequestHandler<Message<HttpRequest, Void>>>
): HttpAsyncServer {
  // In cases where the incoming Host header value or SNI does not match
  // the canonical host name, using the register() method to install a
  // request handler will result in HTTP 421 (Misdirected Request)
  // responses. A workaround is to use a custom router that always routes
  // to the default authority.
  // See https://marc.info/?l=httpclient-users&m=174051095223119&w=2
  val routerBuilder = RequestRouter.builder<Supplier<AsyncServerExchangeHandler>>()
    .resolveAuthority(RequestRouter.LOCAL_AUTHORITY_RESOLVER)
  for ((path, handler) in handlers) {
    routerBuilder.addRoute(RequestRouter.LOCAL_AUTHORITY, path) {
      BasicServerExchangeHandler(handler)
    }
  }

  val serverBootstrap = H2ServerBootstrap.bootstrap().setCanonicalHostName("motogpure.lan").setIOReactorConfig(
      IOReactorConfig.custom().setSoTimeout(10, TimeUnit.SECONDS).setTcpNoDelay(true).build()
    ).setHttpProcessor(
      HttpProcessorBuilder.create().add(ResponseConformance()).add(ResponseContent()).add(ResponseConnControl())
        .add(ResponseDate()).add(ResponseServer()).build()
    ).setExceptionCallback { logger.severe("Error $it") }.setRequestRouter(routerBuilder.build())

  if (keyStore != null) {
    val tlsContext = SSLContext.getInstance("TLSv1.2", Conscrypt.newProvider()).apply {
      val keyManagerFactory = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm())
      keyManagerFactory.init(keyStore, "password".toCharArray())
      init(keyManagerFactory.keyManagers, null, null)
    }
    serverBootstrap.setTlsStrategy(ConscryptServerTlsStrategy(tlsContext))
  }
//   If a custom router is not used, register handlers this way.
//  for ((path, handler) in handlers) {
//    serverBootstrap.register(path, handler)
//  }
  return serverBootstrap.create()
}

val DemoRequestHandler = object : AsyncServerRequestHandler<Message<HttpRequest, Void>> {
  override fun prepare(
    request: HttpRequest, entityDetails: EntityDetails?, context: HttpContext
  ): AsyncRequestConsumer<Message<HttpRequest, Void>> {
    logger.info("prepare: $request")
    val dataConsumer: AsyncEntityConsumer<Void>? = if (entityDetails != null) DiscardingEntityConsumer() else null
    return BasicRequestConsumer(dataConsumer)
  }

  override fun handle(
    requestObject: Message<HttpRequest, Void>,
    responseTrigger: AsyncServerRequestHandler.ResponseTrigger,
    context: HttpContext
  ) {
    logger.info("handle: $requestObject")
    val request = requestObject.head
    val headers = request.headers.joinToString("\n") {
      "${it.name}: ${it.value}"
    }
    val response = AsyncResponseBuilder.create(HttpStatus.SC_OK).setEntity(
        AsyncEntityProducers.create(
          "$request\n$headers", ContentType.TEXT_PLAIN
        )
      ).build()
    responseTrigger.submitResponse(response, context)
  }
}