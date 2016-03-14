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
package uk.co.real_logic.fix_gateway.library.validation;

import uk.co.real_logic.fix_gateway.decoder.Constants;
import uk.co.real_logic.fix_gateway.decoder.HeaderDecoder;
import uk.co.real_logic.fix_gateway.dictionary.CharArraySet;

import java.util.Collection;

import static uk.co.real_logic.fix_gateway.fields.RejectReason.COMPID_PROBLEM;

/**
 * A message validation strategy that checks the sender comp id of each message.
 */
public class SenderCompIdValidationStrategy implements MessageValidationStrategy
{
    private final CharArraySet validSenderIds;

    public SenderCompIdValidationStrategy(final Collection<String> validSenderIds)
    {
        this.validSenderIds = new CharArraySet(validSenderIds);
    }

    public boolean validate(final HeaderDecoder header)
    {
        final char[] senderCompID = header.senderCompID();
        final int senderCompIDLength = header.senderCompIDLength();

        return validSenderIds.contains(senderCompID, senderCompIDLength);
    }

    public int invalidTagId()
    {
        return Constants.SENDER_COMP_ID;
    }

    public int rejectReason()
    {
        return COMPID_PROBLEM.representation();
    }
}
