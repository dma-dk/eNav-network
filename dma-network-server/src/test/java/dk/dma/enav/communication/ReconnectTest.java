/* Copyright (c) 2011 Danish Maritime Authority
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this library.  If not, see <http://www.gnu.org/licenses/>.
 */
package dk.dma.enav.communication;

import static org.junit.Assert.assertEquals;

import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.Ignore;
import org.junit.Test;

import test.stubs.BroadcastTestMessage;
import test.util.TestService;
import test.util.TestService.TestInit;
import test.util.TestService.TestReply;
import dk.dma.enav.communication.broadcast.BroadcastListener;
import dk.dma.enav.communication.broadcast.BroadcastMessageHeader;
import dk.dma.enav.communication.service.InvocationCallback;

/**
 * 
 * @author Kasper Nielsen
 */
public class ReconnectTest extends AbstractNetworkTest {

    public ReconnectTest() {
        super(true);
    }

    @Test
    @Ignore
    public void randomKilling() throws Exception {
        final AtomicInteger ai = new AtomicInteger();
        MaritimeNetworkConnection c1 = newClient(ID1);
        c1.serviceRegister(TestService.TEST_INIT,
                new InvocationCallback<TestService.TestInit, TestService.TestReply>() {
                    public void process(TestService.TestInit l, Context<TestService.TestReply> context) {
                        context.complete(l.reply());
                        ai.incrementAndGet();
                        System.out.println("Receive " + l);
                    }
                }).awaitRegistered(1, TimeUnit.SECONDS);

        MaritimeNetworkConnection c6 = newClient(ID6);

        pt.killRandom(1000, TimeUnit.MILLISECONDS);
        Map<TestInit, ConnectionFuture<TestService.TestReply>> set = new LinkedHashMap<>();
        assertEquals(2, si.getNumberOfConnections());
        for (int i = 0; i < 100; i++) {
            for (int j = 0; j < 100; j++) {
                TestService.TestInit init = new TestService.TestInit(i * 100 + j, ID6, ID1);
                set.put(init, c6.serviceInvoke(ID1, init));
                System.out.println("SEND " + init);
            }
            for (Map.Entry<TestInit, ConnectionFuture<TestService.TestReply>> f : set.entrySet()) {
                try {
                    TestReply reply = f.getValue().get(5, TimeUnit.SECONDS);
                    System.out.println("End " + reply.getInit());
                } catch (TimeoutException e) {
                    System.err.println(f.getKey());
                    throw e;
                }
            }
            set.clear();
        }

        assertEquals(100 * 100, ai.get());
        System.out.println(ai);
    }

    @Test
    @Ignore
    public void randomKilling2() throws Exception {
        final AtomicInteger ai = new AtomicInteger();
        MaritimeNetworkConnection c1 = newClient(ID1);
        c1.serviceRegister(TestService.TEST_INIT,
                new InvocationCallback<TestService.TestInit, TestService.TestReply>() {
                    public void process(TestService.TestInit l, Context<TestService.TestReply> context) {
                        context.complete(l.reply());
                        ai.incrementAndGet();
                        System.out.println("Receive " + l);
                    }
                }).awaitRegistered(1, TimeUnit.SECONDS);

        MaritimeNetworkConnection c6 = newClient(ID6);

        pt.killRandom(500, TimeUnit.MILLISECONDS);
        Map<TestInit, ConnectionFuture<TestService.TestReply>> set = new LinkedHashMap<>();
        assertEquals(2, si.getNumberOfConnections());
        for (int j = 0; j < 10; j++) {
            TestService.TestInit init = new TestService.TestInit(j, ID6, ID1);
            set.put(init, c6.serviceInvoke(ID1, init));
            System.out.println("SEND " + init);
        }
        for (Map.Entry<TestInit, ConnectionFuture<TestService.TestReply>> f : set.entrySet()) {
            try {
                TestReply reply = f.getValue().get(5, TimeUnit.SECONDS);
                System.out.println("End " + reply.getInit());
            } catch (TimeoutException e) {
                System.err.println(f.getKey());
                throw e;
            }
        }
        set.clear();

        // assertEquals(100 * 100, ai.get());
        System.out.println(ai);
    }

    @Test
    @Ignore
    public void singleClient() throws Exception {
        final AtomicInteger ai = new AtomicInteger();
        MaritimeNetworkConnection c1 = newClient(ID1);

        c1.serviceRegister(TestService.TEST_INIT,
                new InvocationCallback<TestService.TestInit, TestService.TestReply>() {
                    public void process(TestService.TestInit l, Context<TestService.TestReply> context) {
                        context.complete(l.reply());
                        ai.incrementAndGet();
                    }
                }).awaitRegistered(1, TimeUnit.SECONDS);

        MaritimeNetworkConnection c6 = newClient(ID6);

        assertEquals(2, si.getNumberOfConnections());
        for (int i = 0; i < 100; i++) {
            pt.killAll();
            TestInit ti = new TestInit(i, ID1, ID6);
            assertEquals(ti.getId(), c6.serviceInvoke(ID1, ti).get(5, TimeUnit.SECONDS).getInit().getId());
        }

        System.out.println(ai);
    }

    @Ignore
    @Test
    public void singleClient2() throws Exception {
        final int count = 10;
        final Set<Integer> received = new HashSet<>();
        final CountDownLatch cdl = new CountDownLatch(count);
        MaritimeNetworkConnection c1 = newClient(ID1);
        MaritimeNetworkConnection c2 = newClient(ID2);

        c2.broadcastListen(BroadcastTestMessage.class, new BroadcastListener<BroadcastTestMessage>() {
            public void onMessage(BroadcastMessageHeader header, BroadcastTestMessage broadcast) {
                received.add(Integer.parseInt(broadcast.getName()));
                cdl.countDown();
            }
        });

        for (int i = 1; i <= count; i++) {
            c1.broadcast(new BroadcastTestMessage("" + i));
            Thread.sleep(100);
            pt.killAll();
        }

        cdl.await(2, TimeUnit.SECONDS);
        System.out.println("Received " + received);
    }
}
