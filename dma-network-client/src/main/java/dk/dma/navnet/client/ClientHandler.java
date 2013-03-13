/*
 * Copyright (c) 2008 Kasper Nielsen.
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
package dk.dma.navnet.client;

import static java.util.Objects.requireNonNull;

import java.io.IOException;
import java.net.ConnectException;
import java.net.URI;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import org.eclipse.jetty.websocket.client.WebSocketClient;

import dk.dma.navnet.core.messages.auxiliary.ConnectedMessage;
import dk.dma.navnet.core.messages.auxiliary.HelloMessage;
import dk.dma.navnet.core.messages.auxiliary.WelcomeMessage;
import dk.dma.navnet.core.messages.c2c.broadcast.BroadcastMsg;
import dk.dma.navnet.core.messages.c2c.service.InvokeService;
import dk.dma.navnet.core.messages.c2c.service.InvokeServiceResult;
import dk.dma.navnet.core.messages.s2c.service.FindServiceResult;
import dk.dma.navnet.core.messages.s2c.service.RegisterServiceResult;
import dk.dma.navnet.core.spi.AbstractClientHandler;
import dk.dma.navnet.core.util.NetworkFutureImpl;

/**
 * 
 * @author Kasper Nielsen
 */
class ClientHandler extends AbstractClientHandler {

    /** The actual websocket client. Changes when reconnecting. */
    private volatile WebSocketClient client = new WebSocketClient();

    final ClientNetwork cm;

    final CountDownLatch connected = new CountDownLatch(1);

    long nextReplyId;

    State state = State.CREATED;

    /** The URL to connect to. */
    private final String url;

    ClientHandler(String url, ClientNetwork cm) {
        this.cm = requireNonNull(cm);
        this.url = requireNonNull(url);
    }

    /** {@inheritDoc} */
    @Override
    protected void serviceRegisteredAck(RegisterServiceResult a, NetworkFutureImpl<RegisterServiceResult> f) {
        f.complete(a);
    }

    /** {@inheritDoc} */
    @Override
    protected void serviceFindAck(FindServiceResult a, NetworkFutureImpl<FindServiceResult> f) {
        f.complete(a);
    }

    /** {@inheritDoc} */
    @Override
    protected void invokeServiceAck(InvokeServiceResult m) {
        cm.services.receiveInvokeServiceAck(m);
    }

    public void close() throws IOException {
        tryClose(4333, "Goodbye");
        try {
            client.stop();
        } catch (Exception e) {
            throw new IOException(e);
        }
    }

    public void connect(long timeout, TimeUnit unit) throws Exception {
        URI echoUri = new URI(url);
        client.start();
        try {
            client.connect(getListener(), echoUri).get();
            connected.await(timeout, unit);
            if (connected.getCount() > 0) {
                throw new ConnectException("Timedout while connecting to " + url);
            }
        } catch (ExecutionException e) {
            cm.es.shutdown();
            cm.ses.shutdown();
            client.stop();
            throw (Exception) e.getCause();// todo fix throw
        }
    }

    /** {@inheritDoc} */
    @Override
    protected void connected(ConnectedMessage m) {
        connected.countDown();
    }

    /** {@inheritDoc} */
    @Override
    protected void invokeService(InvokeService m) {
        cm.services.receiveInvokeService(m);
    }

    /** {@inheritDoc} */
    @Override
    protected void receivedBroadcast(BroadcastMsg m) {
        cm.broadcaster.receive(m);

    }

    /** {@inheritDoc} */
    @Override
    protected void welcome(WelcomeMessage m) {
        sendMessage(new HelloMessage(cm.clientId, "enavClient/1.0", "", 2));
    }

    enum State {
        CONNECTED, CREATED, DISCONNECTED
    }

}