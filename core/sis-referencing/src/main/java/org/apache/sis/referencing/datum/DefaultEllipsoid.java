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
package org.apache.sis.referencing.datum;

import java.util.Map;
import javax.measure.unit.SI;
import javax.measure.unit.Unit;
import javax.measure.quantity.Length;
import javax.measure.converter.ConversionException;
import javax.xml.bind.annotation.XmlType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import org.opengis.util.GenericName;
import org.opengis.util.InternationalString;
import org.opengis.metadata.Identifier;
import org.opengis.referencing.datum.Ellipsoid;
import org.apache.sis.geometry.DirectPosition2D;
import org.apache.sis.internal.util.Numerics;
import org.apache.sis.internal.jaxb.Context;
import org.apache.sis.internal.jaxb.gco.Measure;
import org.apache.sis.internal.jaxb.referencing.SecondDefiningParameter;
import org.apache.sis.internal.referencing.Formulas;
import org.apache.sis.referencing.IdentifiedObjects;
import org.apache.sis.referencing.AbstractIdentifiedObject;
import org.apache.sis.io.wkt.Formatter;
import org.apache.sis.io.wkt.Convention;
import org.apache.sis.util.ComparisonMode;
import org.apache.sis.util.resources.Errors;

import static java.lang.Math.*;
import static java.lang.Double.*;
import static org.apache.sis.internal.util.Numerics.epsilonEqual;
import static org.apache.sis.util.ArgumentChecks.ensureStrictlyPositive;
import static org.apache.sis.util.ArgumentChecks.ensureNonNull;

// Branch-dependent imports
import org.apache.sis.internal.jdk7.Objects;


/**
 * Geometric figure that can be used to describe the approximate shape of the earth.
 * In mathematical terms, it is a surface formed by the rotation of an ellipse about
 * its minor axis. An ellipsoid requires two defining parameters:
 *
 * <ul>
 *   <li>{@linkplain #getSemiMajorAxis() semi-major axis} and
 *       {@linkplain #getInverseFlattening() inverse flattening}, or</li>
 *   <li>{@linkplain #getSemiMajorAxis() semi-major axis} and
 *       {@linkplain #getSemiMinorAxis() semi-minor axis}.</li>
 * </ul>
 *
 * Some numerical values derived from the above properties are:
 *
 * <ul>
 *   <li>{@linkplain #getAuthalicRadius() authalic radius}</li>
 *   <li>{@linkplain #getEccentricity() eccentricity}</li>
 * </ul>
 *
 * {@section Distance calculations}
 * This class contains an {@link #orthodromicDistance(double, double, double, double)} convenience method
 * for calculating distances on great circles.
 *
 * {@section Creating new ellipsoid instances}
 * New instances can be created either directly by specifying all information to a factory method (choices 3
 * and 4 below), or indirectly by specifying the identifier of an entry in a database (choices 1 and 2 below).
 * In particular, the <a href="http://www.epsg.org">EPSG</a> database provides definitions for many ellipsoids,
 * and Apache SIS provides convenience shortcuts for some of them.
 *
 * <p>Choice 1 in the following list is the easiest but most restrictive way to get an ellipsoid.
 * The other choices provide more freedom. Each choice delegates its work to the subsequent items
 * (in the default configuration), so this list can been seen as <cite>top to bottom</cite> API.</p>
 *
 * <ol>
 *   <li>Create an {@code Ellipsoid} from one of the static convenience shortcuts listed in
 *       {@link org.apache.sis.referencing.CommonCRS#ellipsoid()}.</li>
 *   <li>Create an {@code Ellipsoid} from an identifier in a database by invoking
 *       {@link org.opengis.referencing.datum.DatumAuthorityFactory#createEllipsoid(String)}.</li>
 *   <li>Create an {@code Ellipsoid} by invoking the {@code createEllipsoid(…)} or {@code createFlattenedSphere(…)}
 *       methods defined in the {@link org.opengis.referencing.datum.DatumFactory} interface.</li>
 *   <li>Create a {@code DefaultEllipsoid} by invoking the
 *       {@link #createEllipsoid(Map, double, double, Unit) createEllipsoid(…)} or
 *       {@link #createFlattenedSphere(Map, double, double, Unit) createFlattenedSphere(…)}
 *       static methods defined in this class.</li>
 * </ol>
 *
 * <b>Example:</b> the following code gets the WGS84 ellipsoid:
 *
 * {@preformat java
 *     Ellipsoid e = CommonCRS.WGS84.ellipsoid();
 * }
 *
 * {@section Immutability and thread safety}
 * This class is immutable and thus thread-safe if the property <em>values</em> (not necessarily the map itself)
 * given to the constructors are also immutable. Unless otherwise noted in the javadoc, this condition holds if all
 * components were created using only SIS factories and static constants.
 *
 * @author  Martin Desruisseaux (IRD, Geomatys)
 * @author  Cédric Briançon (Geomatys)
 * @since   0.4 (derived from geotk-1.2)
 * @version 0.4
 * @module
 *
 * @see org.apache.sis.referencing.CommonCRS#ellipsoid()
 */
