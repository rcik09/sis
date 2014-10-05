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
package org.apache.sis.parameter;

import java.util.Map;
import java.util.Set;
import java.util.List;
import java.util.HashSet;
import java.util.Collections;
import org.opengis.parameter.ParameterDirection;
import org.opengis.parameter.ParameterValueGroup;
import org.opengis.parameter.ParameterDescriptorGroup;
import org.opengis.parameter.GeneralParameterDescriptor;
import org.opengis.parameter.ParameterNotFoundException;
import org.opengis.parameter.InvalidParameterNameException;
import org.apache.sis.referencing.IdentifiedObjects;
import org.apache.sis.internal.util.UnmodifiableArrayList;
import org.apache.sis.util.resources.Errors;
import org.apache.sis.util.ArgumentChecks;
import org.apache.sis.util.ComparisonMode;

import static org.apache.sis.util.Utilities.deepEquals;


/**
 * The definition of a group of related parameters used by an operation method.
 * {@code DefaultParameterDescriptorGroup} instances are immutable and thus thread-safe.
 * Each map projection or process will typically defines a single static {@code ParameterDescriptorGroup},
 * to be shared by all users of that projection or process.
 *
 * {@section Instantiation}
 * Coordinate operation or process <em>implementors</em> may use the {@link ParameterBuilder} class for making
 * their task easier.
 *
 * <div class="note"><b>Example:</b>
 * The following example declares the parameters for a <cite>Mercator (variant A)</cite> projection method
 * valid from 80°S to 84°N on all the longitude range (±180°).
 *
 * {@preformat java
 *     public class Mercator {
 *         public static final ParameterDescriptorGroup PARAMETERS;
 *         static {
 *             ParameterBuilder builder = new ParameterBuilder();
 *             builder.setCodeSpace(Citations.OGP, "EPSG").setRequired(true);
 *             ParameterDescriptor<?>[] parameters = {
 *                 builder.addName("Latitude of natural origin")    .createBounded( -80,  +84, 0, NonSI.DEGREE_ANGLE),
 *                 builder.addName("Longitude of natural origin")   .createBounded(-180, +180, 0, NonSI.DEGREE_ANGLE),
 *                 builder.addName("Scale factor at natural origin").createStrictlyPositive(1, Unit.ONE),
 *                 builder.addName("False easting")                 .create(0, SI.METRE),
 *                 builder.addName("False northing")                .create(0, SI.METRE)
 *             };
 *             builder.addIdentifier("9804")                    // Primary key in EPSG database.
 *                    .addName("Mercator (variant A)")          // EPSG name since October 2010.
 *                    .addName("Mercator (1SP)")                // EPSG name prior October 2010.
 *                    .addName(Citations.OGC, "Mercator_1SP");  // Name found in some OGC specifications.
 *             PARAMETERS = builder.createGroup(parameters);
 *         }
 *     }
 * }
 * </div>
 *
 * {@section Usage}
 * Users can simply reference the descriptor provided par a coordinate operation or process providers like below:
 *
 * {@preformat java
 *     ParameterValueGroup parameters = Mercator.PARAMETERS.createValue();
 *     // See DefaultParameterValueGroup for examples on 'parameters' usage.
 * }
 *
 * @author  Martin Desruisseaux (IRD, Geomatys)
 * @author  Johann Sorel (Geomatys)
 * @since   0.4 (derived from geotk-2.0)
 * @version 0.5
 * @module
 *
 * @see DefaultParameterValueGroup
 * @see DefaultParameterDescriptor
 */
public class DefaultParameterDescriptorGroup extends AbstractParameterDescriptor implements ParameterDescriptorGroup {
    /**
     * Serial number for inter-operability with different versions.
     */
    private static final long serialVersionUID = 6058599597772994456L;

    /**
     * The {@linkplain #descriptors() parameter descriptors} for this group.
     */
    private final List<GeneralParameterDescriptor> descriptors;

