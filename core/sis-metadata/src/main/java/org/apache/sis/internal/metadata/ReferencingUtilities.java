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
package org.apache.sis.internal.metadata;

import java.util.Collection;
import java.util.logging.Logger;
import javax.measure.unit.Unit;
import org.opengis.annotation.UML;
import org.opengis.annotation.Specification;
import org.opengis.parameter.*;
import org.opengis.referencing.*;
import org.opengis.referencing.cs.*;
import org.opengis.referencing.crs.*;
import org.opengis.referencing.datum.*;
import org.opengis.referencing.operation.*;
import org.apache.sis.util.Static;
import org.apache.sis.util.logging.Logging;
import org.apache.sis.util.resources.Errors;
import org.apache.sis.internal.jaxb.Context;


/**
 * A set of static methods working on GeoAPI referencing objects.
 * Some of those methods may be useful, but not really rigorous.
 * This is why they do not appear in the public packages.
 *
 * <p><strong>Do not rely on this API!</strong> It may change in incompatible way in any future release.</p>
 *
 * @author  Martin Desruisseaux (IRD, Geomatys)
 * @since   0.4 (derived from geotk-2.0)
 * @version 0.5
 * @module
 */
public final class ReferencingUtilities extends Static {
    /**
     * The logger to use for messages related to the {@code sis-referencing} module.
     */
    public static final Logger LOGGER = Logging.getLogger("org.apache.sis.referencing");

    /**
     * Subtypes of {@link IdentifiedObject} for which a URN type is defined.
     * For each interface at index <var>i</var>, the URN type is {@code URN_TYPES[i]}.
     *
     * <p>For performance reasons, most frequently used types should be first.</p>
     */
    private static final Class<?>[] TYPES = {
        CoordinateReferenceSystem.class,
        Datum.class,
        Ellipsoid.class,
        PrimeMeridian.class,
        CoordinateSystem.class,
        CoordinateSystemAxis.class,
        CoordinateOperation.class,
        OperationMethod.class,
        ParameterDescriptor.class,
        ReferenceSystem.class
    };

    /**
     * The URN types for instances of {@link #TYPES}.
     * See {@link org.apache.sis.internal.util.DefinitionURI} javadoc for a list of URN types.
     */
    private static final String[] URN_TYPES = {
        "crs",
        "datum",
        "ellipsoid",
        "meridian",
        "cs",
        "axis",
        "coordinateOperation",
        "method",
        "parameter",
        "referenceSystem"
    };

    /**
     * Do not allow instantiation of this class.
     */
    private ReferencingUtilities() {
    }

    /**
     * Returns {@code true} if codes in the given code space are often represented using the URN syntax.
     * Current implementation conservatively returns {@code true} only for {@code "EPSG"}.
     * The list of accepted code spaces may be expanded in any future SIS version.
     *
     * @param  codeSpace The code space (can be {@code null}).
     * @return {@code true} if the given code space is known to use the URN syntax.
     */
    public static boolean usesURN(final String codeSpace) {
        return (codeSpace != null) && codeSpace.equalsIgnoreCase("EPSG");
    }

    /**
     * Returns the URN type for the given class, or {@code null} if unknown.
     * See {@link org.apache.sis.internal.util.DefinitionURI} javadoc for a list of URN types.
     *
     * @param  type The class for which to get the URN type.
     * @return The URN type, or {@code null} if unknown.
     *
     * @see org.apache.sis.internal.util.DefinitionURI
     */
    public static String toURNType(final Class<?> type) {
        for (int i=0; i<TYPES.length; i++) {
            if (TYPES[i].isAssignableFrom(type)) {
                return URN_TYPES[i];
            }
        }
        return null;
    }

    /**
     * Returns the WKT type of the given interface.
     *
     * For {@link CoordinateSystem} base type, the returned value shall be one of
     * {@code affine}, {@code Cartesian}, {@code cylindrical}, {@code ellipsoidal}, {@code linear},
     * {@code parametric}, {@code polar}, {@code spherical}, {@code temporal} or {@code vertical}.
     *
     * @param  base The abstract base interface.
     * @param  type The interface or classes for which to get the WKT type.
     * @return The WKT type for the given class or interface, or {@code null} if none.
     */
    public static String toWKTType(final Class<?> base, final Class<?> type) {
        if (type != base) {
            final UML uml = type.getAnnotation(UML.class);
            if (uml != null && uml.specification() == Specification.ISO_19111) {
                String name = uml.identifier();
                final int length = name.length() - 5; // Length without "CS_" and "CS".
                if (length >= 1 && name.startsWith("CS_") && name.endsWith("CS")) {
                    final StringBuilder buffer = new StringBuilder(length).append(name, 3, 3 + length);
                    if (!name.regionMatches(3, "Cartesian", 0, 9)) {
                        buffer.setCharAt(0, Character.toLowerCase(buffer.charAt(0)));
                    }
                    name = buffer.toString();
                    if (name.equals("time")) {
                        name = "temporal";
                    }
                    return name;
                }
            }
            for (final Class<?> c : type.getInterfaces()) {
                if (base.isAssignableFrom(c)) {
                    final String name = toWKTType(base, c);
                    if (name != null) {
                        return name;
                    }
                }
            }
        }
        return null;
    }