@XmlType(name="EllipsoidType", propOrder={
    "semiMajorAxisMeasure",
    "secondDefiningParameter"
})
@XmlRootElement(name = "Ellipsoid")
public class DefaultEllipsoid extends AbstractIdentifiedObject implements Ellipsoid {
    /**
     * Serial number for inter-operability with different versions.
     */
    private static final long serialVersionUID = -1149451543954764081L;

    /**
     * Maximum number of iterations in the {@link #orthodromicDistance(double, double, double, double)} method.
     */
    private static final int MAX_ITERATIONS = 100;

    /**
     * Small tolerance value for {@link #orthodromicDistance(double, double, double, double)}.
     */
    private static final double EPS = 0.5E-13;

    /**
     * Tolerance threshold for comparing floating point numbers.
     *
     * @see Numerics#COMPARISON_THRESHOLD
     */
    private static final double COMPARISON_THRESHOLD = 1E-10;

    /**
     * The equatorial radius. This field should be considered as final.
     * It is modified only by JAXB at unmarshalling time.
     *
     * @see #getSemiMajorAxis()
     */
    private double semiMajorAxis;

    /**
     * The polar radius. This field should be considered as final.
     * It is modified only by JAXB at unmarshalling time.
     *
     * @see #getSemiMinorAxis()
     */
    private double semiMinorAxis;

    /**
     * The inverse of the flattening value, or {@link Double#POSITIVE_INFINITY} if the ellipsoid is a sphere.
     * This field shall be considered as final. It is modified only by JAXB at unmarshalling time.
     *
     * @see #getInverseFlattening()
     */
    private double inverseFlattening;

    /**
     * Tells if the Inverse Flattening is definitive for this ellipsoid.
     * This field shall be considered as final. It is modified only by JAXB at unmarshalling time.
     *
     * @see #isIvfDefinitive()
     */
    private boolean ivfDefinitive;

    /**
     * The units of the semi-major and semi-minor axis values.
     */
    private Unit<Length> unit;

    /**
     * Constructs a new object in which every attributes are set to a null value.
     * <strong>This is not a valid object.</strong> This constructor is strictly
     * reserved to JAXB, which will assign values to the fields using reflexion.
     */
    private DefaultEllipsoid() {
        super(org.apache.sis.internal.referencing.NilReferencingObject.INSTANCE);
        // We need to let the DefaultEllipsoid fields unitialized
        // because afterUnmarshal(…) will check for zero values.
    }

