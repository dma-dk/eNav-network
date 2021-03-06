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
package dk.dma.navnet.messages;

import java.io.IOException;

/**
 * 
 * @author Kasper Nielsen
 */
public abstract class ConnectionMessage extends TransportMessage {

    /** The id of the message. */
    private long messageId;

    /** The last message id that was received by the remote end. */
    private long latestReceivedId;

    // options
    // boolean fastack <- receiver should send some kind of ack immediatly
    /**
     * @param messageType
     */
    public ConnectionMessage(MessageType messageType) {
        super(messageType);
    }

    public ConnectionMessage(MessageType messageType, TextMessageReader pr) throws IOException {
        super(messageType);
        this.messageId = pr.takeLong();
        this.latestReceivedId = pr.takeLong();
    }

    /**
     * @return the destination
     */
    public long getLatestReceivedId() {
        return latestReceivedId;
    }

    /**
     * @return the src
     */
    public long getMessageId() {
        return messageId;
    }

    public ConnectionMessage setLatestReceivedId(long latestReceivedId) {
        this.latestReceivedId = latestReceivedId;
        return this;
    }

    public ConnectionMessage setMessageId(long messageId) {
        this.messageId = messageId;
        return this;
    }

    /** {@inheritDoc} */
    @Override
    protected final void write(TextMessageWriter w) {
        w.writeLong(messageId);
        w.writeLong(latestReceivedId);
        write0(w);
    }

    protected abstract void write0(TextMessageWriter w);
}
