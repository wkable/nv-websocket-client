/*
 * Copyright (C) 2015-2016 Neo Visionaries Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.neovisionaries.ws.client;


import static com.neovisionaries.ws.client.WebSocketState.CLOSED;
import static com.neovisionaries.ws.client.WebSocketState.CLOSING;
import static com.neovisionaries.ws.client.WebSocketState.CONNECTING;
import static com.neovisionaries.ws.client.WebSocketState.CREATED;
import static com.neovisionaries.ws.client.WebSocketState.OPEN;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.RejectedExecutionException;
import com.neovisionaries.ws.client.StateManager.CloseInitiator;


/**
 * Web socket.
 *
 * <h3>Create WebSocketFactory</h3>
 *
 * <p>
 * {@link WebSocketFactory} is a factory class that creates
 * {@link WebSocket} instances. The first step is to create a
 * {@code WebSocketFactory} instance.
 * </p>
 *
 * <blockquote>
 * <pre style="border-left: solid 5px lightgray;"> <span style="color: green;">// Create a WebSocketFactory instance.</span>
 * WebSocketFactory factory = new {@link WebSocketFactory#WebSocketFactory()
 * WebSocketFactory()};</pre>
 * </blockquote>
 *
 * <p>
 * By default, {@code WebSocketFactory} uses {@link
 * javax.net.SocketFactory SocketFactory}{@code .}{@link
 * javax.net.SocketFactory#getDefault() getDefault()} for
 * non-secure WebSocket connections ({@code ws:}) and {@link
 * javax.net.ssl.SSLSocketFactory SSLSocketFactory}{@code
 * .}{@link javax.net.ssl.SSLSocketFactory#getDefault()
 * getDefault()} for secure WebSocket connections ({@code
 * wss:}). You can change this default behavior by using
 * {@code WebSocketFactory.}{@link
 * WebSocketFactory#setSocketFactory(javax.net.SocketFactory)
 * setSocketFactory} method, {@code WebSocketFactory.}{@link
 * WebSocketFactory#setSSLSocketFactory(javax.net.ssl.SSLSocketFactory)
 * setSSLSocketFactory} method and {@code WebSocketFactory.}{@link
 * WebSocketFactory#setSSLContext(javax.net.ssl.SSLContext)
 * setSSLContext} method. Note that you don't have to call a {@code
 * setSSL*} method at all if you use the default SSL configuration.
 * Also note that calling {@code setSSLSocketFactory} method has no
 * meaning if you have called {@code setSSLContext} method. See the
 * description of {@code WebSocketFactory.}{@link
 * WebSocketFactory#createSocket(URI) createSocket(URI)} method for
 * details.
 * </p>
 *
 * <p>
 * The following is an example to set a custom SSL context to a
 * {@code WebSocketFactory} instance. (Again, you don't have to call a
 * {@code setSSL*} method if you use the default SSL configuration.)
 * </p>
 *
 * <blockquote>
 * <pre style="border-left: solid 5px lightgray;"> <span style="color: green;">// Create a custom SSL context.</span>
 * SSLContext context = <a href="https://gist.github.com/TakahikoKawasaki/d07de2218b4b81bf65ac"
 * >NaiveSSLContext</a>.getInstance(<span style="color:darkred;">"TLS"</span>);
 *
 * <span style="color: green;">// Set the custom SSL context.</span>
 * factory.{@link WebSocketFactory#setSSLContext(javax.net.ssl.SSLContext)
 * setSSLContext}(context);</pre>
 * </blockquote>
 *
 * <p>
 * <a href="https://gist.github.com/TakahikoKawasaki/d07de2218b4b81bf65ac"
 * >NaiveSSLContext</a> used in the above example is a factory class to
 * create an {@link javax.net.ssl.SSLContext SSLContext} which naively
 * accepts all certificates without verification. It's enough for testing
 * purposes. When you see an error message
 * "unable to find valid certificate path to requested target" while
 * testing, try {@code NaiveSSLContext}.
 * </p>
 *
 * <h3>HTTP Proxy</h3>
 *
 * <p>
 * If a WebSocket endpoint needs to be accessed via an HTTP proxy,
 * information about the proxy server has to be set to a {@code
 * WebSocketFactory} instance before creating a {@code WebSocket}
 * instance. Proxy settings are represented by {@link ProxySettings}
 * class. A {@code WebSocketFactory} instance has an associated
 * {@code ProxySettings} instance and it can be obtained by calling
 * {@code WebSocketFactory.}{@link WebSocketFactory#getProxySettings()
 * getProxySettings()} method.
 * </p>
 *
 * <blockquote>
 * <pre style="border-left: solid 5px lightgray;"> <span style="color: green;">// Get the associated ProxySettings instance.</span>
 * {@link ProxySettings} settings = factory.{@link
 * WebSocketFactory#getProxySettings() getProxySettings()};</pre>
 * </blockquote>
 *
 * <p>
 * {@code ProxySettings} class has methods to set information about
 * a proxy server such as {@link ProxySettings#setHost(String) setHost}
 * method and {@link ProxySettings#setPort(int) setPort} method. The
 * following is an example to set a secure (<code>https</code>) proxy
 * server.
 * </p>
 *
 * <blockquote>
 * <pre style="border-left: solid 5px lightgray;"> <span style="color: green;">// Set a proxy server.</span>
 * settings.{@link ProxySettings#setServer(String)
 * setServer}(<span style="color:darkred;">"https://proxy.example.com"</span>);</pre>
 * </blockquote>
 *
 * <p>
 * If credentials are required for authentication at a proxy server,
 * {@link ProxySettings#setId(String) setId} method and {@link
 * ProxySettings#setPassword(String) setPassword} method, or
 * {@link ProxySettings#setCredentials(String, String) setCredentials}
 * method can be used to set the credentials. Note that, however,
 * the current implementation supports only Basic Authentication.
 * </p>
 *
 * <blockquote>
 * <pre style="border-left: solid 5px lightgray;"> <span style="color: green;">// Set credentials for authentication at a proxy server.</span>
 * settings.{@link ProxySettings#setCredentials(String, String)
 * setCredentials}(id, password);
 * </pre>
 * </blockquote>
 *
 * <h3>Create WebSocket</h3>
 *
 * <p>
 * {@link WebSocket} class represents a web socket. Its instances are
 * created by calling one of {@code createSocket} methods of a {@link
 * WebSocketFactory} instance. Below is the simplest example to create
 * a {@code WebSocket} instance.
 * </p>
 *
 * <blockquote>
 * <pre style="border-left: solid 5px lightgray;"> <span style="color: green;">// Create a web socket. The scheme part can be one of the following:
 * // 'ws', 'wss', 'http' and 'https' (case-insensitive). The user info
 * // part, if any, is interpreted as expected. If a raw socket failed
 * // to be created, or if HTTP proxy handshake or SSL handshake failed,
 * // an IOException is thrown.</span>
 * WebSocket ws = new {@link WebSocketFactory#WebSocketFactory()
 * WebSocketFactory()}
 *     .{@link WebSocketFactory#createSocket(String)
 * createWebSocket}(<span style="color: darkred;">"ws://localhost/endpoint"</span>);</pre>
 * </blockquote>
 *
 * <p>
 * There are two ways to set a timeout value for socket connection. The
 * first way is to call {@link WebSocketFactory#setConnectionTimeout(int)
 * setConnectionTimeout(int timeout)} method of {@code WebSocketFactory}.
 * </p>
 *
 * <blockquote>
 * <pre style="border-left: solid 5px lightgray;"> <span style="color: green;">// Create a web socket factory and set 5000 milliseconds as a timeout
 * // value for socket connection.</span>
 * WebSocketFactory factory = new WebSocketFactory().{@link
 * WebSocketFactory#setConnectionTimeout(int) setConnectionTimeout}(5000);
 *
 * <span style="color: green;">// Create a web socket. The timeout value set above is used.</span>
 * WebSocket ws = factory.{@link WebSocketFactory#createSocket(String)
 * createWebSocket}(<span style="color: darkred;">"ws://localhost/endpoint"</span>);</pre>
 * </blockquote>
 *
 * <p>
 * The other way is to give a timeout value to a {@code createSocket} method.
 * </p>
 *
 * <blockquote>
 * <pre style="border-left: solid 5px lightgray;"> <span style="color: green;">// Create a web socket factory. The timeout value remains 0.</span>
 * WebSocketFactory factory = new WebSocketFactory();
 *
 * <span style="color: green;">// Create a web socket with a socket connection timeout value.</span>
 * WebSocket ws = factory.{@link WebSocketFactory#createSocket(String, int)
 * createWebSocket}(<span style="color: darkred;">"ws://localhost/endpoint"</span>, 5000);</pre>
 * </blockquote>
 *
 * <p>
 * The timeout value is passed to {@link Socket#connect(java.net.SocketAddress, int)
 * connect}{@code (}{@link java.net.SocketAddress SocketAddress}{@code , int)}
 * method of {@link java.net.Socket}.
 * </p>
 *
 * <h3>Register Listener</h3>
 *
 * <p>
 * After creating a {@code WebSocket} instance, you should call {@link
 * #addListener(WebSocketListener)} method to register a {@link
 * WebSocketListener} that receives web socket events. {@link
 * WebSocketAdapter} is an empty implementation of {@link
 * WebSocketListener} interface.
 * </p>
 *
 * <blockquote>
 * <pre style="border-left: solid 5px lightgray;"> <span style="color: green;">// Register a listener to receive web socket events.</span>
 * ws.{@link #addListener(WebSocketListener) addListener}(new {@link
 * WebSocketAdapter#WebSocketAdapter() WebSocketAdapter()} {
 *     <span style="color: gray;">{@code @}Override</span>
 *     public void {@link WebSocketListener#onTextMessage(WebSocket, String)
 *     onTextMessage}(WebSocket websocket, String message) throws Exception {
 *         <span style="color: green;">// Received a text message.</span>
 *         ......
 *     }
 * });</pre>
 * </blockquote>
 *
 * <h3>Configure WebSocket</h3>
 *
 * <p>
 * Before starting a WebSocket <a href="https://tools.ietf.org/html/rfc6455#section-4"
 * >opening handshake</a> with the server, you can configure the web
 * socket instance by using the following methods.
 * </p>
 *
 * <blockquote>
 * <table border="1" cellpadding="5" style="border-collapse: collapse;">
 *   <caption>Methods for Configuration</caption>
 *   <thead>
 *     <tr>
 *       <th>METHOD</th>
 *       <th>DESCRIPTION</th>
 *     </tr>
 *   </thead>
 *   <tbody>
 *     <tr>
 *       <td>{@link #addProtocol(String) addProtocol}</td>
 *       <td>Adds an element to {@code Sec-WebSocket-Protocol}</td>
 *     </tr>
 *     <tr>
 *       <td>{@link #addExtension(WebSocketExtension) addExtension}</td>
 *       <td>Adds an element to {@code Sec-WebSocket-Extensions}</td>
 *     </tr>
 *     <tr>
 *       <td>{@link #addHeader(String, String) addHeader}</td>
 *       <td>Adds an arbitrary HTTP header.</td>
 *     </tr>
 *     <tr>
 *       <td>{@link #setUserInfo(String, String) setUserInfo}</td>
 *       <td>Adds {@code Authorization} header for Basic Authentication.</td>
 *     </tr>
 *     <tr>
 *       <td>{@link #getSocket() getSocket}</td>
 *       <td>Gets the underlying {@link Socket} instance to configure it.</td>
 *     </tr>
 *     <tr>
 *       <td>{@link #setExtended(boolean) setExtended}</td>
 *       <td>Disables validity checks on RSV1/RSV2/RSV3 and opcode.</td>
 *     </tr>
 *     <tr>
 *       <td>{@link #setFrameQueueSize(int) setFrameQueueSize}</td>
 *       <td>Set the size of the frame queue for <a href="#congestion_control">congestion control</a>.</td>
 *     </tr>
 *   </tbody>
 * </table>
 * </blockquote>
 *
 * <p>
 * Note that <strong>permessage-deflate</strong> extension (<a href=
 * "http://tools.ietf.org/html/rfc7692">RFC 7692</a>) has been supported
 * since version 1.17. To enable the extension, call {@link #addExtension(String)
 * addExtension} method with {@code "permessage-deflate"}.
 * </p>
 *
 * <blockquote>
 * <pre style="border-left: solid 5px lightgray;"><span style="color: green;"> // Enable "permessage-deflate" extension (RFC 7692).</span>
 * ws.{@link #addExtension(String) addExtension}({@link WebSocketExtension#PERMESSAGE_DEFLATE});</pre>
 * </blockquote>
 *
 * <p>
 * The permessage-deflate support is new and needs testing.
 * Feedback is welcome.
 * </p>
 *
 * <h3>Perform Opening Handshake</h3>
 *
 * <p>
 * By calling {@link #connect()} method, a WebSocket opening handshake
 * is performed synchronously. If an error occurred during the handshake,
 * a {@link WebSocketException} would be thrown. Instead, when the
 * handshake succeeds, the {@code connect()} implementation creates
 * threads and starts them to read and write web socket frames
 * asynchronously.
 * </p>
 *
 * <blockquote>
 * <pre style="border-left: solid 5px lightgray;"> try
 * {
 *     <span style="color: green;">// Perform an opening handshake.</span>
 *     <span style="color: green;">// This method blocks until the opening handshake is finished.</span>
 *     ws.{@link #connect()};
 * }
 * catch ({@link WebSocketException} e)
 * {
 *     <span style="color: green;">// Failed.</span>
 * }</pre>
 * </blockquote>
 *
 * <h3>Asynchronous Opening Handshake</h3>
 *
 * <p>
 * The simplest way to call {@code connect()} method asynchronously is to
 * use {@link #connectAsynchronously()} method. The implementation of the
 * method creates a thread and calls {@code connect()} method in the thread.
 * When the {@code connect()} call failed, {@link
 * WebSocketListener#onConnectError(WebSocket, WebSocketException)
 * onConnectError()} of {@code WebSocketListener} would be called. Note that
 * {@code onConnectError()} is called only when {@code connectAsynchronously()}
 * was used and the {@code connect()} call executed in the background thread
 * failed. Neither direct synchronous {@code connect()} nor
 * {@link WebSocket#connect(java.util.concurrent.ExecutorService)
 * connect(ExecutorService)} (described below) will trigger the callback method.
 * </p>
 *
 * <blockquote>
 * <pre style="border-left: solid 5px lightgray;"> <span style="color: green;">// Perform an opening handshake asynchronously.</span>
 * ws.{@link #connectAsynchronously()};
 * </pre>
 * </blockquote>
 *
 * <p>
 * Another way to call {@code connect()} method asynchronously is to use
 * {@link #connect(ExecutorService)} method. The method performs a WebSocket
 * opening handshake asynchronously using the given {@link ExecutorService}.
 * </p>
 *
 * <blockquote>
 * <pre style="border-left: solid 5px lightgray;"> <span style="color: green;">// Prepare an ExecutorService.</span>
 * {@link ExecutorService} es = {@link java.util.concurrent.Executors Executors}.{@link
 * java.util.concurrent.Executors#newSingleThreadExecutor() newSingleThreadExecutor()};
 *
 * <span style="color: green;">// Perform an opening handshake asynchronously.</span>
 * {@link Future}{@code <WebSocket>} future = ws.{@link #connect(ExecutorService) connect}(es);
 *
 * try
 * {
 *     <span style="color: green;">// Wait for the opening handshake to complete.</span>
 *     future.get();
 * }
 * catch ({@link java.util.concurrent.ExecutionException ExecutionException} e)
 * {
 *     if (e.getCause() instanceof {@link WebSocketException})
 *     {
 *         ......
 *     }
 * }</pre>
 * </blockquote>
 *
 * <p>
 * The implementation of {@code connect(ExecutorService)} method creates
 * a {@link java.util.concurrent.Callable Callable}{@code <WebSocket>}
 * instance by calling {@link #connectable()} method and passes the
 * instance to {@link ExecutorService#submit(Callable) submit(Callable)}
 * method of the given {@code ExecutorService}. What the implementation
 * of {@link Callable#call() call()} method of the {@code Callable}
 * instance does is just to call the synchronous {@code connect()}.
 * </p>
 *
 * <h3>Send Frames</h3>
 *
 * <p>
 * Web socket frames can be sent by {@link #sendFrame(WebSocketFrame)}
 * method. Other <code>send<i>Xxx</i></code> methods such as {@link
 * #sendText(String)} are aliases of {@code sendFrame} method. All of
 * the <code>send<i>Xxx</i></code> methods work asynchronously.
 * However, under some conditions, <code>send<i>Xxx</i></code> methods
 * may block. See <a href="#congestion_control">Congestion Control</a>
 * for details.
 * </p>
 *
 * <p>
 * Below
 * are some examples of <code>send<i>Xxx</i></code> methods. Note that
 * in normal cases, you don't have to call {@link #sendClose()} method
 * and {@link #sendPong()} (or their variants) explicitly because they
 * are called automatically when appropriate.
 * </p>
 *
 * <blockquote>
 * <pre style="border-left: solid 5px lightgray;"> <span style="color: green;">// Send a text frame.</span>
 * ws.{@link #sendText(String) sendText}(<span style="color: darkred;">"Hello."</span>);
 *
 * <span style="color: green;">// Send a binary frame.</span>
 * byte[] binary = ......;
 * ws.{@link #sendBinary(byte[]) sendBinary}(binary);
 *
 * <span style="color: green;">// Send a ping frame.</span>
 * ws.{@link #sendPing(String) sendPing}(<span style="color: darkred;">"Are you there?"</span>);</pre>
 * </blockquote>
 *
 * <p>
 * If you want to send fragmented frames, you have to know the details
 * of the specification (<a href="https://tools.ietf.org/html/rfc6455#section-5.4"
 * >5.4. Fragmentation</a>). Below is an example to send a text message
 * ({@code "How are you?"}) which consists of 3 fragmented frames.
 * </p>
 *
 * <blockquote>
 * <pre style="border-left: solid 5px lightgray;"> <span style="color: green;">// The first frame must be either a text frame or a binary frame.
 * // And its FIN bit must be cleared.</span>
 * WebSocketFrame firstFrame = WebSocketFrame
 *     .{@link WebSocketFrame#createTextFrame(String)
 *     createTextFrame}(<span style="color: darkred;">"How "</span>)
 *     .{@link WebSocketFrame#setFin(boolean) setFin}(false);
 *
 * <span style="color: green;">// Subsequent frames must be continuation frames. The FIN bit of
 * // all continuation frames except the last one must be cleared.
 * // Note that the FIN bit of frames returned from
 * // WebSocketFrame.createContinuationFrame methods is cleared, so
 * // the example below does not clear the FIN bit explicitly.</span>
 * WebSocketFrame secondFrame = WebSocketFrame
 *     .{@link WebSocketFrame#createContinuationFrame(String)
 *     createContinuationFrame}(<span style="color: darkred;">"are "</span>);
 *
 * <span style="color: green;">// The last frame must be a continuation frame with the FIN bit set.
 * // Note that the FIN bit of frames returned from
 * // WebSocketFrame.createContinuationFrame methods is cleared, so
 * // the FIN bit of the last frame must be set explicitly.</span>
 * WebSocketFrame lastFrame = WebSocketFrame
 *     .{@link WebSocketFrame#createContinuationFrame(String)
 *     createContinuationFrame}(<span style="color: darkred;">"you?"</span>)
 *     .{@link WebSocketFrame#setFin(boolean) setFin}(true);
 *
 * <span style="color: green;">// Send a text message which consists of 3 frames.</span>
 * ws.{@link #sendFrame(WebSocketFrame) sendFrame}(firstFrame)
 *   .{@link #sendFrame(WebSocketFrame) sendFrame}(secondFrame)
 *   .{@link #sendFrame(WebSocketFrame) sendFrame}(lastFrame);</pre>
 * </blockquote>
 *
 * <p>
 * Alternatively, the same as above can be done like this.
 * </p>
 *
 * <blockquote>
 * <pre style="border-left: solid 5px lightgray;"> <span style="color: green;">// Send a text message which consists of 3 frames.</span>
 * ws.{@link #sendText(String, boolean) sendText}(<span style="color: darkred;">"How "</span>, false)
 *   .{@link #sendContinuation(String) sendContinuation}(<span style="color: darkred;">"are "</span>)
 *   .{@link #sendContinuation(String, boolean) sendContinuation}(<span style="color: darkred;">"you?"</span>, true);</pre>
 * </blockquote>
 *
 * <h3>Send Ping/Pong Frames Periodically</h3>
 *
 * <p>
 * You can send ping frames periodically by calling {@link #setPingInterval(long)
 * setPingInterval} method with an interval in milliseconds between ping frames.
 * This method can be called both before and after {@link #connect()} method.
 * Passing zero stops the periodical sending.
 * </p>
 *
 * <blockquote>
 * <pre style="border-left: solid 5px lightgray;"> <span style="color: green;">// Send a ping per 60 seconds.</span>
 * ws.{@link #setPingInterval(long) setPingInterval}(60 * 1000);
 *
 * <span style="color: green;">// Stop the periodical sending.</span>
 * ws.{@link #setPingInterval(long) setPingInterval}(0);</pre>
 * </blockquote>
 *
 * <p>
 * Likewise, you can send pong frames periodically by calling {@link
 * #setPongInterval(long) setPongInterval} method. "<i>A Pong frame MAY be sent
 * <b>unsolicited</b>."</i> (<a href="https://tools.ietf.org/html/rfc6455#section-5.5.3"
 * >RFC 6455, 5.5.3. Pong</a>)
 * </p>
 *
 * <h3>Auto Flush</h3>
 *
 * <p>
 * By default, a frame is automatically flushed to the server immediately after
 * {@link #sendFrame(WebSocketFrame) sendFrame} method is executed. This automatic
 * flush can be disabled by calling {@link #setAutoFlush(boolean) setAutoFlush}{@code
 * (false)}.
 * </p>
 *
 * <blockquote>
 * <pre style="border-left: solid 5px lightgray;"> <span style="color: green;">// Disable auto-flush.</span>
 * ws.{@link #setAutoFlush(boolean) setAutoFlush}(false);</pre>
 * </blockquote>
 *
 * <p>
 * To flush frames manually, call {@link #flush()} method. Note that this method
 * works asynchronously.
 * </p>
 *
 * <blockquote>
 * <pre style="border-left: solid 5px lightgray;"> <span style="color: green;">// Flush frames to the server manually.</span>
 * ws.{@link #flush()};</pre>
 * </blockquote>
 *
 * <h3 id="congestion_control">Congestion Control</h3>
 *
 * <p>
 * <code>send<i>Xxx</i></code> methods queue a {@link WebSocketFrame} instance to the
 * internal queue. By default, no upper limit is imposed on the queue size, so
 * <code>send<i>Xxx</i></code> methods do not block. However, this behavior may cause
 * a problem if your WebSocket client application sends too many WebSocket frames in
 * a short time for the WebSocket server to process. In such a case, you may want
 * <code>send<i>Xxx</i></code> methods to block when many frames are queued.
 * </p>
 *
 * <p>
 * You can set an upper limit on the internal queue by calling {@link #setFrameQueueSize(int)}
 * method. As a result, if the number of frames in the queue has reached the upper limit
 * when a <code>send<i>Xxx</i></code> method is called, the method blocks until the
 * queue gets spaces. The code snippet below is an example to set 5 as the upper limit
 * of the internal frame queue.
 * </p>
 *
 * <blockquote>
 * <pre style="border-left: solid 5px lightgray;"> <span style="color: green;">// Set 5 as the frame queue size.</span>
 * ws.{@link #setFrameQueueSize(int) setFrameQueueSize}(5);</pre>
 * </blockquote>
 *
 * <p>
 * Note that under some conditions, even if the queue is full, <code>send<i>Xxx</i></code>
 * methods do not block. For example, in the case where the thread to send frames
 * ({@code WritingThread}) is going to stop or has already stopped. In addition,
 * method calls to send a <a href="https://tools.ietf.org/html/rfc6455#section-5.5"
 * >control frame</a> (e.g. {@link #sendClose()} and {@link #sendPing()}) do not block.
 * </p>
 *
 * <h3>Disconnect WebSocket</h3>
 *
 * <p>
 * Before a web socket is closed, a closing handshake is performed. A closing handshake
 * is started (1) when the server sends a close frame to the client or (2) when the
 * client sends a close frame to the server. You can start a closing handshake by calling
 * {@link #disconnect()} method (or by sending a close frame manually).
 * </p>
 *
 * <blockquote>
 * <pre style="border-left: solid 5px lightgray;"> <span style="color: green;">// Close the web socket connection.</span>
 * ws.{@link #disconnect()};</pre>
 * </blockquote>
 *
 * <p>
 * {@code disconnect()} method has some variants. If you want to change the close code
 * and the reason phrase of the close frame that this client will send to the server,
 * use a variant method such as {@link #disconnect(int, String)}. {@code disconnect()}
 * method itself is an alias of {@code disconnect(}{@link WebSocketCloseCode}{@code
 * .NORMAL, null)}.
 * </p>
 *
 * <h3>Reconnection</h3>
 *
 * <p>
 * {@code connect()} method can be called at most only once regardless of whether the
 * method succeeded or failed. If you want to re-connect to the WebSocket endpoint,
 * you have to create a new {@code WebSocket} instance again by calling one of {@code
 * createSocket} methods of a {@code WebSocketFactory}. You may find {@link #recreate()}
 * method useful if you want to create a new {@code WebSocket} instance that has the
 * same settings as the original instance. Note that, however, settings you made on
 * the raw socket of the original {@code WebSocket} instance are not copied.
 * </p>
 *
 * <blockquote>
 * <pre style="border-left: solid 5px lightgray;"> <span style="color: green;">// Create a new WebSocket instance and connect to the same endpoint.</span>
 * ws = ws.{@link #recreate()}.{@link #connect()};</pre>
 * </blockquote>
 *
 * <p>
 * There is a variant of {@code recreate()} method that takes a timeout value for
 * socket connection. If you want to use a timeout value that is different from the
 * one used when the existing {@code WebSocket} instance was created, use {@link
 * #recreate(int) recreate(int timeout)} method.
 * </p>
 *
 * <p>
 * Note that you should not trigger reconnection in {@link
 * WebSocketListener#onError(WebSocket, WebSocketException) onError()} method
 * because {@code onError()} may be called multiple times due to one error. Instead,
 * {@link WebSocketListener#onDisconnected(WebSocket, WebSocketFrame, WebSocketFrame,
 * boolean) onDisconnected()} is the right place to trigger reconnection.
 * </p>
 *
 * <h3>Error Handling</h3>
 *
 * <p>
 * {@code WebSocketListener} has some {@code onXxxError()} methods such as {@link
 * WebSocketListener#onFrameError(WebSocket, WebSocketException, WebSocketFrame)
 * onFrameError()} and {@link
 * WebSocketListener#onSendError(WebSocket, WebSocketException, WebSocketFrame)
 * onSendError()}. Among such methods, {@link
 * WebSocketListener#onError(WebSocket, WebSocketException) onError()} is a special
 * one. It is always called before any other {@code onXxxError()} is called. For
 * example, in the implementation of {@code run()} method of {@code ReadingThread},
 * {@code Throwable} is caught and {@code onError()} and {@link
 * WebSocketListener#onUnexpectedError(WebSocket, WebSocketException)
 * onUnexpectedError()} are called in this order. The following is the implementation.
 * </p>
 *
 * <blockquote>
 * <pre style="border-left: solid 5px lightgray;"> <span style="color: gray;">{@code @}Override</span>
 * public void run()
 * {
 *     try
 *     {
 *         main();
 *     }
 *     catch (Throwable t)
 *     {
 *         <span style="color: green;">// An uncaught throwable was detected in the reading thread.</span>
 *         {@link WebSocketException} cause = new WebSocketException(
 *             {@link WebSocketError}.{@link WebSocketError#UNEXPECTED_ERROR_IN_READING_THREAD UNEXPECTED_ERROR_IN_READING_THREAD},
 *             <span style="color: darkred;">"An uncaught throwable was detected in the reading thread"</span>, t);
 *
 *         <span style="color: green;">// Notify the listeners.</span>
 *         ListenerManager manager = mWebSocket.getListenerManager();
 *         manager.callOnError(cause);
 *         manager.callOnUnexpectedError(cause);
 *     }
 * }</pre>
 * </blockquote>
 *
 * <p>
 * So, you can handle all error cases in {@code onError()} method. However, note
 * that {@code onError()} may be called multiple times due to one error, so don't
 * try to trigger reconnection in {@code onError()}. Instead, {@link
 * WebSocketListener#onDisconnected(WebSocket, WebSocketFrame, WebSocketFrame, boolean)
 * onDiconnected()} is the right place to trigger reconnection.
 * </p>
 *
 * <p>
 * All {@code onXxxError()} methods receive a {@link WebSocketException} instance
 * as the second argument (the first argument is a {@code WebSocket} instance). The
 * exception class provides {@link WebSocketException#getError() getError()} method
 * which returns a {@link WebSocketError} enum entry. Entries in {@code WebSocketError}
 * enum are possible causes of errors that may occur in the implementation of this
 * library. The error causes are so granular that they can make it easy for you to
 * find the root cause when an error occurs.
 * </p>
 *
 * <p>
 * {@code Throwable}s thrown by implementations of {@code onXXX()} callback methods
 * are passed to {@link WebSocketListener#handleCallbackError(WebSocket, Throwable)
 * handleCallbackError()} of {@code WebSocketListener}.
 * </p>
 *
 * <blockquote>
 * <pre style="border-left: solid 5px lightgray;"> <span style="color: gray;">{@code @}Override</span>
 * public void {@link WebSocketListener#handleCallbackError(WebSocket, Throwable)
 * handleCallbackError}(WebSocket websocket, Throwable cause) throws Exception {
 *     <span style="color: green;">// Throwables thrown by onXxx() callback methods come here.</span>
 * }</pre>
 * </blockquote>
 *
 * @see <a href="https://tools.ietf.org/html/rfc6455">RFC 6455 (The WebSocket Protocol)</a>
 * @see <a href="https://github.com/TakahikoKawasaki/nv-websocket-client">[GitHub] nv-websocket-client</a>
 *
 * @author Takahiko Kawasaki
 */