    /**
     * Creates a new ellipsoid using the specified axis length.
     * The properties map is given unchanged to the
     * {@linkplain AbstractIdentifiedObject#AbstractIdentifiedObject(Map) super-class constructor}.
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
     *     <td>{@link Identifier} or {@link String}</td>
     *     <td>{@link #getName()}</td>
     *   </tr>
     *   <tr>
     *     <td>{@value org.opengis.referencing.IdentifiedObject#ALIAS_KEY}</td>
     *     <td>{@link GenericName} or {@link CharSequence} (optionally as array)</td>
     *     <td>{@link #getAlias()}</td>
     *   </tr>
     *   <tr>
     *     <td>{@value org.opengis.referencing.IdentifiedObject#IDENTIFIERS_KEY}</td>
     *     <td>{@link Identifier} (optionally as array)</td>
     *     <td>{@link #getIdentifiers()}</td>
     *   </tr>
     *   <tr>
     *     <td>{@value org.opengis.referencing.IdentifiedObject#REMARKS_KEY}</td>
     *     <td>{@link InternationalString} or {@link String}</td>
     *     <td>{@link #getRemarks()}</td>
     *   </tr>
     * </table>
     *
     * @param properties        The properties to be given to the identified object.
     * @param semiMajorAxis     The equatorial radius.
     * @param semiMinorAxis     The polar radius.
     * @param inverseFlattening The inverse of the flattening value.
     * @param ivfDefinitive     {@code true} if the inverse flattening is definitive.
     * @param unit              The units of the semi-major and semi-minor axis values.
     *
     * @see #createEllipsoid(Map, double, double, Unit)
     * @see #createFlattenedSphere(Map, double, double, Unit)
     */
    protected DefaultEllipsoid(final Map<String,?> properties,
                               final double  semiMajorAxis,
                               final double  semiMinorAxis,
                               final double  inverseFlattening,
                               final boolean ivfDefinitive,
                               final Unit<Length> unit)
    {
        super(properties);
        ensureNonNull         ("unit",              unit);
        ensureStrictlyPositive("semiMajorAxis",     semiMajorAxis);
        ensureStrictlyPositive("semiMinorAxis",     semiMinorAxis);
        ensureStrictlyPositive("inverseFlattening", inverseFlattening);
        this.unit              = unit;
        this.semiMajorAxis     = semiMajorAxis;
        this.semiMinorAxis     = semiMinorAxis;
        this.inverseFlattening = inverseFlattening;
        this.ivfDefinitive     = ivfDefinitive;
    }

    /**
     * Creates a new ellipsoid with the same values than the specified one.
     * This copy constructor provides a way to convert an arbitrary implementation into a SIS one
     * or a user-defined one (as a subclass), usually in order to leverage some implementation-specific API.
     *
     * <p>This constructor performs a shallow copy, i.e. the properties are not cloned.</p>
     *
     * @param ellipsoid The ellipsoid to copy.
     *
     * @see #castOrCopy(Ellipsoid)
     */
    protected DefaultEllipsoid(final Ellipsoid ellipsoid) {
        super(ellipsoid);
        semiMajorAxis     = ellipsoid.getSemiMajorAxis();
        semiMinorAxis     = ellipsoid.getSemiMinorAxis();
        inverseFlattening = ellipsoid.getInverseFlattening();
        ivfDefinitive     = ellipsoid.isIvfDefinitive();
        unit              = ellipsoid.getAxisUnit();
    }

    /**
     * Creates a new ellipsoid using the specified properties and axis length.
     * The properties map is given unchanged to the
     * {@linkplain AbstractIdentifiedObject#AbstractIdentifiedObject(Map) super-class constructor}.
     *
     * @param properties    The properties to be given to the identified object.
     * @param semiMajorAxis The equatorial radius in the given unit.
     * @param semiMinorAxis The polar radius in the given unit.
     * @param unit          The units of the semi-major and semi-minor axis values.
     * @return An ellipsoid with the given axis length.
     */
    public static DefaultEllipsoid createEllipsoid(final Map<String,?> properties,
                                                   final double semiMajorAxis,
                                                   final double semiMinorAxis,
                                                   final Unit<Length> unit)
    {
        if (semiMajorAxis == semiMinorAxis) {
            return new Sphere(properties, semiMajorAxis, false, unit);
        } else {
            return new DefaultEllipsoid(properties, semiMajorAxis, semiMinorAxis,
                       semiMajorAxis / (semiMajorAxis - semiMinorAxis), false, unit);
        }
    }

