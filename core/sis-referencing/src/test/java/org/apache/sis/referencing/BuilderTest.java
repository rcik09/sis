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
package org.apache.sis.referencing;

import org.opengis.util.NameSpace;
import org.opengis.util.LocalName;
import org.opengis.util.GenericName;
import org.opengis.metadata.citation.Citation;
import org.opengis.metadata.Identifier;
import org.opengis.referencing.ReferenceIdentifier;
import org.apache.sis.metadata.iso.ImmutableIdentifier;
import org.apache.sis.test.DependsOnMethod;
import org.apache.sis.test.TestCase;
import org.junit.Test;

import static org.junit.Assert.*;
import static org.opengis.referencing.IdentifiedObject.*;
import static org.apache.sis.metadata.iso.citation.HardCodedCitations.*;
import static org.apache.sis.internal.system.DefaultFactories.SIS_NAMES;


/**
 * Tests {@link Builder}.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.4
 * @version 0.4
 * @module
 */
public final strictfp class BuilderTest extends TestCase {
    /**
     * Tests {@link Builder#verifyParameterizedType(Class)}.
     */
    @Test
    public void testVerifyParameterizedType() {
        final class Invalid extends Builder<BuilderMock> {
        }
        try {
            new Invalid();
            fail("Creation of Invalid builder shall not be allowed.");
        } catch (AssertionError e) {
            final String message = e.getMessage();
            assertTrue(message, message.contains(BuilderMock.class.getName()));
        }
    }

    /**
     * Tests {@link Builder#setCodeSpace(Citation, String)}.
     */
    @Test
    public void testSetCodeSpace() {
        final BuilderMock builder = new BuilderMock();
        builder.setCodeSpace(OGP, "EPSG");
        builder.addName("Mercator (variant A)");
        /*
         * Setting the same codespace should have no effect, while attempt to
         * set a new codespace after we added a name shall not be allowed.
         */
        builder.setCodeSpace(OGP, "EPSG");
        try {
            builder.setCodeSpace(EPSG, "EPSG");
            fail("Setting a different codespace shall not be allowed.");
        } catch (IllegalStateException e) {
            final String message = e.getMessage();
            assertTrue(message, message.contains(Identifier.AUTHORITY_KEY));
        }
        /*
         * The failed attempt to set a new codespace shall not have modified builder state.
         */
        assertEquals("EPSG", builder.properties.get(ReferenceIdentifier.CODESPACE_KEY));
        assertSame  ( OGP,   builder.properties.get(Identifier.AUTHORITY_KEY));
        /*
         * After a cleanup (normally after a createXXX(…) method call), user shall be allowed to
         * set a new codespace again. Note that the cleanup operation shall not clear the codespace.
         */
        builder.onCreate(true);
        assertEquals("EPSG", builder.properties.get(ReferenceIdentifier.CODESPACE_KEY));
        assertSame  ( OGP,   builder.properties.get(Identifier.AUTHORITY_KEY));
        builder.setCodeSpace(EPSG, "EPSG");
        assertEquals("EPSG", builder.properties.get(ReferenceIdentifier.CODESPACE_KEY));
        assertSame  ( EPSG,  builder.properties.get(Identifier.AUTHORITY_KEY));
    }

    /**
     * Tests {@link Builder#addName(CharSequence)} without codespace.
     */
    @Test
    public void testUnscopedName() {
        // Expected values to be used later in the test.
        final String    name   = "Mercator (variant A)";
        final LocalName alias1 = SIS_NAMES.createLocalName(null, "Mercator (1SP)");
        final LocalName alias2 = SIS_NAMES.createLocalName(null, "Mercator_1SP");
        final LocalName alias3 = SIS_NAMES.createLocalName(null, "CT_Mercator");
        assertEquals("Mercator (1SP)", alias1.toString());
        assertEquals("Mercator_1SP",   alias2.toString());
        assertEquals("CT_Mercator",    alias3.toString());

        // The test.
        final BuilderMock builder = new BuilderMock();
        assertSame(builder, builder.addName("Mercator (variant A)"));   // EPSG version 7.6 and later.
        assertSame(builder, builder.addName("Mercator (1SP)"));         // EPSG before version 7.6.
        assertSame(builder, builder.addName("Mercator_1SP"));           // OGC
        assertSame(builder, builder.addName("CT_Mercator"));            // GeoTIFF
        builder.onCreate(false);
        assertEquals(name, builder.properties.get(NAME_KEY));
        assertArrayEquals(new GenericName[] {alias1, alias2, alias3},
                (GenericName[]) builder.properties.get(ALIAS_KEY));
    }

    /**
     * Tests {@link Builder#addName(Citation, CharSequence)} and {@link Builder#addName(CharSequence)} with codespace.
     */
    @Test
    @DependsOnMethod({"testUnscopedName", "testSetCodeSpace"})
    public void testScopedName() {
        // Expected values to be used later in the test.
        final String      name   = "Mercator (variant A)";
        final GenericName alias1 = SIS_NAMES.createLocalName(scope("EPSG"), "Mercator (1SP)");
        final GenericName alias2 = new NamedIdentifier(OGC,     "Mercator_1SP");
        final GenericName alias3 = new NamedIdentifier(GEOTIFF, "CT_Mercator");
        assertEquals("Mercator (variant A)", name);
        assertEquals("Mercator (1SP)",       alias1.toString());
        assertEquals("OGC:Mercator_1SP",     alias2.toString());
        assertEquals("GeoTIFF:CT_Mercator",  alias3.toString());
        assertEquals("EPSG",                 alias1.scope().name().toString());

        // The test.
        final BuilderMock builder = new BuilderMock();
        assertSame(builder, builder.setCodeSpace(OGP, "EPSG"));
        assertSame(builder, builder.addName(          "Mercator (variant A)"));
        assertSame(builder, builder.addName(          "Mercator (1SP)"));
        assertSame(builder, builder.addName(OGC,      "Mercator_1SP"));
        assertSame(builder, builder.addName(GEOTIFF,  "CT_Mercator"));
        builder.onCreate(false);
        assertEquals(name, builder.properties.get(NAME_KEY));
        assertArrayEquals(new GenericName[] {alias1, alias2, alias3},
                (GenericName[]) builder.properties.get(ALIAS_KEY));
    }

    /**
     * Convenience method creating a namespace for {@link #testScopedName()} purpose.
     */
    private static NameSpace scope(final String codespace) {
        return SIS_NAMES.createNameSpace(SIS_NAMES.createLocalName(null, codespace), null);
    }

    /**
     * Tests {@link Builder#addIdentifier(Citation, String)} and {@link Builder#addIdentifier(String)}
     * with codespace.
     */
    @Test
    public void testIdentifiers() {
        // Expected values to be used later in the test.
        final Identifier id1 = new ImmutableIdentifier(OGP,     "EPSG",    "9804");
        final Identifier id2 = new ImmutableIdentifier(GEOTIFF, "GeoTIFF", "7");
        assertEquals("EPSG:9804", IdentifiedObjects.toString(id1));
        assertEquals("GeoTIFF:7", IdentifiedObjects.toString(id2));

        // The test.
        final BuilderMock builder = new BuilderMock();
        assertSame(builder, builder.setCodeSpace (OGP,  "EPSG"));
        assertSame(builder, builder.addIdentifier(      "9804"));
        assertSame(builder, builder.addIdentifier(GEOTIFF, "7"));
        builder.onCreate(false);
        assertArrayEquals(new Identifier[] {id1, id2}, (Identifier[]) builder.properties.get(IDENTIFIERS_KEY));
    }
}
