import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.launch
import org.apache.hc.core5.http.ContentType
import org.apache.hc.core5.http.EntityDetails
import org.apache.hc.core5.http.HttpHeaders
import org.apache.hc.core5.http.HttpRequest
import org.apache.hc.core5.http.HttpStatus
import org.apache.hc.core5.http.Message
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
import java.io.IOException
import java.io.OutputStream
import java.nio.ByteBuffer
import java.util.concurrent.Executors
import java.util.concurrent.LinkedBlockingDeque
import java.util.concurrent.atomic.AtomicReference
import java.util.logging.Logger

private val JPEG_FILES = listOf(
  "Brain_MRI_0038_16_t1_pd_t2.jpg",
  "Brain_MRI_0038_17_t1_pd_t2.jpg",
  "Brain_MRI_0038_18_t1_pd_t2.jpg",
  "Brain_MRI_0038_19_t1_pd_t2.jpg",
  "Brain_MRI_0038_20_t1_pd_t2.jpg",
)

private val logger = Logger.getLogger("MotionJPEGHandler")

class MotionJPEGHandler(
  private val coroutineScope: CoroutineScope
) : AsyncServerRequestHandler<Message<HttpRequest, Void>> {
  private val executor = Executors.newSingleThreadExecutor()
  private val dispatcher = executor.asCoroutineDispatcher()

  init {
    coroutineScope.launch {
      try {
        awaitCancellation()
      } finally {
        executor.shutdown()
      }
    }
  }

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
    coroutineScope.launch(dispatcher) {
      try {
        while (true) {
          for (filename in JPEG_FILES) {
            object {}::class.java.getResourceAsStream(filename)!!.use {
              val byteArray = it.readBytes()
              logger.info("handle: JPEG file loaded ${byteArray.size}")
              entity.post(ByteBuffer.wrap(byteArray))
            }
            delay(1000)
          }
        }
      } catch(e: IOException) {
        logger.info("handler exception $e")
      }
    }

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
    private val TAG = this::class.java.declaringClass!!.simpleName
    private const val BOUNDARY = "o3L1JqT8W9yR4cK7pX0vF2zA"
  }

  private val bufferQueue = LinkedBlockingDeque<ByteBuffer>()
  private val error = AtomicReference<Exception?>()

  override fun availableData() = Int.MAX_VALUE
  override fun isRepeatable() = false

  override fun produceData(channel: StreamChannel<ByteBuffer>) {
    // Block until a buffer is available to write.
    val byteBuffer = bufferQueue.takeFirst()
    logger.info("produceData: ${byteBuffer.remaining()} acquired")
    channel.write(byteBuffer)

    if (byteBuffer.hasRemaining()) {
      // This buffer was not completely written so restore it to the
      // queue for the next call.
      logger.info("produceData: ${byteBuffer.remaining()} / ${byteBuffer.capacity()} remaining")
      bufferQueue.putFirst(byteBuffer)
    }
  }

  override fun failed(cause: java.lang.Exception) {
    // Save the error to rethrow from post().
    logger.info("MotionJPEGEntityProducer failed $cause")
    error.set(cause)
  }

  fun post(jpegImage: ByteBuffer) {
    // Rethrow any error to stop streaming images.
    error.get()?.let {
      throw it
    }

    // Enqueue the multipart header and JPEG data.
    val header = StringBuilder()
      .append("--$BOUNDARY\r\n")
      .append("${HttpHeaders.CONTENT_TYPE}: image/jpeg\r\n")
      .append("${HttpHeaders.CONTENT_LENGTH}: ${jpegImage.remaining()}\r\n")
      .append("\r\n")
    val hBuffer = ByteBuffer.wrap(header.toString().toByteArray())
    bufferQueue.putLast(hBuffer)
    bufferQueue.putLast(jpegImage)
  }
}