    /**
     * Creates a new ellipsoid using the specified properties, axis length and inverse flattening value.
     * The properties map is given unchanged to the
     * {@linkplain AbstractIdentifiedObject#AbstractIdentifiedObject(Map) super-class constructor}.
     *
     * @param  properties       The properties to be given to the identified object.
     * @param semiMajorAxis     The equatorial radius in the given unit.
     * @param inverseFlattening The inverse flattening value.
     * @param unit              The units of the semi-major and semi-minor axis values.
     * @return An ellipsoid with the given axis length.
     */
    public static DefaultEllipsoid createFlattenedSphere(final Map<String,?> properties,
                                                         final double semiMajorAxis,
                                                         final double inverseFlattening,
                                                         final Unit<Length> unit)
    {
        if (isInfinite(inverseFlattening)) {
            return new Sphere(properties, semiMajorAxis, true, unit);
        } else {
            return new DefaultEllipsoid(properties, semiMajorAxis,
                    semiMajorAxis * (1 - 1/inverseFlattening), inverseFlattening, true, unit);
        }
    }

    /**
     * Returns a SIS ellipsoid implementation with the same values than the given arbitrary implementation.
     * If the given object is {@code null}, then this method returns {@code null}.
     * Otherwise if the given object is already a SIS implementation, then the given object is returned unchanged.
     * Otherwise a new SIS implementation is created and initialized to the attribute values of the given object.
     *
     * @param  object The object to get as a SIS implementation, or {@code null} if none.
     * @return A SIS implementation containing the values of the given object (may be the
     *         given object itself), or {@code null} if the argument was null.
     */
    public static DefaultEllipsoid castOrCopy(final Ellipsoid object) {
        if (object == null || object instanceof DefaultEllipsoid) {
            return (DefaultEllipsoid) object;
        }
        final Map<String,?> properties = IdentifiedObjects.getProperties(object);
        final double        semiMajor  = object.getSemiMajorAxis();
        final Unit<Length>  unit       = object.getAxisUnit();
        return object.isIvfDefinitive() ?
                createFlattenedSphere(properties, semiMajor, object.getInverseFlattening(), unit) :
                createEllipsoid      (properties, semiMajor, object.getSemiMinorAxis(),     unit);
    }

    /**
     * After the unmarshalling process, only one value between {@link #semiMinorAxis} and
     * {@link #inverseFlattening} has been defined. Since the {@link #semiMajorAxis} has
     * been defined, it is now possible to calculate the value of the missing parameter
     * using the values of those that are set.
     *
     * @see #setSemiMajorAxisMeasure(Measure)
     * @see #setSecondDefiningParameter(SecondDefiningParameter)
     */
    private void afterUnmarshal() {
        if (ivfDefinitive) {
            if (semiMinorAxis == 0) {
                semiMinorAxis = semiMajorAxis * (1 - 1/inverseFlattening);
            }
        } else {
            if (inverseFlattening == 0) {
                inverseFlattening = semiMajorAxis / (semiMajorAxis - semiMinorAxis);
            }
        }
        if (unit == null) {
            Measure.missingUOM(DefaultEllipsoid.class, "semiMajorAxis");
        }
    }

    /**
     * Returns the GeoAPI interface implemented by this class.
     * The SIS implementation returns {@code Ellipsoid.class}.
     *
     * <div class="note"><b>Note for implementors:</b>
     * Subclasses usually do not need to override this method since GeoAPI does not define {@code Ellipsoid}
     * sub-interface. Overriding possibility is left mostly for implementors who wish to extend GeoAPI with
     * their own set of interfaces.</div>
     *
     * @return {@code Ellipsoid.class} or a user-defined sub-interface.
     */
    @Override
    public Class<? extends Ellipsoid> getInterface() {
        return Ellipsoid.class;
    }

