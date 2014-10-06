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
import java.util.List;
import java.util.Locale;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import org.opengis.util.NameSpace;
import org.opengis.util.LocalName;
import org.opengis.util.ScopedName;
import org.opengis.util.GenericName;
import org.opengis.util.NameFactory;
import org.opengis.util.InternationalString;
import org.opengis.metadata.citation.Citation;
import org.opengis.metadata.Identifier;
import org.opengis.referencing.ReferenceIdentifier;
import org.opengis.parameter.InvalidParameterValueException;
import org.apache.sis.internal.metadata.NameToIdentifier;
import org.apache.sis.internal.system.DefaultFactories;
import org.apache.sis.metadata.iso.citation.Citations;
import org.apache.sis.metadata.iso.ImmutableIdentifier;
import org.apache.sis.util.collection.WeakValueHashMap;

// Branch-dependent imports
import org.apache.sis.internal.jdk7.Objects;


/**
 * An identification of a CRS object which is both a {@link Identifier} and a {@link GenericName}.
 * This class implements both interfaces in order to allow usage of the same instance either as an object
 * {@linkplain AbstractIdentifiedObject#getName() name} or {@linkplain AbstractIdentifiedObject#getAlias() alias}.
 * This flexibility make easier to uses object's names in two different models:
 *
 * <ul>
 *   <li>In the ISO 19111 model, objects have a single name of type {@code RS_Identifier} and an arbitrary amount
 *       of aliases of type {@code GenericName}.</li>
 *   <li>In the GML model, objects have an arbitrary amount of names of type {@code gml:CodeType},
 *       but do not have any alias.</li>
 * </ul>
 *
 * By using this {@code NamedIdentifier} class, users can declare supplemental object's names as
 * {@linkplain AbstractIdentifiedObject#getAlias() aliases} and have those names used in contexts
 * where {@code Identifier} instances are required, like GML marshalling time.
 *
 * {@section Name ↔ Identifier mapping}
 * The {@code GenericName} attributes will be inferred from {@code Identifier} attributes as below:
 *
 * <ul>
 *   <li><b>{@linkplain #tip() Tip}:</b> derived from the identifier {@linkplain #getCode() code}.</li>
 *   <li><b>{@linkplain #head() Head}:</b> derived from the identifier {@linkplain #getCodeSpace() code space}.</li>
 *   <li><b>{@linkplain #scope() Scope}:</b> derived from the shortest {@linkplain #getAuthority() authority}'s
 *     {@linkplain Citation#getAlternateTitles() alternate titles}, or the {@linkplain Citation#getTitle() main title}
 *     if there is no alternate titles. This policy exploits the ISO 19115 comment saying that citation alternate titles
 *     often contain abbreviation (for example "DCW" as an alternative title for "<cite>Digital Chart of the World</cite>").</li>
 * </ul>
 *
 * <div class="note"><b>Example:</b>
 * If the identifier attributes are {@code authority} = {@link Citations#OGP}, {@code codeSpace} = {@code "EPSG"}
 * and {@code code} = {@code "4326"}, then the name attributes will be {@code scope} = {@code "OGP"},
 * {@code head} = {@code "EPSG"}, {@code tip} = {@code "4326"} and {@link #toString()} = {@code "EPSG:4326"}.
 * Note that the scope does not appear in the string representation of names.</div>
 *
 *
 * {@section Immutability and thread safety}
 * This class is immutable and thus inherently thread-safe if the {@link Citation} and {@link InternationalString}
 * arguments given to the constructor are also immutable. It is caller's responsibility to ensure that those
 * conditions hold, for example by invoking {@link org.apache.sis.metadata.iso.citation.DefaultCitation#freeze()
 * DefaultCitation.freeze()} before passing the arguments to the constructor.
 * Subclasses shall make sure that any overridden methods remain safe to call from multiple threads and do not change
 * any public {@code NamedIdentifier} state.
 *
 * @author  Martin Desruisseaux (IRD, Geomatys)
 * @since   0.4 (derived from geotk-2.0)
 * @version 0.4
 * @module
 */
