import org.apache.hc.core5.http.*
import org.apache.hc.core5.http.message.BasicHeader
import org.apache.hc.core5.http.message.BasicNameValuePair
import org.apache.hc.core5.http.nio.AsyncEntityConsumer
import org.apache.hc.core5.http.nio.AsyncRequestConsumer
import org.apache.hc.core5.http.nio.AsyncServerRequestHandler
import org.apache.hc.core5.http.nio.entity.AsyncEntityProducers
import org.apache.hc.core5.http.nio.entity.DiscardingEntityConsumer
import org.apache.hc.core5.http.nio.support.AsyncResponseBuilder
import org.apache.hc.core5.http.nio.support.BasicRequestConsumer
import org.apache.hc.core5.http.protocol.HttpContext
import java.nio.ByteBuffer
import java.util.logging.Logger

private val JPEG_FILES = arrayListOf(
  "Brain_MRI_0038_16_t1_pd_t2.jpg",
  "Brain_MRI_0038_17_t1_pd_t2.jpg",
  "Brain_MRI_0038_18_t1_pd_t2.jpg",
  "Brain_MRI_0038_19_t1_pd_t2.jpg",
  "Brain_MRI_0038_20_t1_pd_t2.jpg",
)

private val logger = Logger.getLogger("MotionJPEGHandler")

// Handler that returns a motion JPEG sequence using
// MIME type multipart/x-mixed-replace.
//
// This handler works on HTTP but throws a
// H2StreamResetException on HTTPS.
class MotionJPEGHandler(
) : AsyncServerRequestHandler<Message<HttpRequest, Void>> {
  private val boundary = "o3L1JqT8W9yR4cK7pX0vF2zA"
  private val contentType = ContentType.create(
    "multipart/x-mixed-replace",
    BasicNameValuePair("boundary", boundary)
  )

  override fun prepare(
    request: HttpRequest,
    entityDetails: EntityDetails?,
    context: HttpContext
  ): AsyncRequestConsumer<Message<HttpRequest, Void>> {
    val dataConsumer: AsyncEntityConsumer<Void>? =
      if (entityDetails != null) DiscardingEntityConsumer() else null
    return BasicRequestConsumer(dataConsumer)
  }

  override fun handle(
    requestObject: Message<HttpRequest, Void>,
    responseTrigger: AsyncServerRequestHandler.ResponseTrigger,
    context: HttpContext
  ) {
    logger.info("handle: ${requestObject.head}")

    // Send JPEG images to the requesting client.
    var index = 0
    val bufferQueue = ArrayDeque<ByteBuffer>()
    val entity = AsyncEntityProducers.createBinary({ stream ->
      if (bufferQueue.isEmpty()) {
        if (index < JPEG_FILES.size) {
          // Enqueue the buffers for the next image.
          addBuffers(bufferQueue, JPEG_FILES[index++])
        } else {
          stream.endStream()
          return@createBinary
        }
      }

      val buffer = bufferQueue.first()
      stream.write(buffer)
      if (!buffer.hasRemaining()) {
        bufferQueue.removeFirst()
      }
    }, contentType)

    val response = AsyncResponseBuilder.create(HttpStatus.SC_OK)
      .setHeader(BasicHeader(HttpHeaders.CACHE_CONTROL, "no-cache"))
      .setHeader(BasicHeader(HttpHeaders.PRAGMA, "no-cache"))
      .setEntity(entity)
      .build()
    responseTrigger.submitResponse(response, context)
  }

  fun addBuffers(
    bufferQueue: ArrayDeque<ByteBuffer>,
    filename: String
  ) {
    val imageBuffer = object {}::class.java.getResourceAsStream(filename)!!.use {
      ByteBuffer.wrap(it.readBytes())
    }

    // Build multipart header.
    val header = StringBuilder()
      .append("--$boundary\r\n")
      .append("${HttpHeaders.CONTENT_TYPE}: image/jpeg\r\n")
      .append("${HttpHeaders.CONTENT_LENGTH}: ${imageBuffer.remaining()}\r\n")
      .append("\r\n")
    val headerBuffer = ByteBuffer.wrap(header.toString().toByteArray())

    bufferQueue.addLast(headerBuffer)
    bufferQueue.addLast(imageBuffer)
  }
}
