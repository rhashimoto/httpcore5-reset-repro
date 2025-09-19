import org.apache.hc.core5.http.*
import org.apache.hc.core5.http.message.BasicHeader
import org.apache.hc.core5.http.message.BasicNameValuePair
import org.apache.hc.core5.http.nio.AsyncEntityConsumer
import org.apache.hc.core5.http.nio.AsyncRequestConsumer
import org.apache.hc.core5.http.nio.AsyncServerRequestHandler
import org.apache.hc.core5.http.nio.StreamChannel
import org.apache.hc.core5.http.nio.entity.AbstractBinAsyncEntityProducer
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

class MotionJPEGHandler(
) : AsyncServerRequestHandler<Message<HttpRequest, Void>> {
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
    val entity = MotionJPEGEntityProducer()
    val response = AsyncResponseBuilder.create(HttpStatus.SC_OK)
      .setHeader(BasicHeader(HttpHeaders.CACHE_CONTROL, "no-cache"))
      .setHeader(BasicHeader(HttpHeaders.PRAGMA, "no-cache"))
      .setEntity(entity)
      .build()
    responseTrigger.submitResponse(response, context)
  }
}

private class MotionJPEGEntityProducer(
  fragmentSizeHint: Int = 65536
) : AbstractBinAsyncEntityProducer(
  fragmentSizeHint,
  ContentType.create(
    "multipart/x-mixed-replace",
    BasicNameValuePair("boundary", BOUNDARY))) {
  companion object {
    private const val BOUNDARY = "o3L1JqT8W9yR4cK7pX0vF2zA"
  }

  private var index = 0
  private var bufferQueue = ArrayDeque<ByteBuffer>()

  override fun availableData() = Int.MAX_VALUE
  override fun isRepeatable() = false

  override fun produceData(channel: StreamChannel<ByteBuffer>) {
    // Block until a buffer is available to write.
    if (bufferQueue.isEmpty()) {
      val filename = JPEG_FILES[index++ % JPEG_FILES.size]
      val jpegImage = object {}::class.java.getResourceAsStream(filename)!!.use {
        ByteBuffer.wrap(it.readBytes())
      }

      val header = StringBuilder()
        .append("--$BOUNDARY\r\n")
        .append("${HttpHeaders.CONTENT_TYPE}: image/jpeg\r\n")
        .append("${HttpHeaders.CONTENT_LENGTH}: ${jpegImage.remaining()}\r\n")
        .append("\r\n")
      val hBuffer = ByteBuffer.wrap(header.toString().toByteArray())
      bufferQueue.addLast(hBuffer)
      bufferQueue.addLast(jpegImage)
    }

    val byteBuffer = bufferQueue.first()
    val nWritten = channel.write(byteBuffer)
    logger.info("produceData: $nWritten written")

    if (!byteBuffer.hasRemaining()) {
      bufferQueue.removeFirst()
    } else {
      logger.info("produceData: ${byteBuffer.remaining()} remaining")
    }
  }

  override fun failed(cause: java.lang.Exception) {
    logger.info("MotionJPEGEntityProducer failed $cause")
    logger.info(cause.stackTraceToString())
  }
}
