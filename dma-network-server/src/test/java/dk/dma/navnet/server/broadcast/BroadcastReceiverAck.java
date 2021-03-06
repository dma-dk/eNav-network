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
package dk.dma.navnet.server.broadcast;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import dk.dma.enav.maritimecloud.broadcast.BroadcastOptions;
import dk.dma.enav.model.geometry.PositionTime;
import dk.dma.navnet.client.broadcast.stubs.HelloWorld;
import dk.dma.navnet.messages.auxiliary.PositionReportMessage;
import dk.dma.navnet.messages.c2c.broadcast.BroadcastAck;
import dk.dma.navnet.messages.c2c.broadcast.BroadcastDeliver;
import dk.dma.navnet.messages.c2c.broadcast.BroadcastSend;
import dk.dma.navnet.messages.c2c.broadcast.BroadcastSendAck;
import dk.dma.navnet.server.AbstractServerConnectionTest;
import dk.dma.navnet.server.TesstEndpoint;

/**
 * Test acknowledgments from receivers of broadcasts.
 * 
 * @author Kasper Nielsen
 */
public class BroadcastReceiverAck extends AbstractServerConnectionTest {

    @Test
    public void receiverAck() throws Exception {
        TesstEndpoint c1 = newClient(ID1);
        TesstEndpoint c6 = newClient(ID6);

        HelloWorld hw = new HelloWorld("foo1");
        c1.send(BroadcastSend.create(ID1, PositionTime.create(1, 1, 1), hw,
                new BroadcastOptions().setReceiverAckEnabled(true)).setReplyTo(1234));

        BroadcastDeliver bd = c6.take(BroadcastDeliver.class);
        HelloWorld tryRead = (HelloWorld) bd.tryRead();
        assertEquals("foo1", tryRead.getMessage());

        long time = System.currentTimeMillis();
        c6.send(new PositionReportMessage(4, 4, time).setLatestReceivedId(bd.getMessageId()));
        assertEquals(1234, c1.take(BroadcastSendAck.class).getMessageAck());
        Thread.sleep(100);

        BroadcastAck ba = c1.take(BroadcastAck.class);
        assertEquals(1234, ba.getBroadcastId());
        assertEquals(PositionTime.create(4, 4, time), ba.getPositionTime());
        assertEquals(ID6, ba.getId());
    }
}