public class WebSocket
{
    private final WebSocketFactory mWebSocketFactory;
    private final Socket mSocket;
    private final int mConnectionTimeout;
    private final StateManager mStateManager;
    private HandshakeBuilder mHandshakeBuilder;
    private final ListenerManager mListenerManager;
    private final PingSender mPingSender;
    private final PongSender mPongSender;
    private final Object mThreadsLock = new Object();
    private WebSocketInputStream mInput;
    private WebSocketOutputStream mOutput;
    private ReadingThread mReadingThread;
    private WritingThread mWritingThread;
    private Map<String, List<String>> mServerHeaders;
    private List<WebSocketExtension> mAgreedExtensions;
    private String mAgreedProtocol;
    private boolean mExtended;
    private boolean mAutoFlush = true;
    private int mFrameQueueSize;
    private boolean mOnConnectedCalled;
    private boolean mReadingThreadStarted;
    private boolean mWritingThreadStarted;
    private boolean mReadingThreadFinished;
    private boolean mWritingThreadFinished;
    private WebSocketFrame mServerCloseFrame;
    private WebSocketFrame mClientCloseFrame;
    private PerMessageCompressionExtension mPerMessageCompressionExtension;


    WebSocket(WebSocketFactory factory, boolean secure, String userInfo,
            String host, String path, Socket socket, int timeout)
    {
        mWebSocketFactory  = factory;
        mSocket            = socket;
        mConnectionTimeout = timeout;
        mStateManager      = new StateManager();
        mHandshakeBuilder  = new HandshakeBuilder(secure, userInfo, host, path);
        mListenerManager   = new ListenerManager(this);
        mPingSender        = new PingSender(this);
        mPongSender        = new PongSender(this);
    }