    /**
     * Constructs a parameter group from a set of properties. The properties map is given unchanged to the
     * {@linkplain AbstractParameterDescriptor#AbstractParameterDescriptor(Map) super-class constructor}.
     * The following table is a reminder of main (not all) properties:
     *
     * <table class="sis">
     *   <caption>Recognized properties (non exhaustive list)</caption>
     *   <tr>
     *     <th>Property name</th>
     *     <th>Value type</th>
     *     <th>Returned by</th>
     *   </tr>
     *   <tr>
     *     <td>{@value org.opengis.referencing.IdentifiedObject#NAME_KEY}</td>
     *     <td>{@link org.opengis.metadata.Identifier} or {@link String}</td>
     *     <td>{@link #getName()}</td>
     *   </tr>
     *   <tr>
     *     <td>{@value org.opengis.referencing.IdentifiedObject#ALIAS_KEY}</td>
     *     <td>{@link org.opengis.util.GenericName} or {@link CharSequence} (optionally as array)</td>
     *     <td>{@link #getAlias()}</td>
     *   </tr>
     *   <tr>
     *     <td>{@value org.opengis.referencing.IdentifiedObject#IDENTIFIERS_KEY}</td>
     *     <td>{@link org.opengis.metadata.Identifier} (optionally as array)</td>
     *     <td>{@link #getIdentifiers()}</td>
     *   </tr>
     *   <tr>
     *     <td>{@value org.apache.sis.parameter.AbstractParameterDescriptor#DESCRIPTION_KEY}</td>
     *     <td>{@link org.opengis.util.InternationalString} or {@link String}</td>
     *     <td>{@link #getDescription()}</td>
     *   </tr>
     *   <tr>
     *     <td>{@value org.opengis.referencing.IdentifiedObject#REMARKS_KEY}</td>
     *     <td>{@link org.opengis.util.InternationalString} or {@link String}</td>
     *     <td>{@link #getRemarks()}</td>
     *   </tr>
     * </table>
     *
     * @param properties    The properties to be given to the identified object.
     * @param minimumOccurs The {@linkplain #getMinimumOccurs() minimum number of times} that values
     *                      for this parameter group are required, or 0 if no restriction.
     * @param maximumOccurs The {@linkplain #getMaximumOccurs() maximum number of times} that values
     *                      for this parameter group are required, or {@link Integer#MAX_VALUE} if no restriction.
     * @param parameters    The {@linkplain #descriptors() parameter descriptors} for this group.
     *
     * @throws InvalidParameterNameException If a parameter name is duplicated.
     */
    public DefaultParameterDescriptorGroup(final Map<String,?> properties,
            final int minimumOccurs, final int maximumOccurs, GeneralParameterDescriptor... parameters)
    {
        super(properties, minimumOccurs, maximumOccurs);
        ArgumentChecks.ensureNonNull("parameters", parameters);
        parameters = parameters.clone();
        for (int i=0; i<parameters.length; i++) {
            ArgumentChecks.ensureNonNullElement("parameters", i, parameters);
            final String name = parameters[i].getName().getCode();
            for (int j=0; j<i; j++) {
                if (IdentifiedObjects.isHeuristicMatchForName(parameters[j], name)) {
                    throw new InvalidParameterNameException(Errors.getResources(properties).getString(
                            Errors.Keys.DuplicatedParameterName_4, parameters[j].getName().getCode(), j, name, i),
                            name);
                }
            }
        }
        descriptors = asList(parameters);
    }

    /**
     * Creates a new descriptor with the same values than the specified one.
     * This copy constructor provides a way to convert an arbitrary implementation into a SIS one or a
     * user-defined one (as a subclass), usually in order to leverage some implementation-specific API.
     *
     * <p>This constructor performs a shallow copy, i.e. the properties are not cloned.</p>
     *
     * @param descriptor The descriptor to shallow copy.
     *
     * @see #castOrCopy(ParameterDescriptorGroup)
     */
    protected DefaultParameterDescriptorGroup(final ParameterDescriptorGroup descriptor) {
        super(descriptor);
        final List<GeneralParameterDescriptor> c = descriptor.descriptors();
        if (descriptor instanceof DefaultParameterDescriptorGroup &&
                ((DefaultParameterDescriptorGroup) descriptor).descriptors == c)
        {
            descriptors = c; // Share the immutable instance (no need to clone).
        } else {
            descriptors = asList(c.toArray(new GeneralParameterDescriptor[c.size()]));
        }
    }

    /**
     * Returns the given array of parameters as an unmodifiable list.
     */
    private static List<GeneralParameterDescriptor> asList(final GeneralParameterDescriptor[] parameters) {
        switch (parameters.length) {
            case 0:  return Collections.emptyList();
            case 1:  return Collections.singletonList(parameters[0]);
            case 2:  // fall through
            case 3:  return UnmodifiableArrayList.wrap(parameters);
            default: return new AsList(parameters);
        }
    }

    /**
     * The {@link DefaultParameterDescriptorGroup#descriptors} as an unmodifiable list.
     * This class overrides {@link #contains(Object)} with a faster implementation based on {@link HashSet}.
     * This optimizations is helpful for map projection implementations, which test often for a parameter validity.
     */
    private static final class AsList extends UnmodifiableArrayList<GeneralParameterDescriptor> {
        /** For compatibility with different versions. */
        private static final long serialVersionUID = -2116304004367396735L;

        /** The element as a set, created when first needed. */
        private transient volatile Set<GeneralParameterDescriptor> asSet;

        /** Constructs a list for the specified array. */
        public AsList(final GeneralParameterDescriptor[] array) {
            super(array);
        }

        /** Tests for the inclusion of the specified descriptor. */
        @Override public boolean contains(final Object object) {
            Set<GeneralParameterDescriptor> s = asSet;
            if (s == null) {
                asSet = s = new HashSet<GeneralParameterDescriptor>(this); // No synchronization: not a big problem if created twice.
            }
            return s.contains(object);
        }
    }

