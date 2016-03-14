/*
 * Copyright 2015-2016 Real Logic Ltd.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package uk.co.real_logic.fix_gateway.engine.framer;

import uk.co.real_logic.agrona.DirectBuffer;
import uk.co.real_logic.agrona.ErrorHandler;
import uk.co.real_logic.agrona.concurrent.AtomicCounter;
import uk.co.real_logic.agrona.concurrent.IdleStrategy;
import uk.co.real_logic.fix_gateway.DebugLogger;
import uk.co.real_logic.fix_gateway.ReliefValve;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;

class SenderEndPoint implements AutoCloseable
{
    private final long connectionId;
    private final int libraryId;
    private final SocketChannel channel;
    private final IdleStrategy idleStrategy;
    private final AtomicCounter messagesWrites;
    private final ErrorHandler errorHandler;
    private final Framer framer;
    private final ReliefValve reliefValve;

    SenderEndPoint(
        final long connectionId,
        final int libraryId,
        final SocketChannel channel,
        final IdleStrategy idleStrategy,
        final AtomicCounter messagesWrites,
        final ErrorHandler errorHandler,
        final Framer framer,
        final ReliefValve reliefValve)
    {
        this.connectionId = connectionId;
        this.libraryId = libraryId;
        this.channel = channel;
        this.idleStrategy = idleStrategy;
        this.messagesWrites = messagesWrites;
        this.errorHandler = errorHandler;
        this.framer = framer;
        this.reliefValve = reliefValve;
    }

    public void onFramedMessage(final DirectBuffer directBuffer, final int offset, final int length)
    {
        final ByteBuffer buffer = directBuffer.byteBuffer();
        buffer.limit(offset + length);
        buffer.position(offset);

        try
        {
            int bytesWritten = 0;
            while (bytesWritten < length)
            {
                final int written = channel.write(buffer);
                DebugLogger.log("Written  %s\n", buffer, written);
                messagesWrites.orderedIncrement();
                bytesWritten += written;
                if (written == 0)
                {
                    idleStrategy.idle(reliefValve.vent());
                }
            }
        }
        catch (final IOException ex)
        {
            errorHandler.onError(ex);
            removeEndpoint();
        }
    }

    private void removeEndpoint()
    {
        framer.onDisconnect(libraryId, connectionId, null);
    }

    public long connectionId()
    {
        return connectionId;
    }

    public int libraryId()
    {
        return libraryId;
    }

    public void close()
    {
        messagesWrites.close();
    }
}