public class NamedIdentifier extends ImmutableIdentifier implements GenericName {
    /**
     * Serial number for inter-operability with different versions.
     */
    private static final long serialVersionUID = -3982456534858346939L;

    /**
     * A pool of {@link NameSpace} values for given {@link InternationalString}.
     */
    private static final Map<CharSequence,NameSpace> SCOPES =
            new WeakValueHashMap<CharSequence,NameSpace>(CharSequence.class);

    /**
     * The name of this identifier as a generic name.
     * If {@code null}, will be constructed only when first needed.
     */
    private transient GenericName name;

    /**
     * {@code true} if {@link #name} has been given explicitly by the user.
     * Consider this field as final - it is not only for constructors convenience.
     */
    private transient boolean isNameSupplied;

    /**
     * Creates a new identifier from the specified one. This is a copy constructor
     * which will get the code, codespace, authority, version and the remarks (if
     * available) from the given identifier.
     *
     * <p>If the given identifier implements the {@link GenericName} interface, then calls to
     * {@link #tip()}, {@link #head()}, {@link #scope()} and similar methods will delegate
     * to that name.</p>
     *
     * @param identifier The identifier to copy.
     */
    public NamedIdentifier(final ReferenceIdentifier identifier) {
        super(identifier);
        if (identifier instanceof GenericName) {
            name = (GenericName) identifier;
            isNameSupplied = true;
        }
    }

    /**
     * Creates a new identifier from the specified name. This constructor infers the identifier attributes
     * (code, codespace and authority) from the given name. Calls to name-related methods like {@link #tip()},
     * {@link #head()} and {@link #scope()} will delegate to the given name.
     *
     * @param name The name to wrap.
     */
    public NamedIdentifier(final GenericName name) {
        super(name instanceof ReferenceIdentifier ? (ReferenceIdentifier) name : new NameToIdentifier(name));
        this.name = name;
        isNameSupplied = true;
    }

    /**
     * Constructs an identifier from the given properties. The content of the properties map is used as
     * described in the {@linkplain ImmutableIdentifier#ImmutableIdentifier(Map) super-class constructor}.
     *
     * @param  properties The properties to be given to this identifier.
     * @throws InvalidParameterValueException if a property has an invalid value.
     * @throws IllegalArgumentException if a property is invalid for some other reason.
     */
    public NamedIdentifier(final Map<String,?> properties) throws IllegalArgumentException {
        super(properties);
    }

    /**
     * Constructs an identifier from an authority and localizable code.
     * This is a convenience constructor for commonly-used parameters.
     *
     * @param authority The authority (e.g. {@link Citations#OGC} or {@link Citations#EPSG}),
     *                  or {@code null} if not available.
     * @param code      The code. The {@code code.toString(Locale.ROOT)} return value will be used for the
     *                  {@link #getCode() code} property, and the complete international string will be used
     *                  for the {@link #getName() name} property.
     */
    public NamedIdentifier(final Citation authority, final InternationalString code) {
        this(authority, code.toString(Locale.ROOT));
        name = createName(authority, code);
        isNameSupplied = true; // Because 'code' is an international string.
    }

    /**
     * Constructs an identifier from an authority and code.
     * This is a convenience constructor for commonly-used parameters.
     *
     * @param authority The authority (e.g. {@link Citations#OGC} or {@link Citations#EPSG}),
     *                  or {@code null} if not available.
     * @param code      The code. This parameter is mandatory.
     */
    public NamedIdentifier(final Citation authority, final String code) {
        super(authority, Citations.getIdentifier(authority), code);
    }

