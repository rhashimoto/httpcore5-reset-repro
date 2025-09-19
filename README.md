Reproduction of HttpComponents Core5 exception:
```
INFO: org.apache.hc.core5.http2.H2StreamResetException: Stream reset (1)
        at org.apache.hc.core5.http2.impl.nio.AbstractH2StreamMultiplexer.consumeFrame(AbstractH2StreamMultiplexer.java:874)
        at org.apache.hc.core5.http2.impl.nio.AbstractH2StreamMultiplexer.onInput(AbstractH2StreamMultiplexer.java:455)
        at org.apache.hc.core5.http2.impl.nio.AbstractH2IOEventHandler.inputReady(AbstractH2IOEventHandler.java:65)
        at org.apache.hc.core5.http2.impl.nio.ServerH2IOEventHandler.inputReady(ServerH2IOEventHandler.java:39)
        at org.apache.hc.core5.reactor.ssl.SSLIOSession.decryptData(SSLIOSession.java:618)
        at org.apache.hc.core5.reactor.ssl.SSLIOSession.access$200(SSLIOSession.java:74)
        at org.apache.hc.core5.reactor.ssl.SSLIOSession$1.inputReady(SSLIOSession.java:204)
        at org.apache.hc.core5.reactor.InternalDataChannel.onIOEvent(InternalDataChannel.java:143)
        at org.apache.hc.core5.reactor.InternalChannel.handleIOEvent(InternalChannel.java:51)
        at org.apache.hc.core5.reactor.SingleCoreIOReactor.processEvents(SingleCoreIOReactor.java:176)
        at org.apache.hc.core5.reactor.SingleCoreIOReactor.doExecute(SingleCoreIOReactor.java:125)
        at org.apache.hc.core5.reactor.AbstractSingleCoreIOReactor.execute(AbstractSingleCoreIOReactor.java:92)
        at org.apache.hc.core5.reactor.IOReactorWorker.run(IOReactorWorker.java:44)
        at java.base/java.lang.Thread.run(Thread.java:1447)
```

# Repro steps
* Launch the application. This will start a HTTP server on port 8080, and a HTTPS server on port 8443.
* Use a web browser to visit URLs:
  * http://localhost:8080/
  * https://localhost:8443/

  These URLs will return the current date and time.
* Visit URLs:
  * http://localhost:8080/mjpeg
  * https://localhost:8443/mjpeg
 
  These URLs should display an infinite sequence of JPEG images, but only the HTTP server
  succeeds in this. On the HTTPS server the browser disconnects and the server
  logs an exception.

# Acknowledgements
[Test images](https://commons.wikimedia.org/wiki/Category:Brain_MRI_case_0038) are © Nevit Dilmen,
used under Creative Commons license CC BY-SA 3.0.