    /**
     * Create a new {@code WebSocket} instance that has the same settings
     * as this instance. Note that, however, settings you made on the raw
     * socket are not copied.
     *
     * <p>
     * The {@link WebSocketFactory} instance that you used to create this
     * {@code WebSocket} instance is used again.
     * </p>
     *
     * <p>
     * This method calls {@link #recreate(int)} with the timeout value that
     * was used when this instance was created. If you want to create a
     * socket connection with a different timeout value, use {@link
     * #recreate(int)} method instead.
     * </p>
     *
     * @return
     *         A new {@code WebSocket} instance.
     *
     * @throws IOException
     *         {@link WebSocketFactory#createSocket(URI)} threw an exception.
     *
     * @since 1.6
     */
    public WebSocket recreate() throws IOException
    {
        return recreate(mConnectionTimeout);
    }


    /**
     * Create a new {@code WebSocket} instance that has the same settings
     * as this instance. Note that, however, settings you made on the raw
     * socket are not copied.
     *
     * <p>
     * The {@link WebSocketFactory} instance that you used to create this
     * {@code WebSocket} instance is used again.
     * </p>
     *
     * @return
     *         A new {@code WebSocket} instance.
     *
     * @param timeout
     *         The timeout value in milliseconds for socket timeout.
     *         A timeout of zero is interpreted as an infinite timeout.
     *
     * @throws IllegalArgumentException
     *         The given timeout value is negative.
     *
     * @throws IOException
     *         {@link WebSocketFactory#createSocket(URI)} threw an exception.
     *
     * @since 1.10
     */
    public WebSocket recreate(int timeout) throws IOException
    {
        if (timeout < 0)
        {
            throw new IllegalArgumentException("The given timeout value is negative.");
        }

        WebSocket instance = mWebSocketFactory.createSocket(getURI(), timeout);

        // Copy the settings.
        instance.mHandshakeBuilder = new HandshakeBuilder(mHandshakeBuilder);
        instance.setPingInterval(getPingInterval());
        instance.setPongInterval(getPongInterval());
        instance.mExtended = mExtended;
        instance.mAutoFlush = mAutoFlush;
        instance.mFrameQueueSize = mFrameQueueSize;

        // Copy listeners.
        List<WebSocketListener> listeners = mListenerManager.getListeners();
        synchronized (listeners)
        {
            instance.addListeners(listeners);
        }

        return instance;
    }


