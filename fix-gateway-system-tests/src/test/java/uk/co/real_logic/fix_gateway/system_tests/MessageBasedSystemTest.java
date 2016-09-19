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
package uk.co.real_logic.fix_gateway.system_tests;

import io.aeron.driver.MediaDriver;
import org.junit.After;
import org.junit.experimental.theories.DataPoint;
import org.junit.experimental.theories.Theories;
import org.junit.experimental.theories.Theory;
import org.junit.runner.RunWith;
import uk.co.real_logic.fix_gateway.decoder.LogonDecoder;
import uk.co.real_logic.fix_gateway.decoder.LogoutDecoder;
import uk.co.real_logic.fix_gateway.engine.EngineConfiguration;
import uk.co.real_logic.fix_gateway.engine.FixEngine;

import java.io.IOException;

import static org.agrona.CloseHelper.close;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static uk.co.real_logic.fix_gateway.TestFixtures.*;
import static uk.co.real_logic.fix_gateway.system_tests.SystemTestUtil.*;

@RunWith(Theories.class)
public class MessageBasedSystemTest
{
    private int port = unusedPort();

    private MediaDriver mediaDriver;
    private FixEngine engine;

    // Trying to reproduce
    // > [8=FIX.4.4|9=0079|35=A|49=initiator|56=acceptor|34=1|52=20160825-10:25:03.931|98=0|108=30|141=Y|10=018]
    // < [8=FIX.4.4|9=0079|35=A|49=acceptor|56=initiator|34=1|52=20160825-10:24:57.920|98=0|108=30|141=N|10=013]
    // < [8=FIX.4.4|9=0070|35=2|49=acceptor|56=initiator|34=3|52=20160825-10:25:27.766|7=1|16=0|10=061]

    @DataPoint
    public static boolean on = true;
    @DataPoint
    public static boolean off = false;

    @Theory
    public void shouldComplyWIthLogonBasedSequenceNumberReset(final boolean sequenceNumberReset)
        throws IOException
    {
        setup(sequenceNumberReset);

        logonThenLogout();

        logonThenLogout();
    }

    private void setup(final boolean sequenceNumberReset)
    {
        mediaDriver = launchMediaDriver();

        delete(ACCEPTOR_LOGS);
        final EngineConfiguration config = new EngineConfiguration()
            .bindTo("localhost", port)
            .libraryAeronChannel("aeron:ipc")
            .monitoringFile(acceptorMonitoringFile("engineCounters"))
            .logFileDir(ACCEPTOR_LOGS);
        config.acceptorSequenceNumbersResetUponReconnect(sequenceNumberReset);
        engine = FixEngine.launch(config);
    }

    private void logonThenLogout() throws IOException
    {
        final FixConnection connection = new FixConnection(port);

        connection.logon(System.currentTimeMillis());

        readLogonReply(connection);

        connection.logout();

        readLogoutReply(connection);

        connection.close();
    }

    private void readLogoutReply(final FixConnection connection)
    {
        final LogoutDecoder logon = new LogoutDecoder();
        connection.readMessage(logon);

        assertFalse(logon.textAsString(), logon.hasText());
        assertTrue(logon.validate());
    }

    private void readLogonReply(final FixConnection connection)
    {
        final LogonDecoder logon = new LogonDecoder();
        connection.readMessage(logon);

        assertTrue(logon.validate());
        assertTrue(logon.resetSeqNumFlag());
    }

    @After
    public void tearDown()
    {
        close(engine);
        close(mediaDriver);
        cleanupDirectory(mediaDriver);
    }
}