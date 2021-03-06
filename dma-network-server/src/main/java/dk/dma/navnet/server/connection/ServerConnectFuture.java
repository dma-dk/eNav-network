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
package dk.dma.navnet.server.connection;

import static java.util.Objects.requireNonNull;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import dk.dma.enav.maritimecloud.ClosingCode;
import dk.dma.enav.model.geometry.PositionTime;
import dk.dma.navnet.messages.TransportMessage;
import dk.dma.navnet.messages.auxiliary.ConnectedMessage;
import dk.dma.navnet.messages.auxiliary.HelloMessage;
import dk.dma.navnet.server.targets.Target;
import dk.dma.navnet.server.targets.TargetManager;

/**
 * 
 * @author Kasper Nielsen
 */
class ServerConnectFuture {

    /** The logger. */
    private static final Logger LOG = LoggerFactory.getLogger(ServerConnectFuture.class);

    final ServerTransport serverTransport;

    /**
     * @param serverTransport
     */
    ServerConnectFuture(ServerTransport serverTransport) {
        this.serverTransport = requireNonNull(serverTransport);
    }


    public void onMessage(HelloMessage hm) {
        System.out.println("HELLLO " + hm.getLastReceivedMessageId());

        TargetManager tm = serverTransport.cm.targetManager;
        Target target = tm.getTarget(hm.getClientId());

        // make sure we only have one connection attempt for a target at a time
        target.fullyLock();
        try {
            ServerConnection connection = target.getConnection();
            boolean isReconnect = connection != null;

            if (isReconnect) {
                ServerTransport st = connection.transport;
                if (st != null) {
                    connection.transport = null;
                    st.doClose(ClosingCode.DUPLICATE_CONNECT);
                }
                target.setLatestPosition(PositionTime.create(hm.getLat(), hm.getLon(), System.currentTimeMillis()));
            } else {
                connection = new ServerConnection(target, serverTransport.server);
                target.setLatestPosition(PositionTime.create(hm.getLat(), hm.getLon(), System.currentTimeMillis()));
                target.setConnection(connection);
            }
            connection.transport = serverTransport;

            long id = connection.worker.getLatestReceivedId();

            if (isReconnect) {

            } else {
                new Thread(connection.worker).start();
            }
            serverTransport.sendText(new ConnectedMessage(connection.id, id).toJSON());
            serverTransport.connection = connection;
            serverTransport.connectFuture = null;
            connection.worker.onConnect(serverTransport, hm.getLastReceivedMessageId(), isReconnect);
        } finally {
            target.fullyUnlock();
        }
    }

    /**
     * @param msg
     */
    public void onMessage(TransportMessage m) {
        if (m instanceof HelloMessage) {
            HelloMessage hm = (HelloMessage) m;
            onMessage(hm);
        } else {
            String err = "Expected a welcome message, but was: " + m.getClass().getSimpleName();
            LOG.error(err);
            // transport.doClose(ClosingCode.WRONG_MESSAGE.withMessage(err));
        }
    }
}
