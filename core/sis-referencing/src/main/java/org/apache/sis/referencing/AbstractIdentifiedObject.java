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

import java.util.Map;
import java.util.Set;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.Locale;
import java.io.Serializable;
import javax.xml.bind.annotation.XmlID;
import javax.xml.bind.annotation.XmlType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;
import javax.xml.bind.annotation.adapters.CollapsedStringAdapter;
import org.opengis.util.GenericName;
import org.opengis.util.InternationalString;
import org.opengis.metadata.Identifier;
import org.opengis.metadata.citation.Citation;
import org.opengis.referencing.ObjectFactory;
import org.opengis.referencing.AuthorityFactory;
import org.opengis.referencing.IdentifiedObject;
import org.opengis.referencing.ReferenceIdentifier;
import org.apache.sis.io.wkt.FormattableObject;
import org.apache.sis.xml.Namespaces;
import org.apache.sis.util.Classes;
import org.apache.sis.util.Immutable;
import org.apache.sis.util.ThreadSafe;
import org.apache.sis.util.Deprecable;
import org.apache.sis.util.ComparisonMode;
import org.apache.sis.util.LenientComparable;
import org.apache.sis.util.iso.Types;
import org.apache.sis.util.resources.Errors;

import static org.apache.sis.util.ArgumentChecks.*;
import static org.apache.sis.internal.util.Citations.iterator;
import static org.apache.sis.internal.util.CollectionsExt.nonNull;
import static org.apache.sis.internal.util.CollectionsExt.immutableSet;


/**
 * A base class for metadata applicable to reference system objects.
 * {@code IdentifiedObject} instances are created in two main ways:
 *
 * <ul>
 *   <li>When {@link AuthorityFactory} is used to create an object, the {@linkplain ReferenceIdentifier#getAuthority()
 *       authority} and {@linkplain ReferenceIdentifier#getCode() authority code} values are set to the authority name
 *       of the factory object, and the authority code supplied by the client, respectively.</li>
 *   <li>When {@link ObjectFactory} creates an object, the {@linkplain #getName() name} is set to the value supplied
 *       by the client and all of the other metadata items are left empty.</li>
 * </ul>
 *
 * This class is conceptually <cite>abstract</cite>, even if it is technically possible to instantiate it.
 * Applications should instead instantiate the most specific subclass having a name starting by {@code Default}.
 * However exceptions to this rule may occur when it is not possible to identify the exact type.
 *
 * {@example It is sometime not possible to infer the exact coordinate system from version 1 of
 *           <a href="http://www.geoapi.org/3.0/javadoc/org/opengis/referencing/doc-files/WKT.html"><cite>Well
 *           Known Text</cite></a>, for example when parsing a <code>LOCAL_CS</code> element. In such exceptional
 *           situation, a plain <code>AbstractCS</code> object may be instantiated.}
 *
 * @author  Martin Desruisseaux (IRD, Geomatys)
 * @since   0.4 (derived from geotk-1.2)
 * @version 0.4
 * @module
 */
