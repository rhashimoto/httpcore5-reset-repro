# Main.kt
This is the top-level entry point. This contains the code to launch two servers,
one HTTP and one HTTPS. There is no HttpComponents API use in this file.

# MotionJPEGHandler.kt
This is the code that somehow triggers the exception. It is a request handler
that returns an infinite multipart stream of JPEG images. The primary class,
`MotionJPEGHandler`, is essentially boilerplate. The second class in this
file, `MotionJPEGEntityProducer`, is where things happen.

`MotionJPEGEntityProducer` maintains a queue of `ByteBuffer`s. In the
`produceData()` method, if the queue is empty then the next JPEG file
is read (as a resource in the JAR) and buffers for the multipart
header and body are added to the queue. Then the first buffer in the
queue is submitted for output and removed if completely written.

# TimestampHandler.kt
This is a simpler request handler that returns the current date and time.

# WebServer.kt
This configures and starts a HttpComponents asynchronous web server.
