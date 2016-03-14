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
package uk.co.real_logic.fix_gateway.dictionary.generation;

import org.junit.BeforeClass;
import org.junit.Test;
import uk.co.real_logic.agrona.collections.IntHashSet;
import uk.co.real_logic.agrona.generation.StringWriterOutputManager;

import java.util.Map;
import java.util.Set;

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static uk.co.real_logic.agrona.generation.CompilerUtil.compileInMemory;
import static uk.co.real_logic.fix_gateway.dictionary.ExampleDictionary.*;
import static uk.co.real_logic.fix_gateway.dictionary.generation.ConstantGenerator.ALL_FIELDS;
import static uk.co.real_logic.fix_gateway.util.Reflection.getField;

public class ConstantGeneratorTest
{
    private static StringWriterOutputManager outputManager = new StringWriterOutputManager();
    private static ConstantGenerator constantGenerator =
        new ConstantGenerator(MESSAGE_EXAMPLE, TEST_PACKAGE, outputManager);

    private static Class<?> constantsClass;
    private static Object constants;

    @BeforeClass
    public static void generate() throws Exception
    {
        constantGenerator.generate();
        final Map<String, CharSequence> sources = outputManager.getSources();
        //System.out.println(sources);
        constantsClass = compileInMemory(TEST_PACKAGE + "." + ConstantGenerator.CLASS_NAME, sources);
        constants = constantsClass.newInstance();
    }

    @Test
    public void shouldContainNumericConstantsForFieldTags() throws Exception
    {
        assertEquals(TEST_REQ_ID_TAG, getField(constants, "TEST_REQ_ID"));
    }

    @Test
    public void shouldContainNumericConstantsForMessageTypes() throws Exception
    {
        assertEquals(HEARTBEAT_TYPE, getField(constants, "HEARTBEAT"));
    }

    @Test
    public void shouldEncodeAllFieldsSet() throws Exception
    {
        final Object allFieldsField = getField(constants, ALL_FIELDS);
        assertThat(allFieldsField, instanceOf(IntHashSet.class));

        @SuppressWarnings("unchecked")
        final Set<Integer> allFields = (Set<Integer>) allFieldsField;
        assertThat(allFields, hasItem(123));
        assertThat(allFields, hasItem(124));
        assertThat(allFields, hasItem(35));
        assertThat(allFields, not(hasItem(999)));
    }

}