    @Override
    protected void finalize() throws Throwable
    {
        if (isInState(CREATED))
        {
            // The raw socket needs to be closed.
            finish();
        }

        super.finalize();
    }


    /**
     * Get the current state of this web socket.
     *
     * <p>
     * The initial state is {@link WebSocketState#CREATED CREATED}.
     * When {@link #connect()} is called, the state is changed to
     * {@link WebSocketState#CONNECTING CONNECTING}, and then to
     * {@link WebSocketState#OPEN OPEN} after a successful opening
     * handshake. The state is changed to {@link
     * WebSocketState#CLOSING CLOSING} when a closing handshake
     * is started, and then to {@link WebSocketState#CLOSED CLOSED}
     * when the closing handshake finished.
     * </p>
     *
     * <p>
     * See the description of {@link WebSocketState} for details.
     * </p>
     *
     * @return
     *         The current state.
     *
     * @see WebSocketState
     */
    public WebSocketState getState()
    {
        synchronized (mStateManager)
        {
            return mStateManager.getState();
        }
    }


    /**
     * Check if the current state of this web socket is {@link
     * WebSocketState#OPEN OPEN}.
     *
     * @return
     *         {@code true} if the current state is OPEN.
     *
     * @since 1.1
     */
    public boolean isOpen()
    {
        return isInState(OPEN);
    }


    /**
     * Check if the current state is equal to the specified state.
     */
    private boolean isInState(WebSocketState state)
    {
        synchronized (mStateManager)
        {
            return (mStateManager.getState() == state);
        }
    }


    /**
     * Add a value for {@code Sec-WebSocket-Protocol}.
     *
     * @param protocol
     *         A protocol name.
     *
     * @return
     *         {@code this} object.
     *
     * @throws IllegalArgumentException
     *         The protocol name is invalid. A protocol name must be
     *         a non-empty string with characters in the range U+0021
     *         to U+007E not including separator characters.
     */
    public WebSocket addProtocol(String protocol)
    {
        mHandshakeBuilder.addProtocol(protocol);

        return this;
    }


    /**
     * Remove a protocol from {@code Sec-WebSocket-Protocol}.
     *
     * @param protocol
     *         A protocol name. {@code null} is silently ignored.
     *
     * @return
     *         {@code this} object.
     *
     * @since 1.14
     */
    public WebSocket removeProtocol(String protocol)
    {
        mHandshakeBuilder.removeProtocol(protocol);

        return this;
    }


    /**
     * Remove all protocols from {@code Sec-WebSocket-Protocol}.
     *
     * @return
     *         {@code this} object.
     *
     * @since 1.14
     */
    public WebSocket clearProtocols()
    {
        mHandshakeBuilder.clearProtocols();

        return this;
    }


    /**
     * Add a value for {@code Sec-WebSocket-Extension}.
     *
     * @param extension
     *         An extension. {@code null} is silently ignored.
     *
     * @return
     *         {@code this} object.
     */
    public WebSocket addExtension(WebSocketExtension extension)
    {
        mHandshakeBuilder.addExtension(extension);

        return this;
    }


    /**
     * Add a value for {@code Sec-WebSocket-Extension}. The input string
     * should comply with the format described in <a href=
     * "https://tools.ietf.org/html/rfc6455#section-9.1">9.1. Negotiating
     * Extensions</a> in <a href="https://tools.ietf.org/html/rfc6455"
     * >RFC 6455</a>.
     *
     * @param extension
     *         A string that represents a WebSocket extension. If it does
     *         not comply with RFC 6455, no value is added to {@code
     *         Sec-WebSocket-Extension}.
     *
     * @return
     *         {@code this} object.
     *
     * @since 1.14
     */
    public WebSocket addExtension(String extension)
    {
        mHandshakeBuilder.addExtension(extension);

        return this;
    }


    /**
     * Remove an extension from {@code Sec-WebSocket-Extension}.
     *
     * @param extension
     *         An extension to remove. {@code null} is silently ignored.
     *
     * @return
     *         {@code this} object.
     *
     * @since 1.14
     */
    public WebSocket removeExtension(WebSocketExtension extension)
    {
        mHandshakeBuilder.removeExtension(extension);

        return this;
    }


    /**
     * Remove extensions from {@code Sec-WebSocket-Extension} by
     * an extension name.
     *
     * @param name
     *         An extension name. {@code null} is silently ignored.
     *
     * @return
     *         {@code this} object.
     *
     * @since 1.14
     */
    public WebSocket removeExtensions(String name)
    {
        mHandshakeBuilder.removeExtensions(name);

        return this;
    }


    /**
     * Remove all extensions from {@code Sec-WebSocket-Extension}.
     *
     * @return
     *         {@code this} object.
     *
     * @since 1.14
     */
    public WebSocket clearExtensions()
    {
        mHandshakeBuilder.clearExtensions();

        return this;
    }


    /**
     * Add a pair of extra HTTP header.
     *
     * @param name
     *         An HTTP header name. When {@code null} or an empty
     *         string is given, no header is added.
     *
     * @param value
     *         The value of the HTTP header.
     *
     * @return
     *         {@code this} object.
     */
    public WebSocket addHeader(String name, String value)
    {
        mHandshakeBuilder.addHeader(name, value);

        return this;
    }


    /**
     * Remove pairs of extra HTTP headers.
     *
     * @param name
     *         An HTTP header name. {@code null} is silently ignored.
     *
     * @return
     *         {@code this} object.
     *
     * @since 1.14
     */
    public WebSocket removeHeaders(String name)
    {
        mHandshakeBuilder.removeHeaders(name);

        return this;
    }


    /**
     * Clear all extra HTTP headers.
     *
     * @return
     *         {@code this} object.
     *
     * @since 1.14
     */
    public WebSocket clearHeaders()
    {
        mHandshakeBuilder.clearHeaders();

        return this;
    }


    /**
     * Set the credentials to connect to the web socket endpoint.
     *
     * @param userInfo
     *         The credentials for Basic Authentication. The format
     *         should be <code><i>id</i>:<i>password</i></code>.
     *
     * @return
     *         {@code this} object.
     */
    public WebSocket setUserInfo(String userInfo)
    {
        mHandshakeBuilder.setUserInfo(userInfo);

        return this;
    }


    /**
     * Set the credentials to connect to the web socket endpoint.
     *
     * @param id
     *         The ID.
     *
     * @param password
     *         The password.
     *
     * @return
     *         {@code this} object.
     */
    public WebSocket setUserInfo(String id, String password)
    {
        mHandshakeBuilder.setUserInfo(id, password);

        return this;
    }


    /**
     * Clear the credentials to connect to the web socket endpoint.
     *
     * @return
     *         {@code this} object.
     *
     * @since 1.14
     */
    public WebSocket clearUserInfo()
    {
        mHandshakeBuilder.clearUserInfo();

        return this;
    }


    /**
     * Check if extended use of web socket frames are allowed.
     *
     * <p>
     * When extended use is allowed, values of RSV1/RSV2/RSV3 bits
     * and opcode of frames are not checked. On the other hand,
     * if not allowed (default), non-zero values for RSV1/RSV2/RSV3
     * bits and unknown opcodes cause an error. In such a case,
     * {@link WebSocketListener#onFrameError(WebSocket,
     * WebSocketException, WebSocketFrame) onFrameError} method of
     * listeners are called and the web socket is eventually closed.
     * </p>
     *
     * @return
     *         {@code true} if extended use of web socket frames
     *         are allowed.
     */
    public boolean isExtended()
    {
        return mExtended;
    }


    /**
     * Allow or disallow extended use of web socket frames.
     *
     * @param extended
     *         {@code true} to allow extended use of web socket frames.
     *
     * @return
     *         {@code this} object.
     */
    public WebSocket setExtended(boolean extended)
    {
        mExtended = extended;

        return this;
    }


    /**
     * Check if flush is performed automatically after {@link
     * #sendFrame(WebSocketFrame)} is done. The default value is
     * {@code true}.
     *
     * @return
     *         {@code true} if flush is performed automatically.
     *
     * @since 1.5
     */
    public boolean isAutoFlush()
    {
        return mAutoFlush;
    }


    /**
     * Enable or disable auto-flush of sent frames.
     *
     * @param auto
     *         {@code true} to enable auto-flush. {@code false} to
     *         disable it.
     *
     * @return
     *         {@code this} object.
     *
     * @since 1.5
     */
    public WebSocket setAutoFlush(boolean auto)
    {
        mAutoFlush = auto;

        return this;
    }


