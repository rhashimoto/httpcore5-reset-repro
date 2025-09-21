# Main.kt
This is the top-level entry point. This contains the code to launch two servers,
one HTTP and one HTTPS. There is no HttpComponents API use in this file.

# MotionJPEGHandler.kt
This is the code that somehow triggers the exception. It is a request handler
that returns a multipart stream of JPEG images.

The handler creates a binary entity producer using `AsyncEntityProducers.createBinary()`
with a callback that writes one `ByteBuffer` to the stream per call. Each
`ByteBuffer` is either a multipart header or a multipart body (a JPEG image).

# TimestampHandler.kt
This is a simpler request handler that returns the current date and time.

# WebServer.kt
This configures and starts a HttpComponents asynchronous web server.
