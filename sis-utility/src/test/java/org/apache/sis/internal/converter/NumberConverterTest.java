/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.sis.internal.converter;

import java.math.BigInteger;
import java.math.BigDecimal;
import org.apache.sis.math.FunctionProperty;
import org.apache.sis.util.ObjectConverter;
import org.apache.sis.util.UnconvertibleObjectException;
import org.apache.sis.test.TestCase;
import org.junit.Test;

import static org.apache.sis.test.Assert.*;


/**
 * Tests the various {@link NumberConverter} implementations.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.3 (derived from geotk-2.4)
 * @version 0.3
 * @module
 */
public final strictfp class NumberConverterTest extends TestCase {
    /**
     * Asserts that conversion of the given {@code source} value produces
     * the given {@code target} value, and tests the inverse conversion.
     */
    private static <S extends Number, T extends Number> void runInvertibleConversion(
            final ObjectConverter<S,T> c, final S source, final T target)
            throws UnconvertibleObjectException
    {
        assertEquals("Forward conversion.", target, c.convert(source));
        assertEquals("Inverse conversion.", source, c.inverse().convert(target));
        assertSame("Inconsistent inverse.", c, c.inverse().inverse());
        assertTrue("Invertible converters shall declare this capability.",
                c.properties().contains(FunctionProperty.INVERTIBLE));
    }

    /**
     * Tests conversions to {@link Byte} values.
     */
    @Test
    public void testByte() {
        final ObjectConverter<Integer, Byte> c =
                new NumberConverter<>(Integer.class, Byte.class).unique();
        runInvertibleConversion(c, Integer.valueOf(-8), Byte.valueOf((byte) -8));
        assertSame("Deserialization shall resolves to the singleton instance.", c, assertSerializedEquals(c));
    }

    /**
     * Tests conversions to {@link Short} values.
     */
    @Test
    public void testShort() {
        final ObjectConverter<Integer, Short> c =
                new NumberConverter<>(Integer.class, Short.class).unique();
        runInvertibleConversion(c, Integer.valueOf(-8), Short.valueOf((short) -8));
        assertSame("Deserialization shall resolves to the singleton instance.", c, assertSerializedEquals(c));
    }

    /**
     * Tests conversions to {@link Integer} values.
     */
    @Test
    public void testInteger() {
        final ObjectConverter<Float, Integer> c =
                new NumberConverter<>(Float.class, Integer.class).unique();
        runInvertibleConversion(c, Float.valueOf(-8), Integer.valueOf(-8));
        assertSame("Deserialization shall resolves to the singleton instance.", c, assertSerializedEquals(c));
    }

    /**
     * Tests conversions to {@link Long} values.
     */
    @Test
    public void testLong() {
        final ObjectConverter<Float, Long> c =
                new NumberConverter<>(Float.class, Long.class).unique();
        runInvertibleConversion(c, Float.valueOf(-8), Long.valueOf(-8));
        assertSame("Deserialization shall resolves to the singleton instance.", c, assertSerializedEquals(c));
    }

    /**
     * Tests conversions to {@link Float} values.
     */
    @Test
    public void testFloat() {
        final ObjectConverter<Double, Float> c =
                new NumberConverter<>(Double.class, Float.class).unique();
        runInvertibleConversion(c, Double.valueOf(2.5), Float.valueOf(2.5f));
        assertSame("Deserialization shall resolves to the singleton instance.", c, assertSerializedEquals(c));
    }

    /**
     * Tests conversions to {@link Double} values.
     */
    @Test
    public void testDouble() {
        final ObjectConverter<BigDecimal, Double> c =
                new NumberConverter<>(BigDecimal.class, Double.class).unique();
        runInvertibleConversion(c, BigDecimal.valueOf(2.5), Double.valueOf(2.5));
        assertSame("Deserialization shall resolves to the singleton instance.", c, assertSerializedEquals(c));
    }

    /**
     * Tests conversions to {@link BigInteger} values.
     */
    @Test
    public void testBigInteger() {
        final ObjectConverter<Double, BigInteger> c =
                new NumberConverter<>(Double.class, BigInteger.class).unique();
        runInvertibleConversion(c, Double.valueOf(1000), BigInteger.valueOf(1000));
        assertSame("Deserialization shall resolves to the singleton instance.", c, assertSerializedEquals(c));
    }

    /**
     * Tests conversions to {@link BigDecimal} values.
     */
    @Test
    public void testBigDecimal() {
        final ObjectConverter<Double, BigDecimal> c =
                new NumberConverter<>(Double.class, BigDecimal.class).unique();
        runInvertibleConversion(c, Double.valueOf(2.5), BigDecimal.valueOf(2.5));
        assertSame("Deserialization shall resolves to the singleton instance.", c, assertSerializedEquals(c));
    }

    /**
     * Tests conversions to comparable objects. Should returns the object unchanged
     * since all {@link Number} subclasses are comparable.
     */
    @Test
    public void testComparable() {
        final ObjectConverter<Number,Comparable<?>> c =
                new NumberConverter.Comparable<>(Number.class).unique();
        final Integer value = 8;
        assertSame(value, c.convert(value));
        assertSame("Deserialization shall resolves to the singleton instance.", c, assertSerializedEquals(c));
    }
}