    /**
     * Flush frames to the server. Flush is performed asynchronously.
     *
     * @return
     *         {@code this} object.
     *
     * @since 1.5
     */
    public WebSocket flush()
    {
        synchronized (mStateManager)
        {
            WebSocketState state = mStateManager.getState();

            if (state != OPEN && state != CLOSING)
            {
                return this;
            }
        }

        // Get the reference to the instance of WritingThread.
        WritingThread wt = mWritingThread;

        // If and only if an instance of WritingThread is available.
        if (wt != null)
        {
            // Request flush.
            wt.queueFlush();
        }

        return this;
    }


    /**
     * Get the size of the frame queue. The default value is 0 and it means
     * there is no limit on the queue size.
     *
     * @return
     *         The size of the frame queue.
     *
     * @since 1.15
     */
    public int getFrameQueueSize()
    {
        return mFrameQueueSize;
    }


    /**
     * Set the size of the frame queue. The default value is 0 and it means
     * there is no limit on the queue size.
     *
     * <p>
     * <code>send<i>Xxx</i></code> methods queue a {@link WebSocketFrame}
     * instance to the internal queue. If the number of frames in the queue
     * has reached the upper limit (which has been set by this method) when
     * a <code>send<i>Xxx</i></code> method is called, the method blocks
     * until the queue gets spaces.
     * </p>
     *
     * <p>
     * Under some conditions, even if the queue is full, <code>send<i>Xxx</i></code>
     * methods do not block. For example, in the case where the thread to send
     * frames ({@code WritingThread}) is going to stop or has already stopped.
     * In addition, method calls to send a <a href=
     * "https://tools.ietf.org/html/rfc6455#section-5.5">control frame</a> (e.g.
     * {@link #sendClose()} and {@link #sendPing()}) do not block.
     * </p>
     *
     * @param size
     *         The queue size. 0 means no limit. Negative numbers are not allowed.
     *
     * @return
     *         {@code this} object.
     *
     * @throws IllegalArgumentException
     *         {@code size} is negative.
     *
     * @since 1.15
     */
    public WebSocket setFrameQueueSize(int size) throws IllegalArgumentException
    {
        if (size < 0)
        {
            throw new IllegalArgumentException("size must not be negative.");
        }

        mFrameQueueSize = size;

        return this;
    }


    /**
     * Get the interval of periodical
     * <a href="https://tools.ietf.org/html/rfc6455#section-5.5.2">ping</a>
     * frames.
     *
     * @return
     *         The interval in milliseconds.
     *
     * @since 1.2
     */
    public long getPingInterval()
    {
        return mPingSender.getInterval();
    }


    /**
     * Set the interval of periodical
     * <a href="https://tools.ietf.org/html/rfc6455#section-5.5.2">ping</a>
     * frames.
     *
     * <p>
     * Setting a positive number starts sending ping frames periodically.
     * Setting zero stops the periodical sending. This method can be called
     * both before and after {@link #connect()} method.
     * </p>
     *
     * @param interval
     *         The interval in milliseconds. A negative value is
     *         regarded as zero.
     *
     * @return
     *         {@code this} object.
     *
     * @since 1.2
     */
    public WebSocket setPingInterval(long interval)
    {
        mPingSender.setInterval(interval);

        return this;
    }


    /**
     * Get the interval of periodical
     * <a href="https://tools.ietf.org/html/rfc6455#section-5.5.3">pong</a>
     * frames.
     *
     * @return
     *         The interval in milliseconds.
     *
     * @since 1.2
     */
    public long getPongInterval()
    {
        return mPongSender.getInterval();
    }


    /**
     * Set the interval of periodical
     * <a href="https://tools.ietf.org/html/rfc6455#section-5.5.3">pong</a>
     * frames.
     *
     * <p>
     * Setting a positive number starts sending pong frames periodically.
     * Setting zero stops the periodical sending. This method can be called
     * both before and after {@link #connect()} method.
     * </p>
     *
     * <blockquote>
     * <dl>
     * <dt>
     * <span style="font-weight: normal;">An excerpt from <a href=
     * "https://tools.ietf.org/html/rfc6455#section-5.5.3"
     * >RFC 6455, 5.5.3. Pong</a></span>
     * </dt>
     * <dd>
     * <p><i>
     * A Pong frame MAY be sent <b>unsolicited</b>. This serves as a
     * unidirectional heartbeat.  A response to an unsolicited Pong
     * frame is not expected.
     * </i></p>
     * </dd>
     * </dl>
     * </blockquote>
     *
     * @param interval
     *         The interval in milliseconds. A negative value is
     *         regarded as zero.
     *
     * @return
     *         {@code this} object.
     *
     * @since 1.2
     */
    public WebSocket setPongInterval(long interval)
    {
        mPongSender.setInterval(interval);

        return this;
    }


    /**
     * Add a listener to receive events on this web socket.
     *
     * @param listener
     *         A listener to add.
     *
     * @return
     *         {@code this} object.
     */
    public WebSocket addListener(WebSocketListener listener)
    {
        mListenerManager.addListener(listener);

        return this;
    }


    /**
     * Add listeners.
     *
     * @param listeners
     *         Listeners to add. {@code null} is silently ignored.
     *         {@code null} elements in the list are ignored, too.
     *
     * @return
     *         {@code this} object.
     *
     * @since 1.14
     */
    public WebSocket addListeners(List<WebSocketListener> listeners)
    {
        mListenerManager.addListeners(listeners);

        return this;
    }


    /**
     * Remove a listener from this web socket.
     *
     * @param listener
     *         A listener to remove. {@code null} won't cause an error.
     *
     * @return
     *         {@code this} object.
     *
     * @since 1.13
     */
    public WebSocket removeListener(WebSocketListener listener)
    {
        mListenerManager.removeListener(listener);

        return this;
    }


    /**
     * Remove listeners.
     *
     * @param listeners
     *         Listeners to remove. {@code null} is silently ignored.
     *         {@code null} elements in the list are ignored, too.
     *
     * @return
     *         {@code this} object.
     *
     * @since 1.14
     */
    public WebSocket removeListeners(List<WebSocketListener> listeners)
    {
        mListenerManager.removeListeners(listeners);

        return this;
    }


    /**
     * Remove all the listeners from this web socket.
     *
     * @return
     *         {@code this} object.
     *
     * @since 1.13
     */
    public WebSocket clearListeners()
    {
        mListenerManager.clearListeners();

        return this;
    }


    /**
     * Get the raw socket which this web socket uses internally.
     *
     * @return
     *         The underlying {@link Socket} instance.
     */
    public Socket getSocket()
    {
        return mSocket;
    }


    /**
     * Get the URI of the web socket endpoint. The scheme part is either
     * {@code "ws"} or {@code "wss"}. The authority part is always empty.
     *
     * @return
     *         The URI of the web socket endpoint.
     *
     * @since 1.1
     */
    public URI getURI()
    {
        return mHandshakeBuilder.getURI();
    }


    /**
     * Send an opening handshake to the server, receive the response and then
     * start threads to communicate with the server.
     *
     * <p>
     * As necessary, {@link #addProtocol(String)}, {@link #addExtension(WebSocketExtension)}
     * {@link #addHeader(String, String)} should be called before you call this
     * method. It is because the parameters set by these methods are used in the
     * opening handshake.
     * </p>
     *
     * <p>
     * Also, as necessary, {@link #getSocket()} should be used to set up socket
     * parameters before you call this method. For example, you can set the
     * socket timeout like the following.
     * </p>
     *
     * <pre>
     * WebSocket websocket = ......;
     * websocket.{@link #getSocket() getSocket()}.{@link Socket#setSoTimeout(int)
     * setSoTimeout}(5000);
     * </pre>
     *
     * <p>
     * If the web socket endpoint requires Basic Authentication, you can set
     * credentials by {@link #setUserInfo(String) setUserInfo(userInfo)} or
     * {@link #setUserInfo(String, String) setUserInfo(id, password)} before
     * you call this method.
     * Note that if the URI passed to {@link WebSocketFactory}{@code
     * .createSocket} method contains the user-info part, you don't have to
     * call {@code setUserInfo} method.
     * </p>
     *
     * <p>
     * Note that this method can be called at most only once regardless of
     * whether this method succeeded or failed. If you want to re-connect to
     * the WebSocket endpoint, you have to create a new {@code WebSocket}
     * instance again by calling one of {@code createSocket} methods of a
     * {@link WebSocketFactory}. You may find {@link #recreate()} method
     * useful if you want to create a new {@code WebSocket} instance that
     * has the same settings as this instance. (But settings you made on
     * the raw socket are not copied.)
     * </p>
     *
     * @return
     *         {@code this} object.
     *
     * @throws WebSocketException
     *         <ul>
     *           <li>The current state of the web socket is not {@link
     *               WebSocketState#CREATED CREATED}
     *           <li>Connecting the server failed.
     *           <li>The opening handshake failed.
     *         </ul>
     */
    public WebSocket connect() throws WebSocketException
    {
        // Change the state to CONNECTING. If the state before
        // the change is not CREATED, an exception is thrown.
        changeStateOnConnect();

        // HTTP headers from the server.
        Map<String, List<String>> headers;

        try
        {
            // Perform WebSocket handshake.
            headers = shakeHands();
        }
        catch (WebSocketException e)
        {
            // Change the state to CLOSED.
            mStateManager.setState(CLOSED);

            // Notify the listener of the state change.
            mListenerManager.callOnStateChanged(CLOSED);

            // The handshake failed.
            throw e;
        }

        // HTTP headers in the response from the server.
        mServerHeaders = headers;

        // Extensions.
        mPerMessageCompressionExtension = findAgreedPerMessageCompressionExtension();

        // Change the state to OPEN.
        mStateManager.setState(OPEN);

        // Notify the listener of the state change.
        mListenerManager.callOnStateChanged(OPEN);

        // Start threads that communicate with the server.
        startThreads();

        return this;
    }


    /**
     * Execute {@link #connect()} asynchronously using the given {@link
     * ExecutorService}. This method is just an alias of the following.
     *
     * <blockquote>
     * <code>executorService.{@link ExecutorService#submit(Callable) submit}({@link #connectable()})</code>
     * </blockquote>
     *
     * @param executorService
     *         An {@link ExecutorService} to execute a task created by
     *         {@link #connectable()}.
     *
     * @return
     *         The value returned from {@link ExecutorService#submit(Callable)}.
     *
     * @throws NullPointerException
     *         If the given {@link ExecutorService} is {@code null}.
     *
     * @throws RejectedExecutionException
     *         If the given {@link ExecutorService} rejected the task
     *         created by {@link #connectable()}.
     *
     * @see #connectAsynchronously()
     *
     * @since 1.7
     */
    public Future<WebSocket> connect(ExecutorService executorService)
    {
        return executorService.submit(connectable());
    }