    /**
     * Returns a SIS group implementation with the same values than the given arbitrary implementation.
     * If the given object is {@code null}, then this method returns {@code null}.
     * Otherwise if the given object is already a SIS implementation, then the given object is returned unchanged.
     * Otherwise a new SIS implementation is created and initialized to the values of the given object.
     *
     * @param  object The object to get as a SIS implementation, or {@code null} if none.
     * @return A SIS implementation containing the values of the given object (may be the
     *         given object itself), or {@code null} if the argument was null.
     */
    public static DefaultParameterDescriptorGroup castOrCopy(final ParameterDescriptorGroup object) {
        return (object == null) || (object instanceof DefaultParameterDescriptorGroup)
                ? (DefaultParameterDescriptorGroup) object : new DefaultParameterDescriptorGroup(object);
    }

    /**
     * Returns the GeoAPI interface implemented by this class.
     * The SIS implementation returns {@code ParameterDescriptorGroup.class}.
     *
     * <div class="note"><b>Note for implementors:</b>
     * Subclasses usually do not need to override this method since GeoAPI does not define {@code ParameterDescriptorGroup}
     * sub-interface. Overriding possibility is left mostly for implementors who wish to extend GeoAPI with their own
     * set of interfaces.</div>
     *
     * @return {@code ParameterDescriptorGroup.class} or a user-defined sub-interface.
     */
    @Override
    public Class<? extends ParameterDescriptorGroup> getInterface() {
        return ParameterDescriptorGroup.class;
    }

    /**
     * Returns an indication if all parameters in this group are inputs to the service, outputs or both.
     * If this group contains parameters with different direction, then this method returns {@code null}.
     *
     * @return Indication if all parameters are inputs or outputs to the service, or {@code null} if undetermined.
     */
    @Override
    public ParameterDirection getDirection() {
        ParameterDirection dir = null;
        for (final GeneralParameterDescriptor param : descriptors) {
            final ParameterDirection c = param.getDirection();
            if (c == null) {
                return null;
            }
            if (c != dir) {
                if (dir == null) {
                    dir = c;
                } else {
                    return null;
                }
            }
        }
        return dir;
    }

    /**
     * Returns all parameters in this group.
     *
     * @return The parameter descriptors in this group.
     */
    @Override
    public List<GeneralParameterDescriptor> descriptors() {
        return descriptors;
    }

    /**
     * Returns the first parameter in this group for the specified name.
     * This method does not search in sub-groups.
     *
     * @param  name The name of the parameter to search for.
     * @return The parameter for the given identifier name.
     * @throws ParameterNotFoundException if there is no parameter for the given name.
     */
    @Override
    @SuppressWarnings("null")
    public GeneralParameterDescriptor descriptor(final String name) throws ParameterNotFoundException {
        // Quick search for an exact match.
        ArgumentChecks.ensureNonNull("name", name);
        for (final GeneralParameterDescriptor param : descriptors) {
            if (name.equals(param.getName().getCode())) {
                return param;
            }
        }
        // More costly search before to give up.
        GeneralParameterDescriptor fallback = null, ambiguity = null;
        for (final GeneralParameterDescriptor param : descriptors) {
            if (IdentifiedObjects.isHeuristicMatchForName(param, name)) {
                if (fallback == null) {
                    fallback = param;
                } else {
                    ambiguity = param;
                }
            }
        }
        if (fallback != null && ambiguity == null) {
            return fallback;
        }
        throw new ParameterNotFoundException(ambiguity != null
                ? Errors.format(Errors.Keys.AmbiguousName_3, fallback.getName(), ambiguity.getName(), name)
                : Errors.format(Errors.Keys.ParameterNotFound_2, getName(), name), name);
    }

    /**
     * Creates a new instance of {@linkplain DefaultParameterValueGroup parameter value group}
     * initialized with the {@linkplain DefaultParameterDescriptor#getDefaultValue default values}.
     * The {@linkplain DefaultParameterValueGroup#getDescriptor() parameter descriptor} for the
     * created group will be {@code this} object.
     *
     * @return A new parameter instance initialized to the default value.
     */
    @Override
    public ParameterValueGroup createValue() {
        return new DefaultParameterValueGroup(this);
    }

    /**
     * Compares the specified object with this parameter group for equality.
     *
     * @return {@inheritDoc}
     */
    @Override
    public boolean equals(final Object object, final ComparisonMode mode) {
        if (object == this) { // Optimization for a common case.
            return true;
        }
        if (super.equals(object, mode)) {
            switch (mode) {
                case STRICT: {
                    return descriptors.equals(((DefaultParameterDescriptorGroup) object).descriptors);
                }
                default: {
                    return deepEquals(descriptors(), ((ParameterDescriptorGroup) object).descriptors(), mode);
                }
            }
        }
        return false;
    }

    /**
     * Invoked by {@link #hashCode()} for computing the hash code when first needed.
     *
     * @return {@inheritDoc}
     */
    @Override
    protected long computeHashCode() {
        return super.computeHashCode() + descriptors.hashCode();
    }
}
