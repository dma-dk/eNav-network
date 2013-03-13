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
package dk.dma.enav.communication.server;

import static org.junit.Assert.assertTrue;

import java.util.concurrent.TimeUnit;

import org.junit.Test;

import dk.dma.navnet.server.ENavNetworkServer;

/**
 * 
 * @author Kasper Nielsen
 */
public class StartStopServerTest {

    @Test
    public void noStart() throws InterruptedException {
        ENavNetworkServer s = new ENavNetworkServer(12345);

        s.shutdown();
        assertTrue(s.awaitTerminated(10, TimeUnit.SECONDS));
    }

    @Test
    public void start() throws Exception {
        ENavNetworkServer s = new ENavNetworkServer(12345);

        s.start();

        s.shutdown();
        assertTrue(s.awaitTerminated(10, TimeUnit.SECONDS));
    }

    @Test
    public void start2() throws Exception {
        ENavNetworkServer s = new ENavNetworkServer(12345);

        s.start();

        s.shutdown();
        assertTrue(s.awaitTerminated(10, TimeUnit.SECONDS));
    }
}