    /**
     * Returns the linear unit of the {@linkplain #getSemiMajorAxis() semi-major}
     * and {@linkplain #getSemiMinorAxis() semi-minor} axis values.
     *
     * @return The axis linear unit.
     */
    @Override
    public Unit<Length> getAxisUnit() {
        return unit;
    }

    /**
     * Length of the semi-major axis of the ellipsoid.
     * This is the equatorial radius in {@linkplain #getAxisUnit() axis linear unit}.
     *
     * @return Length of semi-major axis.
     */
    @Override
    public double getSemiMajorAxis() {
        return semiMajorAxis;
    }

    /**
     * Returns the semi-major axis value as a measurement.
     * This method is invoked by JAXB for XML marshalling.
     */
    @XmlElement(name = "semiMajorAxis", required = true)
    final Measure getSemiMajorAxisMeasure() {
        return new Measure(semiMajorAxis, unit);
    }

    /**
     * Sets the semi-major axis value.
     * This method is invoked by JAXB at unmarshalling time only.
     *
     * @throws ConversionException If semi-major and semi-minor axes use inconsistent units
     *         and we can not convert from one to the other.
     *
     * @see #setSecondDefiningParameter(SecondDefiningParameter)
     * @see #afterUnmarshal()
     */
    private void setSemiMajorAxisMeasure(final Measure measure) throws ConversionException {
        if (semiMajorAxis != 0) {
            warnDuplicated("semiMajorAxis");
        } else {
            final Unit<Length> uom = unit; // In case semi-minor were defined before semi-major.
            ensureStrictlyPositive("semiMajorAxis", semiMajorAxis = measure.value);
            unit = measure.getUnit(Length.class);
            harmonizeAxisUnits(uom);
            if ((ivfDefinitive ? inverseFlattening : semiMinorAxis) != 0) {
                afterUnmarshal();
            }
        }
    }

    /**
     * Length of the semi-minor axis of the ellipsoid. This is the
     * polar radius in {@linkplain #getAxisUnit() axis linear unit}.
     *
     * @return Length of semi-minor axis.
     */
    @Override
    public double getSemiMinorAxis() {
        return semiMinorAxis;
    }

    /**
     * Returns the radius of a hypothetical sphere having the same surface than this ellipsoid.
     * The radius is expressed in {@linkplain #getAxisUnit() axis linear unit}.
     *
     * @return The radius of a sphere having the same surface than this ellipsoid.
     *
     * @see org.apache.sis.referencing.CommonCRS#SPHERE
     */
    public double getAuthalicRadius() {
        return Formulas.getAuthalicRadius(getSemiMajorAxis(), getSemiMinorAxis());
    }

    /**
     * The ratio of the distance between the center and a focus of the ellipse
     * to the length of its semi-major axis. The eccentricity can alternately be
     * computed from the equation: <code>e = sqrt(2f - f²)</code>.
     *
     * @return The eccentricity of this ellipsoid.
     */
    public double getEccentricity() {
        final double f = 1 - getSemiMinorAxis() / getSemiMajorAxis();
        return sqrt(2*f - f*f);
    }

    /**
     * Returns the value of the inverse of the flattening constant. Flattening is a value
     * used to indicate how closely an ellipsoid approaches a spherical shape. The inverse
     * flattening is related to the equatorial/polar radius by the formula:
     *
     * <blockquote>
     * <var>ivf</var> = <var>r</var><sub>e</sub> / (<var>r</var><sub>e</sub> - <var>r</var><sub>p</sub>).
     * </blockquote>
     *
     * For perfect spheres (i.e. if {@link #isSphere()} returns {@code true}),
     * the {@link Double#POSITIVE_INFINITY} value is used.
     *
     * @return The inverse flattening value.
     */
    @Override
    public double getInverseFlattening() {
        return inverseFlattening;
    }