@Immutable
@ThreadSafe
@XmlType(name="IdentifiedObjectType", propOrder={
    "identifier",
    "name"
})
public class AbstractIdentifiedObject extends FormattableObject implements IdentifiedObject,
        LenientComparable, Deprecable, Serializable
{
    /**
     * Serial number for inter-operability with different versions.
     */
    private static final long serialVersionUID = -5173281694258483264L;

    /**
     * The name for this object or code. Should never be {@code null}.
     *
     * @see #getName()
     * @see #getIdentifier()
     */
    @XmlElement
    private final ReferenceIdentifier name;

    /**
     * An alternative name by which this object is identified.
     */
    private final Collection<GenericName> alias;

    /**
     * An identifier which references elsewhere the object's defining information.
     * Alternatively an identifier by which this object can be referenced.
     *
     * @see #getIdentifiers()
     * @see #getIdentifier()
     */
    private final Set<ReferenceIdentifier> identifiers;

    /**
     * Comments on or information about this object, or {@code null} if none.
     */
    private final InternationalString remarks;

    /**
     * The cached hash code value, or 0 if not yet computed. This field is calculated only when
     * first needed. We do not declare it {@code volatile} because it is not a big deal if this
     * field is calculated many time, and the same value should be produced by all computations.
     * The only possible outdated value is 0, which is okay.
     */
    private transient int hashCode;

    /**
     * Constructs a new identified object with the same values than the specified one.
     * This copy constructor provides a way to convert an arbitrary implementation into a SIS one or a
     * user-defined one (as a subclass), usually in order to leverage some implementation-specific API.
     *
     * <p>This constructor performs a shallow copy, i.e. the properties are not cloned.</p>
     *
     * @param object The object to shallow copy.
     */
    public AbstractIdentifiedObject(final IdentifiedObject object) {
        ensureNonNull("object", object);
        name        =         object.getName();
        alias       = nonNull(object.getAlias());
        identifiers = nonNull(object.getIdentifiers());
        remarks     =         object.getRemarks();
    }

    /**
     * Constructs an object from a set of properties. Keys are strings from the table below.
     * The map given in argument shall contain an entry at least for the
     * {@value org.opengis.referencing.IdentifiedObject#NAME_KEY} key.
     * Other properties listed in the table below are optional.
     *
     * <table class="sis">
     *   <tr>
     *     <th>Property name</th>
     *     <th>Value type</th>
     *     <th>Returned by</th>
     *   </tr>
     *   <tr>
     *     <td>{@value org.opengis.referencing.IdentifiedObject#NAME_KEY}</td>
     *     <td>{@link String} or {@link ReferenceIdentifier}</td>
     *     <td>{@link #getName()}</td>
     *   </tr>
     *   <tr>
     *     <td>{@value org.opengis.referencing.IdentifiedObject#ALIAS_KEY}</td>
     *     <td>{@link CharSequence}, {@link GenericName} or an array of those</td>
     *     <td>{@link #getAlias()}</td>
     *   </tr>
     *   <tr>
     *     <td>{@value org.opengis.metadata.Identifier#AUTHORITY_KEY}</td>
     *     <td>{@link String} or {@link Citation}</td>
     *     <td>{@link ReferenceIdentifier#getAuthority()} on the {@linkplain #getName() name}</td>
     *   </tr>
     *   <tr>
     *     <td>{@value org.opengis.referencing.ReferenceIdentifier#CODE_KEY}</td>
     *     <td>{@link String}</td>
     *     <td>{@link ReferenceIdentifier#getCode()} on the {@linkplain #getName() name}</td>
     *   </tr>
     *   <tr>
     *     <td>{@value org.opengis.referencing.ReferenceIdentifier#CODESPACE_KEY}</td>
     *     <td>{@link String}</td>
     *     <td>{@link ReferenceIdentifier#getCodeSpace()} on the {@linkplain #getName() name}</td>
     *   </tr>
     *   <tr>
     *     <td>{@value org.opengis.referencing.ReferenceIdentifier#VERSION_KEY}</td>
     *     <td>{@link String}</td>
     *     <td>{@link ReferenceIdentifier#getVersion()} on the {@linkplain #getName() name}</td>
     *   </tr>
     *   <tr>
     *     <td>{@value org.opengis.referencing.IdentifiedObject#IDENTIFIERS_KEY}</td>
     *     <td>{@link ReferenceIdentifier} or <code>{@linkplain ReferenceIdentifier}[]</code></td>
     *     <td>{@link #getIdentifiers()}</td>
     *   </tr>
     *   <tr>
     *     <td>{@value org.opengis.referencing.IdentifiedObject#REMARKS_KEY}</td>
     *     <td>{@link String} or {@link InternationalString}</td>
     *     <td>{@link #getRemarks()}</td>
     *   </tr>
     * </table>
     *
     * Additionally, all localizable attributes like {@code "remarks"} may have a language and country code suffix.
     * For example the {@code "remarks_fr"} property stands for remarks in {@linkplain Locale#FRENCH French} and
     * the {@code "remarks_fr_CA"} property stands for remarks in {@linkplain Locale#CANADA_FRENCH French Canadian}.
     *
     * <p>Note that the {@code "authority"} and {@code "version"} properties are ignored if the {@code "name"}
     * property is already a {@link ReferenceIdentifier} object instead than a {@link String}.</p>
     *
     * @param  properties The properties to be given to this identified object.
     * @throws IllegalArgumentException if a property has an invalid value.
     */
    public AbstractIdentifiedObject(final Map<String,?> properties) throws IllegalArgumentException {
        ensureNonNull("properties", properties);

        // -------------------------------------
        // "name": String or ReferenceIdentifier
        // -------------------------------------
        Object value = properties.get(NAME_KEY);
        if (value == null || value instanceof String) {
            name = new NamedIdentifier(PropertiesConverter.convert(properties));
        } else if (value instanceof ReferenceIdentifier) {
            name = (ReferenceIdentifier) value;
        } else {
            throw illegalPropertyType(NAME_KEY, value);
        }

        // -------------------------------------------------------------------
        // "alias": CharSequence, CharSequence[], GenericName or GenericName[]
        // -------------------------------------------------------------------
        value = properties.get(ALIAS_KEY);
        try {
            alias = immutableSet(Types.toGenericNames(value, null));
        } catch (ClassCastException e) {
            throw (IllegalArgumentException) illegalPropertyType(ALIAS_KEY, value).initCause(e);
        }

        // -----------------------------------------------------------
        // "identifiers": ReferenceIdentifier or ReferenceIdentifier[]
        // -----------------------------------------------------------
        value = properties.get(IDENTIFIERS_KEY);
        if (value == null) {
            identifiers = null;
        } else if (value instanceof ReferenceIdentifier) {
            identifiers = Collections.singleton((ReferenceIdentifier) value);
        } else if (value instanceof ReferenceIdentifier[]) {
            identifiers = immutableSet((ReferenceIdentifier[]) value);
        } else {
            throw illegalPropertyType(IDENTIFIERS_KEY, value);
        }

        // ----------------------------------------
        // "remarks": String or InternationalString
        // ----------------------------------------
        remarks = Types.toInternationalString(properties, REMARKS_KEY);
    }

    /**
     * Returns the exception to be thrown when a property if of illegal type.
     */
    private static IllegalArgumentException illegalPropertyType(final String key, final Object value) {
        return new IllegalArgumentException(Errors.format(Errors.Keys.IllegalPropertyClass_2, key, value.getClass()));
    }

    /**
     * The {@code gml:id}, which is mandatory. The current implementation searches for the first identifier,
     * regardless its authority. If no identifier is found, then the name is used.
     * If no name is found (which should not occur for valid objects), then this method returns {@code null}.
     *
     * <p>When an identifier has been found, this method returns the concatenation of its code space with its code,
     * <em>without separator</em>. For example this method may return {@code "EPSG4326"}, not {@code "EPSG:4326"}.</p>
     *
     * <p>The returned ID needs to be unique only in the XML document being marshalled.
     * Consecutive invocations of this method do not need to return the same value,
     * since it may depends on the marshalling context.</p>
     */
    @XmlID
    @XmlAttribute(name = "id", namespace = Namespaces.GML, required = true)
    @XmlJavaTypeAdapter(CollapsedStringAdapter.class)
    final String getID() {
        final StringBuilder id = new StringBuilder();
        /*
         * We will iterate over the identifiers first. Only after the iteration is over,
         * if we found no suitable ID, then we will use the primary name as a last resort.
         */
        Iterator<ReferenceIdentifier> it = iterator(identifiers);
        do {
            final ReferenceIdentifier identifier;
            if (it != null && it.hasNext()) {
                identifier = it.next();
            } else {
                it = null;
                identifier = name;
            }
            if (identifier != null) {
                boolean codeSpace = true;
                do { // Executed exactly twice: once for codespace, then once for code.
                    final String part = codeSpace ? identifier.getCodeSpace() : identifier.getCode();
                    if (part != null) {
                        /*
                         * Found a codespace (in the first iteration) or a code (in the second iteration).
                         * Append to the buffer only the characters that are valid for a Unicode identifier.
                         */
                        for (int i=0; i<part.length();) {
                            final int c = part.codePointAt(i);
                            if (id.length() == 0 ? Character.isUnicodeIdentifierStart(c)
                                                 : Character.isUnicodeIdentifierPart(c))
                            {
                                id.appendCodePoint(c);
                            }
                            i += Character.charCount(c);
                        }
                    }
                } while ((codeSpace = !codeSpace) == false);
                if (id.length() != 0) {
                    /*
                     * TODO: If we want to check for ID uniqueness or any other condition before to accept the ID,
                     * we would do that here. If the ID is rejected, then we just need to clear the buffer and let
                     * the iteration continue the search for an other ID.
                     */
                    return id.toString();
                }
            }
        } while (it != null);
        return null;
    }

    /**
     * Returns the primary name by which this object is identified.
     *
     * @return The primary name.
     *
     * @see Citations#getName(IdentifiedObject, Citation)
     */
    @Override
    public ReferenceIdentifier getName() {
        return name;
    }

    /**
     * Returns alternative names by which this object is identified.
     *
     * @return The aliases, or an empty collection if there is none.
     *
     * @see #getName()
     */
    @Override
    public Collection<GenericName> getAlias() {
        return nonNull(alias); // Needs to be null-safe because we may have a null value on unmarshalling.
    }

    /**
     * Returns identifiers which references elsewhere the object's defining information.
     * Alternatively identifiers by which this object can be referenced.
     *
     * @return This object identifiers, or an empty set if there is none.
     *
     * @see IdentifiedObjects#getIdentifier(IdentifiedObject, Citation)
     */
    @Override
    public Set<ReferenceIdentifier> getIdentifiers() {
        return nonNull(identifiers); // Needs to be null-safe because we may have a null value on unmarshalling.
    }

    /**
     * Returns the first identifier found, or {@code null} if none.
     * This method is invoked by JAXB at marshalling time.
     *
     * @see #name
     */
    @XmlElement(name = "identifier")
    final ReferenceIdentifier getIdentifier() {
        final Iterator<ReferenceIdentifier> it = iterator(identifiers);
        return (it != null && it.hasNext()) ? it.next() : null;
    }

    /**
     * Returns comments on or information about this object, including data source information.
     *
     * @return The remarks, or {@code null} if none.
     */
    @Override
    public InternationalString getRemarks(){
        return remarks;
    }

    /**
     * Returns {@code true} if this object is deprecated. Deprecated objects exist in some
     * {@linkplain org.opengis.referencing.AuthorityFactory authority factories} like the
     * EPSG database. Deprecated objects are usually obtained from a deprecated authority code.
     * For this reason, the default implementation applies the following rules:
     *
     * <ul>
     *   <li>If the {@linkplain #getName() name} is deprecated, then returns {@code true}.</li>
     *   <li>Otherwise if <strong>all</strong> {@linkplain #getIdentifiers() identifiers}
     *       are deprecated, ignoring the identifiers that are not instance of {@link Deprecable}
     *       (because they can not be tested), then returns {@code true}.</li>
     *   <li>Otherwise returns {@code false}.</li>
     * </ul>
     *
     * @return {@code true} if this object is deprecated.
     *
     * @see org.apache.sis.metadata.iso.ImmutableIdentifier#isDeprecated()
     */
    @Override
    public boolean isDeprecated() {
        if (name instanceof Deprecable) {
            if (((Deprecable) name).isDeprecated()) {
                return true;
            }
        }
        boolean isDeprecated = false;
        for (final ReferenceIdentifier identifier : nonNull(identifiers)) {
            if (identifier instanceof Deprecable) {
                if (!((Deprecable) identifier).isDeprecated()) {
                    return false;
                }
                isDeprecated = true;
            }
        }
        return isDeprecated;
    }

    /**
     * Returns {@code true} if either the {@linkplain #getName() primary name} or at least
     * one {@linkplain #getAlias alias} matches the specified string.
     * This method performs the search in the following order, regardless of any authority:
     *
     * <ul>
     *   <li>The {@linkplain #getName() primary name} of this object</li>
     *   <li>The {@linkplain ScopedName fully qualified name} of an alias</li>
     *   <li>The {@linkplain LocalName local name} of an alias</li>
     * </ul>
     *
     * @param  name The name to compare.
     * @return {@code true} if the primary name of at least one alias matches the specified {@code name}.
     *
     * @see IdentifiedObjects#nameMatches(IdentifiedObject, String)
     */
    public boolean nameMatches(final String name) {
        return IdentifiedObjects.nameMatches(this, alias, name);
    }

    /**
     * Compares the specified object with this object for equality.
     * This method is implemented as below (omitting assertions):
     *
     * {@preformat java
     *     return equals(other, ComparisonMode.STRICT);
     * }
     *
     * @param  object The other object (may be {@code null}).
     * @return {@code true} if both objects are equal.
     */
    @Override
    public final boolean equals(final Object object) {
        final boolean eq = equals(object, ComparisonMode.STRICT);
        // If objects are equal, then they must have the same hash code value.
        assert !eq || hashCode() == object.hashCode() : this;
        return eq;
    }

    @Override
    public boolean equals(Object other, ComparisonMode mode) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    /**
     * Returns a hash value for this identified object.
     * This method invokes {@link #computeHashCode()} when first needed and caches the value for future invocations.
     * Subclasses shall override {@code computeHashCode()} instead than this method.
     *
     * {@section Implementation specific feature}
     * In the Apache SIS implementation, the {@linkplain #getName() name}, {@linkplain #getIdentifiers() identifiers}
     * and {@linkplain #getRemarks() remarks} are not used for hash code computation.
     * Consequently two identified objects will return the same hash value if they are equal in the sense of
     * <code>{@linkplain #equals(Object, ComparisonMode) equals}(…, {@linkplain ComparisonMode#IGNORE_METADATA})</code>.
     * This feature allows users to implement metadata-insensitive {@link java.util.HashMap}.
     *
     * @return The hash code value. This value may change between different execution of the Apache SIS library.
     */
    @Override
    public final int hashCode() { // No need to synchronize; ok if invoked twice.
        int hash = hashCode;
        if (hash == 0) {
            hash = computeHashCode();
            if (hash == 0) {
                hash = -1;
            }
            hashCode = hash;
        }
        assert hash == -1 || hash == computeHashCode() : this;
        return hash;
    }

    /**
     * Computes a hash value for this identified object.
     * This method is invoked by {@link #hashCode()} when first needed.
     *
     * <p>The default implementation computes a code derived from the list of {@link IdentifiedObject} interfaces
     * implemented by this instance. The {@linkplain #getName() name}, {@linkplain #getIdentifiers() identifiers}
     * and {@linkplain #getRemarks() remarks} are intentionally <strong>not</strong> used for hash code computation.
     * See the <cite>Implementation specific feature</cite> section in {@link #hashCode()} for more information.</p>
     *
     * @return The hash code value. This value may change between different execution of the Apache SIS library.
     */
    protected int computeHashCode() {
        // Subclasses need to overrides this!!!!
        int code = (int) serialVersionUID;
        for (final Class<?> type : Classes.getLeafInterfaces(getClass(), IdentifiedObject.class)) {
            // Use a plain addition in order to be insensitive to array element order.
            code += type.hashCode();
        }
        return code;
    }
}
