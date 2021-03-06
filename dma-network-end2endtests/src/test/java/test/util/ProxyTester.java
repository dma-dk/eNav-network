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
package test.util;

import static java.util.Objects.requireNonNull;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketAddress;
import java.util.LinkedList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

import com.google.common.io.Closeables;

/**
 * 
 * @author Kasper Nielsen
 */
public class ProxyTester {
    final LinkedList<Connection> connections = new LinkedList<>();

    final SocketAddress proxyAddress;

    final SocketAddress remoteAddress;

    final ExecutorService es = Executors.newSingleThreadExecutor();

    final ScheduledExecutorService ses = Executors.newSingleThreadScheduledExecutor();
    volatile ServerSocket ss;

    volatile CountDownLatch pause = new CountDownLatch(0);

    public ProxyTester(SocketAddress proxyAddress, SocketAddress remoteAddress) {
        this.proxyAddress = requireNonNull(proxyAddress);
        this.remoteAddress = requireNonNull(remoteAddress);
    }

    public synchronized void pause() {
        if (pause.getCount() == 0) {
            pause = new CountDownLatch(1);
        }
    }

    public void noPause() {
        pause.countDown();
    }

    public void start() throws IOException {
        ss = new ServerSocket();
        ss.bind(proxyAddress);
        System.out.println("Starting proxy");
        es.submit(new Runnable() {
            public void run() {
                for (;;) {
                    try {
                        pause.await();
                        final Socket in = ss.accept();
                        pause.await();
                        Socket out = new Socket();
                        out.connect(remoteAddress);
                        Connection con = new Connection(in, out);
                        con.inToOut.start();
                        con.outToIn.start();
                        connections.add(con);
                    } catch (Throwable t) {
                        return;
                    }
                }
            }
        });
    }

    public void killLastConnection() {
        close(connections.pollLast());
    }

    public void killFirstConnection() {
        close(connections.pollFirst());
    }

    public void killRandom() {
        close(connections.remove(ThreadLocalRandom.current().nextInt(connections.size())));
    }

    public Future<?> killRandom(long time, TimeUnit unit) {
        return ses.scheduleWithFixedDelay(new Runnable() {
            @Override
            public void run() {
                killRandom();
            }
        }, 0, time, unit);
    }

    public void killAll() {
        for (Connection c : connections) {
            close(c);
        }
        connections.clear();
    }

    @SuppressWarnings("deprecation")
    public void shutdown() throws InterruptedException {
        pause.countDown();
        ses.shutdown();
        es.shutdown();
        Closeables.closeQuietly(ss);
        killAll();
        es.awaitTermination(10, TimeUnit.SECONDS);
    }

    @SuppressWarnings("deprecation")
    private void close(Connection c) {
        if (c != null) {
            Closeables.closeQuietly(c.incoming);
            Closeables.closeQuietly(c.outgoing);
        }
    }

    static class Connection {
        final Socket incoming;
        final Socket outgoing;

        final Thread inToOut;
        final Thread outToIn;

        Connection(Socket incoming, Socket outgoing) {
            this.incoming = requireNonNull(incoming);
            this.outgoing = requireNonNull(outgoing);
            inToOut = new Thread(new Runnable() {
                public void run() {
                    inToOut();
                }
            });
            outToIn = new Thread(new Runnable() {
                public void run() {
                    outToIn();
                }
            });

        }

        void inToOut() {
            for (;;) {
                try {
                    byte[] buffer = new byte[1024]; // Adjust if you want
                    int bytesRead;
                    InputStream is = incoming.getInputStream();
                    OutputStream os = outgoing.getOutputStream();
                    while ((bytesRead = is.read(buffer)) != -1) {
                        os.write(buffer, 0, bytesRead);
                    }
                } catch (Throwable t) {
                    return;
                }
            }
        }

        void outToIn() {
            for (;;) {
                try {
                    byte[] buffer = new byte[1024]; // Adjust if you want
                    int bytesRead;
                    InputStream is = outgoing.getInputStream();
                    OutputStream os = incoming.getOutputStream();
                    while ((bytesRead = is.read(buffer)) != -1) {
                        os.write(buffer, 0, bytesRead);
                    }
                } catch (Throwable t) {
                    return;
                }
            }
        }
    }
}
