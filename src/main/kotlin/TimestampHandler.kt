import org.apache.hc.core5.http.ContentType
import org.apache.hc.core5.http.EntityDetails
import org.apache.hc.core5.http.HttpRequest
import org.apache.hc.core5.http.HttpStatus
import org.apache.hc.core5.http.Message
import org.apache.hc.core5.http.nio.AsyncEntityConsumer
import org.apache.hc.core5.http.nio.AsyncRequestConsumer
import org.apache.hc.core5.http.nio.AsyncServerRequestHandler
import org.apache.hc.core5.http.nio.entity.AsyncEntityProducers
import org.apache.hc.core5.http.nio.entity.DiscardingEntityConsumer
import org.apache.hc.core5.http.nio.support.AsyncResponseBuilder
import org.apache.hc.core5.http.nio.support.BasicRequestConsumer
import org.apache.hc.core5.http.protocol.HttpContext
import java.util.Date

// Very simple handler that returns the date and time.
// This handler works both with HTTP and HTTPS.
val TimestampHandler = object : AsyncServerRequestHandler<Message<HttpRequest, Void>> {
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
    val response = AsyncResponseBuilder.create(HttpStatus.SC_OK).setEntity(
      AsyncEntityProducers.create(
        Date().toString(), ContentType.TEXT_PLAIN
      )
    ).build()
    responseTrigger.submitResponse(response, context)
  }
}