    /**
     * Creates an identifier from the specified code and authority,
     * with an optional version number and remarks.
     *
     * @param authority
     *          Organization or party responsible for definition and maintenance of the code
     *          space or code, or {@code null} if not available.
     * @param codeSpace
     *          Name or identifier of the person or organization responsible for namespace, or
     *          {@code null} if not available. This is often an abbreviation of the authority name.
     * @param code
     *          Identifier code or name, optionally from a controlled list or pattern defined by
     *          a code space. The code can not be null.
     * @param version
     *          The version of the associated code space or code as specified by the code authority,
     *          or {@code null} if none.
     * @param remarks
     *          Comments on or information about this identifier, or {@code null} if none.
     */
    public NamedIdentifier(final Citation authority, final String codeSpace,
            final String code, final String version, final InternationalString remarks)
    {
        super(authority, codeSpace, code, version, remarks);
    }

    /**
     * Returns the generic name of this identifier.
     * The name will be constructed automatically the first time it will be needed.
     * The name's scope is inferred from the shortest alternative title (if any).
     * This heuristic rule is compatible to the ISO 19115 remark saying that the
     * {@linkplain Citation#getAlternateTitles() alternate titles} often contains abbreviation
     * (for example "DCW" as an alternative title for "Digital Chart of the World").
     * If no alternative title is found or if the main title is yet shorter, then it is used.
     *
     * @category Generic name
     */
    private synchronized GenericName getName() {
        if (name == null) {
            name = createName(super.getAuthority(), super.getCode());
        }
        return name;
    }

    /**
     * Constructs a generic name from the specified authority and code.
     *
     * @param  authority The authority, or {@code null} if none.
     * @param  code The code.
     * @return A new generic name for the given authority and code.
     * @category Generic name
     */
    private GenericName createName(final Citation authority, final CharSequence code) {
        final NameFactory factory = DefaultFactories.NAMES;
        final String title = Citations.getIdentifier(authority); // Whitespaces trimed by Citations.
        NameSpace scope = null;
        if (title != null) {
            synchronized (SCOPES) {
                scope = SCOPES.get(title);
                if (scope == null) {
                    scope = factory.createNameSpace(factory.createLocalName(null, title), null);
                    SCOPES.put(title, scope);
                }
            }
        }
        final String codeSpace = super.getCodeSpace();
        if (codeSpace != null) {
            return factory.createGenericName(scope, codeSpace, code);
        } else {
            return factory.createLocalName(scope, code);
        }
    }

    /**
     * The last element in the sequence of {@linkplain #getParsedNames() parsed names}.
     * By default, this is the same value than the {@linkplain #getCode() code} provided as a local name.
     *
     * @return The last element in the list of {@linkplain #getParsedNames() parsed names}.
     *
     * @see #getCode()
     */
    @Override
    public LocalName tip() {
        return getName().tip();
    }

    /**
     * Returns the first element in the sequence of {@linkplain #getParsedNames() parsed names}.
     * By default, this is the same value than the {@linkplain #getCodeSpace() code space} provided as a local name.
     *
     * @return The first element in the list of {@linkplain #getParsedNames() parsed names}.
     *
     * @see #scope()
     * @see #getCodeSpace()
     */
    @Override
    public LocalName head() {
        return getName().head();
    }

    /**
     * Returns the scope (name space) in which this name is local.
     * By default, this is the same value than the {@link #getAuthority() authority} provided as a name space.
     *
     * @see #head()
     * @see #getAuthority()
     *
     * @return The scope of this name.
     */
    @Override
    public NameSpace scope() {
        return getName().scope();
    }

    /**
     * Returns the depth of this name within the namespace hierarchy.
     *
     * @return The depth of this name.
     */
    @Override
    public int depth() {
        return getName().depth();
    }

    /**
     * Returns the sequence of {@linkplain LocalName local names} making this generic name.
     * The length of this sequence is the {@linkplain #depth() depth}.
     * It does not include the {@linkplain #scope() scope}.
     *
     * @return The local names making this generic name, without the {@linkplain #scope() scope}.
     *         Shall never be {@code null} neither empty.
     */
    @Override
    public List<? extends LocalName> getParsedNames() {
        return getName().getParsedNames();
    }

