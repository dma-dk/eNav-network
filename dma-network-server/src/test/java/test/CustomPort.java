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
package test;

import org.junit.Test;

import dk.dma.enav.net.MaritimeNetworkConnection;
import dk.dma.enav.net.broadcast.HelloWorld;
import dk.dma.navnet.client.MaritimeNetworkConnectionBuilder;
import dk.dma.navnet.server.ENavNetworkServer;

/**
 * Tests that we can run both the server and the client on a custom port.
 * 
 * @author Kasper Nielsen
 */
public class CustomPort {

    @Test
    public void testNonDefaultPort() throws Exception {
        ENavNetworkServer server = new ENavNetworkServer(12345);
        server.start();
        MaritimeNetworkConnectionBuilder b = MaritimeNetworkConnectionBuilder.create("mmsi://1234");
        b.setHost("localhost:12345");
        try (MaritimeNetworkConnection c = b.connect()) {
            c.broadcast(new HelloWorld());
        }
        server.shutdown();
    }
}