    /**
     * Returns the unit used for all axes in the given coordinate system.
     * If not all axes use the same unit, then this method returns {@code null}.
     *
     * <p>This method is used either when the coordinate system is expected to contain exactly one axis,
     * or for operations that support only one units for all axes, for example Well Know Text version 1
     * (WKT 1) formatting.</p>
     *
     * @param cs The coordinate system for which to get the unit, or {@code null}.
     * @return The unit for all axis in the given coordinate system, or {@code null}.
     */
    public static Unit<?> getUnit(final CoordinateSystem cs) {
        Unit<?> unit = null;
        if (cs != null) {
            for (int i=cs.getDimension(); --i>=0;) {
                final CoordinateSystemAxis axis = cs.getAxis(i);
                if (axis != null) { // Paranoiac check.
                    final Unit<?> candidate = axis.getUnit();
                    if (candidate != null) {
                        if (unit == null) {
                            unit = candidate;
                        } else if (!unit.equals(candidate)) {
                            return null;
                        }
                    }
                }
            }
        }
        return unit;
    }

    /**
     * Copies all {@link SingleCRS} components from the given source to the given collection.
     * For each {@link CompoundCRS} element found in the iteration, this method replaces the
     * {@code CompoundCRS} by its {@linkplain CompoundCRS#getComponents() components}, which
     * may themselves have other {@code CompoundCRS}. Those replacements are performed recursively
     * until we obtain a flat view of CRS components.
     *
     * @param  source The collection of single or compound CRS.
     * @param  addTo  Where to add the single CRS in order to obtain a flat view of {@code source}.
     * @return {@code true} if this method found only single CRS in {@code source}, in which case {@code addTo}
     *         got the same content (assuming that {@code addTo} was empty prior this method call).
     * @throws ClassCastException if a CRS is neither a {@link SingleCRS} or a {@link CompoundCRS}.
     *
     * @see org.apache.sis.referencing.CRS#getSingleComponents(CoordinateReferenceSystem)
     */
    public static boolean getSingleComponents(final Iterable<? extends CoordinateReferenceSystem> source,
            final Collection<? super SingleCRS> addTo) throws ClassCastException
    {
        boolean sameContent = true;
        for (final CoordinateReferenceSystem candidate : source) {
            if (candidate instanceof CompoundCRS) {
                getSingleComponents(((CompoundCRS) candidate).getComponents(), addTo);
                sameContent = false;
            } else {
                // Intentional CassCastException here if the candidate is not a SingleCRS.
                addTo.add((SingleCRS) candidate);
            }
        }
        return sameContent;
    }

    /**
     * Ensures that the given argument value is {@code false}. This method is invoked by private setter methods,
     * which are themselves invoked by JAXB at unmarshalling time. Invoking this method from those setter methods
     * serves two purposes:
     *
     * <ul>
     *   <li>Make sure that a singleton property is not defined twice in the XML document.</li>
     *   <li>Protect ourselves against changes in immutable objects outside unmarshalling. It should
     *       not be necessary since the setter methods shall not be public, but we are paranoiac.</li>
     *   <li>Be a central point where we can trace all setter methods, in case we want to improve
     *       warning or error messages in future SIS versions.</li>
     * </ul>
     *
     * @param  classe    The caller class, used only in case of warning message to log.
     * @param  method    The caller method, used only in case of warning message to log.
     * @param  name      The property name, used only in case of error message to format.
     * @param  isDefined Whether the property in the caller object is current defined.
     * @return {@code true} if the caller can set the property.
     * @throws IllegalStateException If {@code isDefined} is {@code true} and we are not unmarshalling an object.
     */
    public static boolean canSetProperty(final Class<?> classe, final String method,
            final String name, final boolean isDefined) throws IllegalStateException
    {
        if (!isDefined) {
            return true;
        }
        final Context context = Context.current();
        if (context != null) {
            Context.warningOccured(context, LOGGER, classe, method, Errors.class, Errors.Keys.ElementAlreadyPresent_1, name);
            return false;
        } else {
            throw new IllegalStateException(Errors.format(Errors.Keys.ElementAlreadyPresent_1, name));
        }
    }
}