    /**
     * Indicates if the {@linkplain #getInverseFlattening() inverse flattening} is definitive for
     * this ellipsoid. Some ellipsoids use the IVF as the defining value, and calculate the polar
     * radius whenever asked. Other ellipsoids use the polar radius to calculate the IVF whenever
     * asked. This distinction can be important to avoid floating-point rounding errors.
     *
     * @return {@code true} if the {@linkplain #getInverseFlattening inverse flattening} is definitive,
     *         or {@code false} if the {@linkplain #getSemiMinorAxis() polar radius} is definitive.
     */
    @Override
    public boolean isIvfDefinitive() {
        return ivfDefinitive;
    }

    /**
     * Returns the object to be marshalled as the {@code SecondDefiningParameter} XML element. The
     * returned object contains the values for {@link #semiMinorAxis} or {@link #inverseFlattening},
     * according to the {@link #isIvfDefinitive()} value. This method is for JAXB marshalling only.
     */
    @XmlElement(name = "secondDefiningParameter")
    final SecondDefiningParameter getSecondDefiningParameter() {
        return new SecondDefiningParameter(this, true);
    }

    /**
     * Sets the second defining parameter value, either the inverse of the flattening
     * value or the semi minor axis value, according to what have been defined in the
     * second defining parameter given. This is for JAXB unmarshalling process only.
     *
     * @throws ConversionException If semi-major and semi-minor axes use inconsistent units
     *         and we can not convert from one to the other.
     *
     * @see #setSemiMajorAxisMeasure(Measure)
     * @see #afterUnmarshal()
     */
    private void setSecondDefiningParameter(SecondDefiningParameter second) throws ConversionException {
        while (second.secondDefiningParameter != null) {
            second = second.secondDefiningParameter;
        }
        final Measure measure = second.measure;
        if (measure != null) {
            final boolean isIvfDefinitive = second.isIvfDefinitive();
            if ((isIvfDefinitive ? inverseFlattening : semiMinorAxis) != 0) {
                warnDuplicated("secondDefiningParameter");
            } else {
                ivfDefinitive = isIvfDefinitive;
                double value = measure.value;
                if (isIvfDefinitive) {
                    if (value == 0) {
                        value = Double.POSITIVE_INFINITY;
                    }
                    ensureStrictlyPositive("inverseFlattening", inverseFlattening = value);
                } else if (semiMinorAxis == 0) {
                    ensureStrictlyPositive("semiMinorAxis", semiMinorAxis = value);
                    harmonizeAxisUnits(measure.getUnit(Length.class));
                }
                if (semiMajorAxis != 0) {
                    afterUnmarshal();
                }
            }
        }
    }

    /**
     * Ensures that the semi-minor axis uses the same unit than the semi-major one.
     * The {@link #unit} field shall be set to the semi-major axis unit before this method call.
     *
     * @param  uom The semi-minor axis unit.
     * @throws ConversionException If semi-major and semi-minor axes use inconsistent units
     *         and we can not convert from one to the other.
     */
    private void harmonizeAxisUnits(final Unit<Length> uom) throws ConversionException {
        if (unit == null) {
            unit = uom;
        } else if (uom != null && uom != unit) {
            semiMinorAxis = uom.getConverterToAny(unit).convert(semiMinorAxis);
        }
    }

    /**
     * Emits a warning telling that the given element is repeated twice.
     */
    private static void warnDuplicated(final String element) {
         // We cheat a bit for the "unmarshal" method name since there is not such method...
        Context.warningOccured(Context.current(), DefaultEllipsoid.class, "unmarshal",
                Errors.class, Errors.Keys.DuplicatedElement_1, element);
    }

    /**
     * {@code true} if the ellipsoid is degenerate and is actually a sphere.
     * The sphere is completely defined by the {@linkplain #getSemiMajorAxis() semi-major axis},
     * which is the radius of the sphere.
     *
     * @return {@code true} if the ellipsoid is degenerate and is actually a sphere.
     */
    @Override
    public boolean isSphere() {
        return semiMajorAxis == semiMinorAxis;
    }