    /**
     * Returns this name expanded with the specified scope. One may represent this operation
     * as a concatenation of the specified {@code name} with {@code this}.
     *
     * @param scope The name to use as prefix.
     * @return A concatenation of the given scope with this name.
     */
    @Override
    public ScopedName push(final GenericName scope) {
        return getName().push(scope);
    }

    /**
     * Returns a view of this name as a fully-qualified name.
     *
     * @return The fully-qualified name (never {@code null}).
     */
    @Override
    public GenericName toFullyQualifiedName() {
        return getName().toFullyQualifiedName();
    }

    /**
     * Returns a local-dependent string representation of this generic name.
     * This string is similar to the one returned by {@link #toString()} except that each element has
     * been localized in the {@linkplain InternationalString#toString(Locale) specified locale}.
     * If no international string is available, then this method returns an implementation mapping
     * to {@code toString()} for all locales.
     *
     * @return A localizable string representation of this name.
     */
    @Override
    public InternationalString toInternationalString() {
        return getName().toInternationalString();
    }

    /**
     * Returns a string representation of this generic name. This string representation
     * is local-independent. It contains all elements listed by {@link #getParsedNames()}
     * separated by a namespace-dependent character (usually {@code :} or {@code /}).
     *
     * @return A local-independent string representation of this generic name.
     *
     * @see IdentifiedObjects#toString(Identifier)
     */
    @Override
    public String toString() {
        return getName().toString();
    }

    /**
     * Compares this name with the specified object for order. Returns a negative integer,
     * zero, or a positive integer as this name lexicographically precedes, is equal to,
     * or follows the specified object.
     *
     * @param object The object to compare with.
     * @return -1 if this identifier precedes the given object, +1 if it follows it.
     */
    @Override
    public int compareTo(final GenericName object) {
        return getName().compareTo(object);
    }

    /**
     * Compares this identifier with the specified object for equality.
     *
     * @param object The object to compare with this name.
     * @return {@code true} if the given object is equal to this name.
     */
    @Override
    public boolean equals(final Object object) {
        if (object == this) {
            return true;
        }
        if (super.equals(object)) {
            if (!isNameSupplied) {
                return true; // No need to compare names if they are computed from the same values.
            }
            final NamedIdentifier that = (NamedIdentifier) object;
            return Objects.equals(this.getName(), that.getName());
        }
        return false;
    }

    /**
     * Returns a hash code value for this object.
     */
    @Override
    public int hashCode() {
        /*
         * We do not use the name because it is usually inferred from existing properties.
         * We only revert the bits for differentiating this NamedIdentifier class from its parent class.
         */
        return ~super.hashCode();
    }

    /**
     * Invoked on serialization for writing the {@linkplain #name} if it was supplied by the user.
     * Otherwise, we will let {@link #getName()} recompute the name only when needed.
     *
     * @param  out The output stream where to serialize this named identifier.
     * @throws IOException If an I/O error occurred while writing.
     */
    private void writeObject(final ObjectOutputStream out) throws IOException {
        out.defaultWriteObject();
        out.writeObject(isNameSupplied ? name : null);
    }

    /**
     * Invoked on deserialization for reading the name written by {@link #writeObject(ObjectOutputStream)},
     * if any.
     *
     * @param  in The input stream from which to deserialize a named identifier.
     * @throws IOException If an I/O error occurred while reading or if the stream contains invalid data.
     * @throws ClassNotFoundException If the class serialized on the stream is not on the classpath.
     */
    private void readObject(final ObjectInputStream in) throws IOException, ClassNotFoundException {
        in.defaultReadObject();
        name = (GenericName) in.readObject();
        isNameSupplied = (name != null);
    }
}