    /**
     * Get a new {@link Callable}{@code <}{@link WebSocket}{@code >} instance
     * whose {@link Callable#call() call()} method calls {@link #connect()}
     * method of this {@code WebSocket} instance.
     *
     * @return
     *         A new {@link Callable}{@code <}{@link WebSocket}{@code >} instance
     *         for asynchronous {@link #connect()}.
     *
     * @see #connect(ExecutorService)
     *
     * @since 1.7
     */
    public Callable<WebSocket> connectable()
    {
        return new Connectable(this);
    }


    /**
     * Execute {@link #connect()} asynchronously by creating a new thread and
     * calling {@code connect()} in the thread. If {@code connect()} failed,
     * {@link WebSocketListener#onConnectError(WebSocket, WebSocketException)
     * onConnectError()} method of {@link WebSocketListener} is called.
     *
     * @return
     *         {@code this} object.
     *
     * @since 1.8
     */
    public WebSocket connectAsynchronously()
    {
        new ConnectThread(this).start();

        return this;
    }


    /**
     * Disconnect the web socket.
     *
     * <p>
     * This method is an alias of {@link #disconnect(int, String)
     * disconnect}{@code (}{@link WebSocketCloseCode#NORMAL}{@code , null)}.
     * </p>
     *
     * @return
     *         {@code this} object.
     */
    public WebSocket disconnect()
    {
        return disconnect(WebSocketCloseCode.NORMAL, null);
    }


    /**
     * Disconnect the web socket.
     *
     * <p>
     * This method is an alias of {@link #disconnect(int, String)
     * disconnect}{@code (closeCode, null)}.
     * </p>
     *
     * @param closeCode
     *         The close code embedded in a <a href=
     *         "https://tools.ietf.org/html/rfc6455#section-5.5.1">close frame</a>
     *         which this WebSocket client will send to the server.
     *
     * @return
     *         {@code this} object.
     *
     * @since 1.5
     */
    public WebSocket disconnect(int closeCode)
    {
        return disconnect(closeCode, null);
    }


    /**
     * Disconnect the web socket.
     *
     * <p>
     * This method is an alias of {@link #disconnect(int, String)
     * disconnect}{@code (}{@link WebSocketCloseCode#NORMAL}{@code , reason)}.
     * </p>
     *
     * @param reason
     *         The reason embedded in a <a href=
     *         "https://tools.ietf.org/html/rfc6455#section-5.5.1">close frame</a>
     *         which this WebSocket client will send to the server. Note that
     *         the length of the bytes which represents the given reason must
     *         not exceed 125. In other words, {@code (reason.}{@link
     *         String#getBytes(String) getBytes}{@code ("UTF-8").length <= 125)}
     *         must be true.
     *
     * @return
     *         {@code this} object.
     *
     * @since 1.5
     */
    public WebSocket disconnect(String reason)
    {
        return disconnect(WebSocketCloseCode.NORMAL, reason);
    }


    /**
     * Disconnect the web socket.
     *
     * @param closeCode
     *         The close code embedded in a <a href=
     *         "https://tools.ietf.org/html/rfc6455#section-5.5.1">close frame</a>
     *         which this WebSocket client will send to the server.
     *
     * @param reason
     *         The reason embedded in a <a href=
     *         "https://tools.ietf.org/html/rfc6455#section-5.5.1">close frame</a>
     *         which this WebSocket client will send to the server. Note that
     *         the length of the bytes which represents the given reason must
     *         not exceed 125. In other words, {@code (reason.}{@link
     *         String#getBytes(String) getBytes}{@code ("UTF-8").length <= 125)}
     *         must be true.
     *
     * @return
     *         {@code this} object.
     *
     * @see WebSocketCloseCode
     *
     * @see <a href="https://tools.ietf.org/html/rfc6455#section-5.5.1">RFC 6455, 5.5.1. Close</a>
     *
     * @since 1.5
     */
    public WebSocket disconnect(int closeCode, String reason)
    {
        synchronized (mStateManager)
        {
            switch (mStateManager.getState())
            {
                case CREATED:
                    finishAsynchronously();
                    return this;

                case OPEN:
                    break;

                default:
                    // - CONNECTING
                    //     It won't happen unless the programmer dare call
                    //     open() and disconnect() in parallel.
                    //
                    // - CLOSING
                    //     A closing handshake has already been started.
                    //
                    // - CLOSED
                    //     The connection has already been closed.
                    return this;
            }

            // Change the state to CLOSING.
            mStateManager.changeToClosing(CloseInitiator.CLIENT);

            // Create a close frame.
            WebSocketFrame frame = WebSocketFrame.createCloseFrame(closeCode, reason);

            // Send the close frame to the server.
            sendFrame(frame);
        }

        // Notify the listeners of the state change.
        mListenerManager.callOnStateChanged(CLOSING);

        // Request the threads to stop.
        stopThreads();

        return this;
    }


    /**
     * Get the agreed extensions.
     *
     * <p>
     * This method works correctly only after {@link #connect()} succeeds
     * (= after the opening handshake succeeds).
     * </p>
     *
     * @return
     *         The agreed extensions.
     */
    public List<WebSocketExtension> getAgreedExtensions()
    {
        return mAgreedExtensions;
    }


    /**
     * Get the agreed protocol.
     *
     * <p>
     * This method works correctly only after {@link #connect()} succeeds
     * (= after the opening handshake succeeds).
     * </p>
     *
     * @return
     *         The agreed protocol.
     */
    public String getAgreedProtocol()
    {
        return mAgreedProtocol;
    }


    /**
     * Send a web socket frame to the server.
     *
     * <p>
     * This method just queues the given frame. Actual transmission
     * is performed asynchronously.
     * </p>
     *
     * <p>
     * When the current state of this web socket is not {@link
     * WebSocketState#OPEN OPEN}, this method does not accept
     * the frame.
     * </p>
     *
     * <p>
     * Sending a <a href="https://tools.ietf.org/html/rfc6455#section-5.5.1"
     * >close frame</a> changes the state to {@link WebSocketState#CLOSING
     * CLOSING} (if the current state is neither {@link WebSocketState#CLOSING
     * CLOSING} nor {@link WebSocketState#CLOSED CLOSED}).
     * </p>
     *
     * <p>
     * Note that the validity of the give frame is not checked.
     * For example, even if the payload length of a given frame
     * is greater than 125 and the opcode indicates that the
     * frame is a control frame, this method accepts the given
     * frame.
     * </p>
     *
     * @param frame
     *         A web socket frame to be sent to the server.
     *         If {@code null} is given, nothing is done.
     *
     * @return
     *         {@code this} object.
     */
    public WebSocket sendFrame(WebSocketFrame frame)
    {
        if (frame == null)
        {
            return this;
        }

        synchronized (mStateManager)
        {
            WebSocketState state = mStateManager.getState();

            if (state != OPEN && state != CLOSING)
            {
                return this;
            }
        }

        // The current state is either OPEN or CLOSING. Or, CLOSED.

        // Get the reference to the writing thread.
        WritingThread wt = mWritingThread;

        // If and only if an instance of WritingThread is available.
        //
        // Some applications call sendFrame() without waiting for the
        // notification of WebSocketListener.onConnected() (Issue #23),
        // and/or even after the connection is closed. That is, there
        // are chances that sendFrame() is called when mWritingThread
        // is null. So, it should be checked whether an instance of
        // WritingThread is available or not before calling queueFrame().
        if (wt != null)
        {
            // Queue the frame. Even if the current state is CLOSED,
            // queuing a frame won't be a big issue.
            wt.queueFrame(frame);
        }

        return this;
    }


    /**
     * Send a continuation frame to the server.
     *
     * <p>
     * This method is an alias of {@link #sendFrame(WebSocketFrame)
     * sendFrame}{@code (WebSocketFrame.}{@link
     * WebSocketFrame#createContinuationFrame()
     * createContinuationFrame()}{@code )}.
     * </p>
     *
     * <p>
     * Note that the FIN bit of a frame sent by this method is {@code false}.
     * If you want to set the FIN bit, use {@link #sendContinuation(boolean)
     * sendContinuation(boolean fin)} with {@code fin=true}.
     * </p>
     *
     * @return
     *         {@code this} object.
     */
    public WebSocket sendContinuation()
    {
        return sendFrame(WebSocketFrame.createContinuationFrame());
    }


    /**
     * Send a continuation frame to the server.
     *
     * <p>
     * This method is an alias of {@link #sendFrame(WebSocketFrame)
     * sendFrame}{@code (WebSocketFrame.}{@link
     * WebSocketFrame#createContinuationFrame()
     * createContinuationFrame()}{@code .}{@link
     * WebSocketFrame#setFin(boolean) setFin}{@code (fin))}.
     * </p>
     *
     * @param fin
     *         The FIN bit value.
     *
     * @return
     *         {@code this} object.
     */
    public WebSocket sendContinuation(boolean fin)
    {
        return sendFrame(WebSocketFrame.createContinuationFrame().setFin(fin));
    }


    /**
     * Send a continuation frame to the server.
     *
     * <p>
     * This method is an alias of {@link #sendFrame(WebSocketFrame)
     * sendFrame}{@code (WebSocketFrame.}{@link
     * WebSocketFrame#createContinuationFrame(String)
     * createContinuationFrame}{@code (payload))}.
     * </p>
     *
     * <p>
     * Note that the FIN bit of a frame sent by this method is {@code false}.
     * If you want to set the FIN bit, use {@link #sendContinuation(String,
     * boolean) sendContinuation(String payload, boolean fin)} with {@code
     * fin=true}.
     * </p>
     *
     * @param payload
     *         The payload of a continuation frame.
     *
     * @return
     *         {@code this} object.
     */
    public WebSocket sendContinuation(String payload)
    {
        return sendFrame(WebSocketFrame.createContinuationFrame(payload));
    }


    /**
     * Send a continuation frame to the server.
     *
     * <p>
     * This method is an alias of {@link #sendFrame(WebSocketFrame)
     * sendFrame}{@code (WebSocketFrame.}{@link
     * WebSocketFrame#createContinuationFrame(String)
     * createContinuationFrame}{@code (payload).}{@link
     * WebSocketFrame#setFin(boolean) setFin}{@code (fin))}.
     * </p>
     *
     * @param payload
     *         The payload of a continuation frame.
     *
     * @param fin
     *         The FIN bit value.
     *
     * @return
     *         {@code this} object.
     */
    public WebSocket sendContinuation(String payload, boolean fin)
    {
        return sendFrame(WebSocketFrame.createContinuationFrame(payload).setFin(fin));
    }


    /**
     * Send a continuation frame to the server.
     *
     * <p>
     * This method is an alias of {@link #sendFrame(WebSocketFrame)
     * sendFrame}{@code (WebSocketFrame.}{@link
     * WebSocketFrame#createContinuationFrame(byte[])
     * createContinuationFrame}{@code (payload))}.
     * </p>
     *
     * <p>
     * Note that the FIN bit of a frame sent by this method is {@code false}.
     * If you want to set the FIN bit, use {@link #sendContinuation(byte[],
     * boolean) sendContinuation(byte[] payload, boolean fin)} with {@code
     * fin=true}.
     * </p>
     *
     * @param payload
     *         The payload of a continuation frame.
     *
     * @return
     *         {@code this} object.
     */
    public WebSocket sendContinuation(byte[] payload)
    {
        return sendFrame(WebSocketFrame.createContinuationFrame(payload));
    }


