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

import java.util.Locale;
import org.opengis.util.InternationalString;
import org.opengis.util.NameSpace;
import org.opengis.util.GenericName;
import org.opengis.metadata.Identifier;
import org.opengis.test.Validators;
import org.apache.sis.util.iso.DefaultInternationalString;
import org.apache.sis.test.DependsOnMethod;
import org.apache.sis.test.TestCase;
import org.junit.Test;

import static org.apache.sis.test.Assert.*;
import static org.apache.sis.internal.system.DefaultFactories.SIS_NAMES;
import static org.apache.sis.metadata.iso.citation.HardCodedCitations.OGP;
import static org.apache.sis.metadata.iso.citation.HardCodedCitations.EPSG;


/**
 * Tests the {@link NamedIdentifier} class.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.4
 * @version 0.4
 * @module
 */
public final strictfp class NamedIdentifierTest extends TestCase {
    /**
     * Tests the {@link NamedIdentifier#NamedIdentifier(Citation, String, String, String, InternationalString)}
     * constructor.
     */
    @Test
    public void testCreateFromCode() {
        final NamedIdentifier identifier = new NamedIdentifier(OGP, "EPSG", "4326", "8.3", null);
        Validators.validate((Identifier)  identifier);
        Validators.validate((GenericName) identifier);

        // ImmutableIdentifier properties
        assertEquals("code",      "4326", identifier.getCode());
        assertEquals("codeSpace", "EPSG", identifier.getCodeSpace());
        assertSame  ("authority",  OGP,   identifier.getAuthority());
        assertEquals("version",   "8.3",  identifier.getVersion());
        assertNull  ("remarks",           identifier.getRemarks());
        assertFalse ("isDeprecated",      identifier.isDeprecated());

        // NamedIdentifier properties
        assertEquals("depth",  2,          identifier.depth());
        assertEquals("tip",   "4326",      identifier.tip().toString());
        assertEquals("head",  "EPSG",      identifier.head().toString());
        assertEquals("name",  "EPSG:4326", identifier.toString());

        // Scope (derived from the autority)
        final NameSpace scope = identifier.scope();
        assertFalse ("scope",        scope.isGlobal());
        assertEquals("scope", "OGP", scope.name().toString());
    }

    /**
     * Tests the {@link NamedIdentifier#NamedIdentifier(GenericName)} constructor.
     */
    @Test
    public void testCreateFromName() {
        final NameSpace scope = SIS_NAMES.createNameSpace(SIS_NAMES.createLocalName(null, "OGP"), null);
        final NamedIdentifier identifier = new NamedIdentifier(SIS_NAMES.createGenericName(scope, "EPSG", "4326"));
        Validators.validate((Identifier)  identifier);
        Validators.validate((GenericName) identifier);

        // ImmutableIdentifier properties
        assertEquals("code",      "4326", identifier.getCode());
        assertEquals("codeSpace", "EPSG", identifier.getCodeSpace());
        assertEquals("authority", "OGP",  identifier.getAuthority().getTitle().toString());
        assertNull  ("version",           identifier.getVersion());
        assertNull  ("remarks",           identifier.getRemarks());
        assertFalse ("isDeprecated",      identifier.isDeprecated());

        // NamedIdentifier properties
        assertEquals("depth",  2,          identifier.depth());
        assertEquals("tip",   "4326",      identifier.tip().toString());
        assertEquals("head",  "EPSG",      identifier.head().toString());
        assertEquals("name",  "EPSG:4326", identifier.toString());
        assertSame  ("scope", scope,       identifier.scope());
    }

    /**
     * Creates an internationalized name with a code set to "name" localized in English, French and Japanese.
     */
    private NamedIdentifier createI18N() {
        final DefaultInternationalString i18n = new DefaultInternationalString();
        i18n.add(Locale.ENGLISH,  "name");
        i18n.add(Locale.FRENCH,   "nom");
        i18n.add(Locale.JAPANESE, "名前");
        return new NamedIdentifier(EPSG, i18n);
    }

    /**
     * Tests the {@link NamedIdentifier#NamedIdentifier(Citation, InternationalString)} constructor.
     */
    @Test
    @DependsOnMethod("testCreateFromCode")
    public void testCreateFromInternationalString() {
        final NamedIdentifier identifier = createI18N();
        Validators.validate((Identifier)  identifier);
        Validators.validate((GenericName) identifier);

        // ImmutableIdentifier properties
        assertEquals("code",      "name", identifier.getCode());
        assertEquals("codeSpace", "EPSG", identifier.getCodeSpace());
        assertSame  ("authority",  EPSG,  identifier.getAuthority());
        assertNull  ("version",           identifier.getVersion());
        assertNull  ("remarks",           identifier.getRemarks());
        assertFalse ("isDeprecated",      identifier.isDeprecated());

        // NamedIdentifier properties
        assertEquals("depth",  2,          identifier.depth());
        assertEquals("tip",   "name",      identifier.tip().toInternationalString().toString(Locale.ENGLISH));
        assertEquals("tip",   "nom",       identifier.tip().toInternationalString().toString(Locale.FRENCH));
        assertEquals("tip",   "名前",       identifier.tip().toInternationalString().toString(Locale.JAPANESE));
        assertEquals("head",  "EPSG",      identifier.head().toString());
        assertEquals("name",  "EPSG:name", identifier.toInternationalString().toString(Locale.ENGLISH));
        assertEquals("name",  "EPSG:nom",  identifier.toInternationalString().toString(Locale.FRENCH));
        assertEquals("name",  "EPSG:名前",  identifier.toInternationalString().toString(Locale.JAPANESE));

        // Scope (derived from the autority)
        final NameSpace scope = identifier.scope();
        assertFalse ("scope",         scope.isGlobal());
        assertEquals("scope", "EPSG", scope.name().toString());
    }

    /**
     * Tests serialization.
     */
    @Test
    @DependsOnMethod("testCreateFromInternationalString")
    public void testSerialization() {
        NamedIdentifier unserial = assertSerializedEquals(new NamedIdentifier(EPSG, "4326"));
        assertEquals("EPSG:4326", unserial.toInternationalString().toString(Locale.ENGLISH));
        /*
         * Try again with an international string. We would not been able to get back the
         * localized strings if NamedIdentifier.writeObject/readObject(…) didn't worked.
         */
        unserial = assertSerializedEquals(createI18N());
        assertEquals("EPSG:name", unserial.toInternationalString().toString(Locale.ENGLISH));
        assertEquals("EPSG:nom",  unserial.toInternationalString().toString(Locale.FRENCH));
        assertEquals("EPSG:名前",  unserial.toInternationalString().toString(Locale.JAPANESE));
    }
}
