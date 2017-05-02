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
package uk.co.real_logic.fix_gateway.timing;

import org.HdrHistogram.Histogram;
import org.HdrHistogram.SingleWriterRecorder;
import org.agrona.concurrent.NanoClock;

import static uk.co.real_logic.fix_gateway.CommonConfiguration.TIME_MESSAGES;

public class Timer
{
    private static final int NUMBER_OF_SIGNIFICANT_VALUE_DIGITS = 3;

    // Only written to on recording thread
    private final SingleWriterRecorder recorder = new SingleWriterRecorder(NUMBER_OF_SIGNIFICANT_VALUE_DIGITS);

    private final NanoClock clock;
    private final int id;
    private final String name;
    // Only accessed upon logging thread
    private Histogram histogram;

    public Timer(final NanoClock clock, final String name, final int id)
    {
        this.clock = clock;
        this.name = name;
        this.id = id;
    }

    public long recordSince(final long timestamp)
    {
        if (TIME_MESSAGES)
        {
            final long time = clock.nanoTime();
            final long duration = time - timestamp;
            recordValue(duration);
            return time;
        }

        return 0;
    }

    void recordValue(final long duration)
    {
        recorder.recordValue(duration);
    }

    int id()
    {
        return id;
    }

    String name()
    {
        return name;
    }

    Histogram getTimings()
    {
        histogram = recorder.getIntervalHistogram(histogram);
        return histogram;
    }
}