    /**
     * Send a continuation frame to the server.
     *
     * <p>
     * This method is an alias of {@link #sendFrame(WebSocketFrame)
     * sendFrame}{@code (WebSocketFrame.}{@link
     * WebSocketFrame#createContinuationFrame(byte[])
     * createContinuationFrame}{@code (payload).}{@link
     * WebSocketFrame#setFin(boolean) setFin}{@code (fin))}.
     * </p>
     *
     * @param payload
     *         The payload of a continuation frame.
     *
     * @param fin
     *         The FIN bit value.
     *
     * @return
     *         {@code this} object.
     */
    public WebSocket sendContinuation(byte[] payload, boolean fin)
    {
        return sendFrame(WebSocketFrame.createContinuationFrame(payload).setFin(fin));
    }


    /**
     * Send a text message to the server.
     *
     * <p>
     * This method is an alias of {@link #sendFrame(WebSocketFrame)
     * sendFrame}{@code (WebSocketFrame.}{@link
     * WebSocketFrame#createTextFrame(String)
     * createTextFrame}{@code (message))}.
     * </p>
     *
     * <p>
     * If you want to send a text frame that is to be followed by
     * continuation frames, use {@link #sendText(String, boolean)
     * setText(String payload, boolean fin)} with {@code fin=false}.
     * </p>
     *
     * @param message
     *         A text message to be sent to the server.
     *
     * @return
     *         {@code this} object.
     */
    public WebSocket sendText(String message)
    {
        return sendFrame(WebSocketFrame.createTextFrame(message));
    }


    /**
     * Send a text frame to the server.
     *
     * <p>
     * This method is an alias of {@link #sendFrame(WebSocketFrame)
     * sendFrame}{@code (WebSocketFrame.}{@link
     * WebSocketFrame#createTextFrame(String)
     * createTextFrame}{@code (payload).}{@link
     * WebSocketFrame#setFin(boolean) setFin}{@code (fin))}.
     * </p>
     *
     * @param payload
     *         The payload of a text frame.
     *
     * @param fin
     *         The FIN bit value.
     *
     * @return
     *         {@code this} object.
     */
    public WebSocket sendText(String payload, boolean fin)
    {
        return sendFrame(WebSocketFrame.createTextFrame(payload).setFin(fin));
    }


    /**
     * Send a binary message to the server.
     *
     * <p>
     * This method is an alias of {@link #sendFrame(WebSocketFrame)
     * sendFrame}{@code (WebSocketFrame.}{@link
     * WebSocketFrame#createBinaryFrame(byte[])
     * createBinaryFrame}{@code (message))}.
     * </p>
     *
     * <p>
     * If you want to send a binary frame that is to be followed by
     * continuation frames, use {@link #sendBinary(byte[], boolean)
     * setBinary(byte[] payload, boolean fin)} with {@code fin=false}.
     * </p>
     *
     * @param message
     *         A binary message to be sent to the server.
     *
     * @return
     *         {@code this} object.
     */
    public WebSocket sendBinary(byte[] message)
    {
        return sendFrame(WebSocketFrame.createBinaryFrame(message));
    }


    /**
     * Send a binary frame to the server.
     *
     * <p>
     * This method is an alias of {@link #sendFrame(WebSocketFrame)
     * sendFrame}{@code (WebSocketFrame.}{@link
     * WebSocketFrame#createBinaryFrame(byte[])
     * createBinaryFrame}{@code (payload).}{@link
     * WebSocketFrame#setFin(boolean) setFin}{@code (fin))}.
     * </p>
     *
     * @param payload
     *         The payload of a binary frame.
     *
     * @param fin
     *         The FIN bit value.
     *
     * @return
     *         {@code this} object.
     */
    public WebSocket sendBinary(byte[] payload, boolean fin)
    {
        return sendFrame(WebSocketFrame.createBinaryFrame(payload).setFin(fin));
    }


    /**
     * Send a close frame to the server.
     *
     * <p>
     * This method is an alias of {@link #sendFrame(WebSocketFrame)
     * sendFrame}{@code (WebSocketFrame.}{@link
     * WebSocketFrame#createCloseFrame() createCloseFrame()}).
     * </p>
     *
     * @return
     *         {@code this} object.
     */
    public WebSocket sendClose()
    {
        return sendFrame(WebSocketFrame.createCloseFrame());
    }


    /**
     * Send a close frame to the server.
     *
     * <p>
     * This method is an alias of {@link #sendFrame(WebSocketFrame)
     * sendFrame}{@code (WebSocketFrame.}{@link
     * WebSocketFrame#createCloseFrame(int)
     * createCloseFrame}{@code (closeCode))}.
     * </p>
     *
     * @param closeCode
     *         The close code.
     *
     * @return
     *         {@code this} object.
     *
     * @see WebSocketCloseCode
     */
    public WebSocket sendClose(int closeCode)
    {
        return sendFrame(WebSocketFrame.createCloseFrame(closeCode));
    }


    /**
     * Send a close frame to the server.
     *
     * <p>
     * This method is an alias of {@link #sendFrame(WebSocketFrame)
     * sendFrame}{@code (WebSocketFrame.}{@link
     * WebSocketFrame#createCloseFrame(int, String)
     * createCloseFrame}{@code (closeCode, reason))}.
     * </p>
     *
     * @param closeCode
     *         The close code.
     *
     * @param reason
     *         The close reason.
     *         Note that a control frame's payload length must be 125 bytes or less
     *         (RFC 6455, <a href="https://tools.ietf.org/html/rfc6455#section-5.5"
     *         >5.5. Control Frames</a>).
     *
     * @return
     *         {@code this} object.
     *
     * @see WebSocketCloseCode
     */
    public WebSocket sendClose(int closeCode, String reason)
    {
        return sendFrame(WebSocketFrame.createCloseFrame(closeCode, reason));
    }


    /**
     * Send a ping frame to the server.
     *
     * <p>
     * This method is an alias of {@link #sendFrame(WebSocketFrame)
     * sendFrame}{@code (WebSocketFrame.}{@link
     * WebSocketFrame#createPingFrame() createPingFrame()}).
     * </p>
     *
     * @return
     *         {@code this} object.
     */
    public WebSocket sendPing()
    {
        return sendFrame(WebSocketFrame.createPingFrame());
    }


    /**
     * Send a ping frame to the server.
     *
     * <p>
     * This method is an alias of {@link #sendFrame(WebSocketFrame)
     * sendFrame}{@code (WebSocketFrame.}{@link
     * WebSocketFrame#createPingFrame(byte[])
     * createPingFrame}{@code (payload))}.
     * </p>
     *
     * @param payload
     *         The payload for a ping frame.
     *         Note that a control frame's payload length must be 125 bytes or less
     *         (RFC 6455, <a href="https://tools.ietf.org/html/rfc6455#section-5.5"
     *         >5.5. Control Frames</a>).
     *
     * @return
     *         {@code this} object.
     */
    public WebSocket sendPing(byte[] payload)
    {
        return sendFrame(WebSocketFrame.createPingFrame(payload));
    }


    /**
     * Send a ping frame to the server.
     *
     * <p>
     * This method is an alias of {@link #sendFrame(WebSocketFrame)
     * sendFrame}{@code (WebSocketFrame.}{@link
     * WebSocketFrame#createPingFrame(String)
     * createPingFrame}{@code (payload))}.
     * </p>
     *
     * @param payload
     *         The payload for a ping frame.
     *         Note that a control frame's payload length must be 125 bytes or less
     *         (RFC 6455, <a href="https://tools.ietf.org/html/rfc6455#section-5.5"
     *         >5.5. Control Frames</a>).
     *
     * @return
     *         {@code this} object.
     */
    public WebSocket sendPing(String payload)
    {
        return sendFrame(WebSocketFrame.createPingFrame(payload));
    }


    /**
     * Send a pong frame to the server.
     *
     * <p>
     * This method is an alias of {@link #sendFrame(WebSocketFrame)
     * sendFrame}{@code (WebSocketFrame.}{@link
     * WebSocketFrame#createPongFrame() createPongFrame()}).
     * </p>
     *
     * @return
     *         {@code this} object.
     */
    public WebSocket sendPong()
    {
        return sendFrame(WebSocketFrame.createPongFrame());
    }


    /**
     * Send a pong frame to the server.
     *
     * <p>
     * This method is an alias of {@link #sendFrame(WebSocketFrame)
     * sendFrame}{@code (WebSocketFrame.}{@link
     * WebSocketFrame#createPongFrame(byte[])
     * createPongFrame}{@code (payload))}.
     * </p>
     *
     * @param payload
     *         The payload for a pong frame.
     *         Note that a control frame's payload length must be 125 bytes or less
     *         (RFC 6455, <a href="https://tools.ietf.org/html/rfc6455#section-5.5"
     *         >5.5. Control Frames</a>).
     *
     * @return
     *         {@code this} object.
     */
    public WebSocket sendPong(byte[] payload)
    {
        return sendFrame(WebSocketFrame.createPongFrame(payload));
    }


    /**
     * Send a pong frame to the server.
     *
     * <p>
     * This method is an alias of {@link #sendFrame(WebSocketFrame)
     * sendFrame}{@code (WebSocketFrame.}{@link
     * WebSocketFrame#createPongFrame(String)
     * createPongFrame}{@code (payload))}.
     * </p>
     *
     * @param payload
     *         The payload for a pong frame.
     *         Note that a control frame's payload length must be 125 bytes or less
     *         (RFC 6455, <a href="https://tools.ietf.org/html/rfc6455#section-5.5"
     *         >5.5. Control Frames</a>).
     *
     * @return
     *         {@code this} object.
     */
    public WebSocket sendPong(String payload)
    {
        return sendFrame(WebSocketFrame.createPongFrame(payload));
    }


    private void changeStateOnConnect() throws WebSocketException
    {
        synchronized (mStateManager)
        {
            // If the current state is not CREATED.
            if (mStateManager.getState() != CREATED)
            {
                throw new WebSocketException(
                    WebSocketError.NOT_IN_CREATED_STATE,
                    "The current state of the web socket is not CREATED.");
            }

            // Change the state to CONNECTING.
            mStateManager.setState(CONNECTING);
        }

        // Notify the listeners of the state change.
        mListenerManager.callOnStateChanged(CONNECTING);
    }


