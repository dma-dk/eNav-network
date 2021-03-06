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
package dk.dma.navnet.client.service;

import static java.util.Objects.requireNonNull;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import dk.dma.enav.maritimecloud.service.invocation.InvocationCallback;
import dk.dma.enav.model.MaritimeId;

/**
 * The default implementation of InvocationCallback.Context.
 * 
 * @author Kasper Nielsen
 */
class DefaultLocalServiceInvocationContext<T> implements InvocationCallback.Context<T> {

    /** The logger. */
    private static final Logger LOG = LoggerFactory.getLogger(DefaultLocalServiceInvocationContext.class);

    private final MaritimeId id;

    T message;

    int errorCode;

    String errorMessage;

    boolean done;

    DefaultLocalServiceInvocationContext(MaritimeId id) {
        this.id = requireNonNull(id);
    }

    /** {@inheritDoc} */
    @Override
    public void complete(T message) {
        checkNotDone();
        this.message = message;
    }

    /** {@inheritDoc} */
    @Override
    public void failWithIllegalAccess(String errorMessage) {
        failWith(1, errorMessage);
    }

    /** {@inheritDoc} */
    @Override
    public void failWithIllegalInput(String errorMessage) {
        failWith(2, errorMessage);
    }

    /** {@inheritDoc} */
    @Override
    public void failWithInternalError(String errorMessage) {
        failWith(3, errorMessage);
    }

    private void failWith(int errorCode, String errorMessage) {
        checkNotDone();
        errorCode = 1;
        this.errorMessage = errorMessage == null ? "Unknown Error" : errorMessage;
    }

    private void checkNotDone() {
        if (done) {
            LOG.error("This context has already been used", new Exception());
        }
        done = true;
    }

    /** {@inheritDoc} */
    @Override
    public MaritimeId getCaller() {
        return id;
    }
}
