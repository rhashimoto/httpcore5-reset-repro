import org.apache.hc.core5.http.*
import org.apache.hc.core5.http.message.BasicNameValuePair
import org.apache.hc.core5.http.nio.AsyncEntityConsumer
import org.apache.hc.core5.http.nio.AsyncRequestConsumer
import org.apache.hc.core5.http.nio.AsyncServerRequestHandler
import org.apache.hc.core5.http.nio.entity.AsyncEntityProducers
import org.apache.hc.core5.http.nio.entity.DiscardingEntityConsumer
import org.apache.hc.core5.http.nio.support.AsyncResponseBuilder
import org.apache.hc.core5.http.nio.support.BasicRequestConsumer
import org.apache.hc.core5.http.protocol.HttpContext
import java.util.logging.Logger

private val logger = Logger.getLogger("MotionJPEGHandler")

// Very simple handler that returns the date and time.
// This handler works both with HTTP and HTTPS.
val BodyHandler = object : AsyncServerRequestHandler<Message<HttpRequest, Void>> {
  private val boundary = "o3L1JqT8W9yR4cK7pX0vF2zA"
  private val contentType = ContentType.create(
    "multipart/x-mixed-replace",
    BasicNameValuePair("boundary", boundary)
  )

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
    logger.info("handle: ${requestObject.head}")

    // Fetch entire multipart body.
    val body = object {}::class.java.getResourceAsStream("multipart.payload")!!.use {
      it.readBytes()
    }

    val response = AsyncResponseBuilder.create(HttpStatus.SC_OK).setEntity(
      AsyncEntityProducers.create(body, contentType)
    ).build()
    responseTrigger.submitResponse(response, context)
  }
}