    /**
     * Perform the opening handshake.
     */
    private Map<String, List<String>> shakeHands() throws WebSocketException
    {
        // The raw socket created by WebSocketFactory.
        Socket socket = mSocket;

        // Get the input stream of the socket.
        WebSocketInputStream input = openInputStream(socket);

        // Get the output stream of the socket.
        WebSocketOutputStream output = openOutputStream(socket);

        // Generate a value for Sec-WebSocket-Key.
        String key = generateWebSocketKey();

        // Send an opening handshake to the server.
        writeHandshake(output, key);

        // Read the response from the server.
        Map<String, List<String>> headers = readHandshake(input, key);

        // Keep the input stream and the output stream to pass them
        // to the reading thread and the writing thread later.
        mInput  = input;
        mOutput = output;

        // The handshake succeeded.
        return headers;
    }


    /**
     * Open the input stream of the WebSocket connection.
     * The stream is used by the reading thread.
     */
    private WebSocketInputStream openInputStream(Socket socket) throws WebSocketException
    {
        try
        {
            // Get the input stream of the raw socket through which
            // this client receives data from the server.
            return new WebSocketInputStream(
                new BufferedInputStream(socket.getInputStream()));
        }
        catch (IOException e)
        {
            // Failed to get the input stream of the raw socket.
            throw new WebSocketException(
                WebSocketError.SOCKET_INPUT_STREAM_FAILURE,
                "Failed to get the input stream of the raw socket: " + e.getMessage(), e);
        }
    }


    /**
     * Open the output stream of the WebSocket connection.
     * The stream is used by the writing thread.
     */
    private WebSocketOutputStream openOutputStream(Socket socket) throws WebSocketException
    {
        try
        {
            // Get the output stream of the socket through which
            // this client sends data to the server.
            return new WebSocketOutputStream(
                new BufferedOutputStream(socket.getOutputStream()));
        }
        catch (IOException e)
        {
            // Failed to get the output stream from the raw socket.
            throw new WebSocketException(
                WebSocketError.SOCKET_OUTPUT_STREAM_FAILURE,
                "Failed to get the output stream from the raw socket: " + e.getMessage(), e);
        }
    }


    /**
     * Generate a value for Sec-WebSocket-Key.
     *
     * <blockquote>
     * <p><i>
     * The request MUST include a header field with the name Sec-WebSocket-Key.
     * The value of this header field MUST be a nonce consisting of a randomly
     * selected 16-byte value that has been base64-encoded (see Section 4 of
     * RFC 4648). The nonce MUST be selected randomly for each connection.
     * </i></p>
     * </blockquote>
     *
     * @return
     *         A randomly generated web socket key.
     */
    private static String generateWebSocketKey()
    {
        // "16-byte value"
        byte[] data = new byte[16];

        // "randomly selected"
        Misc.nextBytes(data);

        // "base64-encoded"
        return Base64.encode(data);
    }


    /**
     * Send an opening handshake request to the WebSocket server.
     */
    private void writeHandshake(WebSocketOutputStream output, String key) throws WebSocketException
    {
        // Generate an opening handshake sent to the server from this client.
        mHandshakeBuilder.setKey(key);
        String handshake = mHandshakeBuilder.build();

        try
        {
            // Send the opening handshake to the server.
            output.write(handshake);
            output.flush();
        }
        catch (IOException e)
        {
            // Failed to send an opening handshake request to the server.
            throw new WebSocketException(
                WebSocketError.OPENING_HAHDSHAKE_REQUEST_FAILURE,
                "Failed to send an opening handshake request to the server: " + e.getMessage(), e);
        }
    }


    /**
     * Receive an opening handshake response from the WebSocket server.
     */
    private Map<String, List<String>> readHandshake(WebSocketInputStream input, String key) throws WebSocketException
    {
        return new HandshakeReader(this).readHandshake(input, key);
    }


    /**
     * Start both the reading thread and the writing thread.
     *
     * <p>
     * The reading thread will call {@link #onReadingThreadStarted()}
     * as its first step. Likewise, the writing thread will call
     * {@link #onWritingThreadStarted()} as its first step. After
     * both the threads have started, {@link #onThreadsStarted()} is
     * called.
     * </p>
     */
    private void startThreads()
    {
        ReadingThread readingThread = new ReadingThread(this);
        WritingThread writingThread = new WritingThread(this);

        synchronized (mThreadsLock)
        {
            mReadingThread = readingThread;
            mWritingThread = writingThread;
        }

        readingThread.start();
        writingThread.start();
    }


    /**
     * Stop both the reading thread and the writing thread.
     *
     * <p>
     * The reading thread will call {@link #onReadingThreadFinished(WebSocketFrame)}
     * as its last step. Likewise, the writing thread will call {@link
     * #onWritingThreadFinished(WebSocketFrame)} as its last step.
     * After both the threads have stopped, {@link #onThreadsFinished()}
     * is called.
     * </p>
     */
    private void stopThreads()
    {
        ReadingThread readingThread;
        WritingThread writingThread;

        synchronized (mThreadsLock)
        {
            readingThread = mReadingThread;
            writingThread = mWritingThread;

            mReadingThread = null;
            mWritingThread = null;
        }

        if (readingThread != null)
        {
            readingThread.requestStop();
        }

        if (writingThread != null)
        {
            writingThread.requestStop();
        }
    }


    /**
     * Get the input stream of the WebSocket connection.
     */
    WebSocketInputStream getInput()
    {
        return mInput;
    }


    /**
     * Get the output stream of the WebSocket connection.
     */
    WebSocketOutputStream getOutput()
    {
        return mOutput;
    }


    /**
     * Get the manager that manages the state of this {@code WebSocket} instance.
     */
    StateManager getStateManager()
    {
        return mStateManager;
    }


    /**
     * Get the manager that manages registered listeners.
     */
    ListenerManager getListenerManager()
    {
        return mListenerManager;
    }


    /**
     * Get the handshake builder. {@link HandshakeReader} uses this method.
     */
    HandshakeBuilder getHandshakeBuilder()
    {
        return mHandshakeBuilder;
    }


    /**
     * Set the agreed extensions. {@link HandshakeReader} uses this method.
     */
    void setAgreedExtensions(List<WebSocketExtension> extensions)
    {
        mAgreedExtensions = extensions;
    }


    /**
     * Set the agreed protocol. {@link HandshakeReader} uses this method.
     */
    void setAgreedProtocol(String protocol)
    {
        mAgreedProtocol = protocol;
    }


    /**
     * Called by the reading thread as its first step.
     */
    void onReadingThreadStarted()
    {
        synchronized (mThreadsLock)
        {
            mReadingThreadStarted = true;

            // Call onConnected() method of listeners if net called yet.
            callOnConnectedIfNotYet();

            if (mWritingThreadStarted == false)
            {
                // Wait for the writing thread to start.
                return;
            }
        }

        // Both the reading thread and the writing thread have started.
        onThreadsStarted();
    }


    /**
     * Called by the writing thread as its first step.
     */
    void onWritingThreadStarted()
    {
        synchronized (mThreadsLock)
        {
            mWritingThreadStarted = true;

            // Call onConnected() method of listeners if not called yet.
            callOnConnectedIfNotYet();

            if (mReadingThreadStarted == false)
            {
                // Wait for the reading thread to start.
                return;
            }
        }

        // Both the reading thread and the writing thread have started.
        onThreadsStarted();
    }


    /**
     * Call {@link WebSocketListener#onConnected(WebSocket, Map)} method
     * of the registered listeners if it has not been called yet. Either
     * the reading thread or the writing thread calls this method.
     */
    private void callOnConnectedIfNotYet()
    {
        // This method is called in synchronized (mThreadsLock) block.

        // If onConnected() has already been called.
        if (mOnConnectedCalled)
        {
            // Do not call onConnected() twice.
            return;
        }

        // Notify the listeners that the handshake succeeded.
        mListenerManager.callOnConnected(mServerHeaders);

        mOnConnectedCalled = true;
    }


    /**
     * Called when both the reading thread and the writing thread have started.
     * This method is called in the context of either the reading thread or
     * the writing thread.
     */
    private void onThreadsStarted()
    {
        // Start sending ping frames periodically.
        // If the interval is zero, this call does nothing.
        mPingSender.start();

        // Likewise, start the pong sender.
        mPongSender.start();
    }


    /**
     * Called by the reading thread as its last step.
     */
    void onReadingThreadFinished(WebSocketFrame closeFrame)
    {
        synchronized (mThreadsLock)
        {
            mReadingThreadFinished = true;
            mServerCloseFrame = closeFrame;

            if (mWritingThreadFinished == false)
            {
                // Wait for the writing thread to finish.
                return;
            }
        }

        // Both the reading thread and the writing thread have finished.
        onThreadsFinished();
    }


    /**
     * Called by the writing thread as its last step.
     */
    void onWritingThreadFinished(WebSocketFrame closeFrame)
    {
        synchronized (mThreadsLock)
        {
            mWritingThreadFinished = true;
            mClientCloseFrame = closeFrame;

            if (mReadingThreadFinished == false)
            {
                // Wait for the reading thread to finish.
                return;
            }
        }

        // Both the reading thread and the writing thread have finished.
        onThreadsFinished();
    }


    /**
     * Called when both the reading thread and the writing thread have finished.
     * This method is called in the context of either the reading thread or
     * the writing thread.
     */
    private void onThreadsFinished()
    {
        finish();
    }


    private void finish()
    {
        // Stop the ping sender and the pong sender.
        mPingSender.stop();
        mPongSender.stop();

        try
        {
            // Close the raw socket.
            mSocket.close();
        }
        catch (Throwable t)
        {
        }

        synchronized (mStateManager)
        {
            // Change the state to CLOSED.
            mStateManager.setState(CLOSED);
        }

        // Notify the listeners of the state change.
        mListenerManager.callOnStateChanged(CLOSED);

        // Notify the listeners that the web socket was disconnected.
        mListenerManager.callOnDisconnected(
            mServerCloseFrame, mClientCloseFrame, mStateManager.getClosedByServer());
    }


    /**
     * Call {@link #finish()} from within a separate thread.
     */
    private void finishAsynchronously()
    {
        new Thread() {
            @Override
            public void run() {
                finish();
            }
        }.start();
    }


    /**
     * Find a per-message compression extension from among the agreed extensions.
     */
    private PerMessageCompressionExtension findAgreedPerMessageCompressionExtension()
    {
        if (mAgreedExtensions == null)
        {
            return null;
        }

        for (WebSocketExtension extension : mAgreedExtensions)
        {
            if (extension instanceof PerMessageCompressionExtension)
            {
                return (PerMessageCompressionExtension)extension;
            }
        }

        return null;
    }


    /**
     * Get the PerMessageCompressionExtension in the agreed extensions.
     * This method returns null if a per-message compression extension
     * is not found in the agreed extensions.
     */
    PerMessageCompressionExtension getPerMessageCompressionExtension()
    {
        return mPerMessageCompressionExtension;
    }
}