    /**
     * Returns the orthodromic distance between two geographic coordinates.
     * The orthodromic distance is the shortest distance between two points on a ellipsoid's surface.
     * The orthodromic path is always on a great circle.
     *
     * <div class="note"><b>Note:</b>
     * Orthodromic distances are different than the <cite>loxodromic distance</cite>.
     * The later is a longer distance on a path with a constant direction on the compass.</div>
     *
     * @param  λ1 Longitude of first  point (in decimal degrees).
     * @param  φ1 Latitude  of first  point (in decimal degrees).
     * @param  λ2 Longitude of second point (in decimal degrees).
     * @param  φ2 Latitude  of second point (in decimal degrees).
     * @return The orthodromic distance (in the units of this ellipsoid's axis).
     */
    public double orthodromicDistance(double λ1, double φ1, double λ2, double φ2) {
        λ1 = toRadians(λ1);
        φ1 = toRadians(φ1);
        λ2 = toRadians(λ2);
        φ2 = toRadians(φ2);
        /*
         * Solution of the geodetic inverse problem after T.Vincenty.
         * Modified Rainsford's method with Helmert's elliptical terms.
         * Effective in any azimuth and at any distance short of antipodal.
         *
         * Latitudes and longitudes in radians positive North and East.
         * Forward azimuths at both points returned in radians from North.
         *
         * Programmed for CDC-6600 by LCDR L.Pfeifer NGS ROCKVILLE MD 18FEB75
         * Modified for IBM SYSTEM 360 by John G.Gergen NGS ROCKVILLE MD 7507
         * Ported from Fortran to Java by Martin Desruisseaux.
         *
         * Source: ftp://ftp.ngs.noaa.gov/pub/pcsoft/for_inv.3d/source/inverse.for
         *         subroutine INVER1
         */
        final double F = 1 / getInverseFlattening();
        final double R = 1 - F;

        double tu1 = R * tan(φ1);
        double tu2 = R * tan(φ2);
        double cu1 = 1 / sqrt(tu1*tu1 + 1);
        double cu2 = 1 / sqrt(tu2*tu2 + 1);
        double su1 = cu1 * tu1;
        double s   = cu1 * cu2;
        double baz =   s * tu2;
        double faz = baz * tu1;
        double x   =  λ2 - λ1;
        for (int i=0; i<MAX_ITERATIONS; i++) {
            final double sx = sin(x);
            final double cx = cos(x);
            tu1 = cu2 * sx;
            tu2 = baz - su1*cu2*cx;
            final double sy = hypot(tu1, tu2);
            final double cy = s*cx + faz;
            final double y = atan2(sy, cy);
            final double SA = s * (sx/sy);
            final double c2a = 1 - SA*SA;
            double cz = 2*faz;
            if (c2a > 0) {
                cz = -cz/c2a + cy;
            }
            double e = cz*cz*2 - 1;
            double c = ((-3*c2a+4)*F + 4) * c2a * F/16;
            double d = x;
            x = ((e*cy*c+cz)*sy*c + y) * SA;
            x = (1-c)*x*F + λ2-λ1;

            if (abs(d-x) <= EPS) {
                x = sqrt((1/(R*R) - 1) * c2a + 1) + 1;
                x = (x-2)/x;
                c = 1-x;
                c = (x*x/4 + 1)/c;
                d = (0.375*x*x - 1)*x;
                x = e*cy;
                s = 1 - 2*e;
                s = ((((sy*sy*4 - 3)*s*cz*d/6-x)*d/4+cz)*sy*d+y)*c*R * getSemiMajorAxis();
                return s;
            }
        }
        // No convergence. It may be because coordinate points
        // are equals or because they are at antipodes.
        if (abs(λ1-λ2) <= COMPARISON_THRESHOLD && abs(φ1-φ2) <= COMPARISON_THRESHOLD) {
            return 0; // Coordinate points are equals
        }
        if (abs(φ1) <= COMPARISON_THRESHOLD && abs(φ2) <= COMPARISON_THRESHOLD) {
            return abs(λ1-λ2) * getSemiMajorAxis(); // Points are on the equator.
        }
        // At least one input ordinate is NaN.
        if (isNaN(λ1) || isNaN(φ1) || isNaN(λ2) || isNaN(φ2)) {
            return NaN;
        }
        // Other cases: no solution for this algorithm.
        throw new ArithmeticException(Errors.format(Errors.Keys.NoConvergenceForPoints_2,
                  new DirectPosition2D(toDegrees(λ1), toDegrees(φ1)),
                  new DirectPosition2D(toDegrees(λ2), toDegrees(φ2))));
    }

    /**
     * Compares this ellipsoid with the specified object for equality.
     *
     * @param  object The object to compare to {@code this}.
     * @param  mode {@link ComparisonMode#STRICT STRICT} for performing a strict comparison, or
     *         {@link ComparisonMode#IGNORE_METADATA IGNORE_METADATA} for comparing only properties
     *         relevant to coordinate transformations.
     * @return {@code true} if both objects are equal.
     */
    @Override
    @SuppressWarnings("fallthrough")
    public boolean equals(final Object object, final ComparisonMode mode) {
        if (object == this) {
            return true; // Slight optimization.
        }
        if (!super.equals(object, mode)) {
            return false;
        }
        switch (mode) {
            case STRICT: {
                final DefaultEllipsoid that = (DefaultEllipsoid) object;
                return ivfDefinitive == that.ivfDefinitive &&
                       Numerics.equals(this.semiMajorAxis,     that.semiMajorAxis)     &&
                       Numerics.equals(this.semiMinorAxis,     that.semiMinorAxis)     &&
                       Numerics.equals(this.inverseFlattening, that.inverseFlattening) &&
                        Objects.equals(this.unit,              that.unit);
            }
            case BY_CONTRACT: {
                /*
                 * isIvfDefinitive has no incidence on calculation using ellipsoid parameters,
                 * so we consider it as metadata that can be ignored in IGNORE_METADATA mode.
                 */
                if (isIvfDefinitive() != ((Ellipsoid) object).isIvfDefinitive()) {
                    return false;
                }
                // Fall through
            }
            default: {
                final Ellipsoid that = (Ellipsoid) object;
                return epsilonEqual(getSemiMajorAxis(),     that.getSemiMajorAxis(),     mode) &&
                       epsilonEqual(getSemiMinorAxis(),     that.getSemiMinorAxis(),     mode) &&
                       epsilonEqual(getInverseFlattening(), that.getInverseFlattening(), mode) &&
                       Objects.equals(getAxisUnit(),        that.getAxisUnit());
            }
        }
    }

    /**
     * Invoked by {@code hashCode()} for computing the hash code when first needed.
     * See {@link org.apache.sis.referencing.AbstractIdentifiedObject#computeHashCode()}
     * for more information.
     *
     * @return The hash code value. This value may change in any future Apache SIS version.
     */
    @Override
    protected long computeHashCode() {
        return super.computeHashCode() + Double.doubleToLongBits(semiMajorAxis) +
               31 * Double.doubleToLongBits(ivfDefinitive ? inverseFlattening : semiMinorAxis);
    }

    /**
     * Formats this ellipsoid as a <cite>Well Known Text</cite> {@code Ellipsoid[…]} element.
     *
     * @return {@code "Ellipsoid"} (WKT 2) or {@code "Spheroid"} (WKT 1).
     */
    @Override
    protected String formatTo(final Formatter formatter) {
        super.formatTo(formatter);
        final Convention convention = formatter.getConvention();
        final boolean isWKT1 = convention.majorVersion() == 1;
        double length = semiMajorAxis;
        if (isWKT1) {
            length = unit.getConverterTo(SI.METRE).convert(length);
        }
        formatter.append(length);
        formatter.append(isInfinite(inverseFlattening) ? 0 : inverseFlattening);
        if (isWKT1) {
            return "Spheroid";
        }
        if (!convention.isSimplified() || !SI.METRE.equals(unit)) {
            formatter.append(unit);
        }
        return "Ellipsoid";
    }
}
