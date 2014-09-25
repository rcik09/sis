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

import java.util.Date;
import org.opengis.test.Validators;
import org.opengis.util.FactoryException;
import org.opengis.referencing.crs.SingleCRS;
import org.opengis.referencing.crs.TemporalCRS;
import org.opengis.referencing.crs.VerticalCRS;
import org.opengis.referencing.crs.GeographicCRS;
import org.opengis.referencing.crs.GeocentricCRS;
import org.opengis.referencing.cs.AxisDirection;
import org.opengis.referencing.cs.CoordinateSystem;
import org.opengis.referencing.cs.EllipsoidalCS;
import org.opengis.referencing.datum.TemporalDatum;
import org.opengis.referencing.datum.VerticalDatum;
import org.opengis.referencing.datum.VerticalDatumType;
import org.apache.sis.internal.referencing.VerticalDatumTypes;
import org.apache.sis.test.DependsOnMethod;
import org.apache.sis.test.DependsOn;
import org.apache.sis.test.TestCase;
import org.junit.Test;

import static org.junit.Assert.*;
import static org.apache.sis.test.TestUtilities.*;


/**
 * Tests the {@link CommonCRS} class.
 *
 * @author  Martin Desruisseaux (IRD, Geomatys)
 * @since   0.4 (derived from geotk-2.2)
 * @version 0.5
 * @module
 */
@DependsOn({
  org.apache.sis.referencing.crs.DefaultGeodeticCRSTest.class,
  org.apache.sis.referencing.datum.DefaultVerticalDatumTest.class,
  StandardDefinitionsTest.class
})
public final strictfp class CommonCRSTest extends TestCase {
    /**
     * Length of a day in milliseconds.
     */
    private static final double DAY_LENGTH = 24 * 60 * 60 * 1000;

    /**
     * Tests the {@link CommonCRS#geographic()} method.
     */
    @Test
    public void testGeographic() {
        final GeographicCRS geographic = CommonCRS.WGS84.geographic();
        Validators.validate(geographic);
        GeodeticObjectVerifier.assertIsWGS84(geographic, true, true);
        assertSame("Cached value", geographic, CommonCRS.WGS84.geographic());
    }

    /**
     * Tests the {@link CommonCRS#normalizedGeographic()} method.
     */
    @Test
    @DependsOnMethod("testGeographic")
    public void testNormalizedGeographic() {
        final GeographicCRS geographic = CommonCRS.WGS84.geographic();
        final GeographicCRS normalized = CommonCRS.WGS84.normalizedGeographic();
        Validators.validate(normalized);
        assertSame(geographic.getDatum(), normalized.getDatum());

        final CoordinateSystem φλ = geographic.getCoordinateSystem();
        final CoordinateSystem λφ = normalized.getCoordinateSystem();
        assertSame("Longitude", φλ.getAxis(1), λφ.getAxis(0));
        assertSame("Latitude",  φλ.getAxis(0), λφ.getAxis(1));
        assertSame("Cached value", normalized, CommonCRS.WGS84.normalizedGeographic());
    }

    /**
     * Tests the {@link CommonCRS#geographic3D()} method.
     */
    @Test
    @DependsOnMethod("testGeographic")
    public void testGeographic3D() {
        final GeographicCRS crs = CommonCRS.WGS72.geographic3D();
        Validators.validate(crs);
        assertEquals("WGS 72", crs.getName().getCode());
        assertSame   (CommonCRS.WGS72.geographic().getDatum(), crs.getDatum());
        assertNotSame(CommonCRS.WGS84.geographic().getDatum(), crs.getDatum());

        final EllipsoidalCS cs = crs.getCoordinateSystem();
        final String name = cs.getName().getCode();
        assertTrue(name, name.startsWith("Ellipsoidal 3D"));
        assertEquals("dimension", 3, cs.getDimension());
        assertEquals(AxisDirection.NORTH, cs.getAxis(0).getDirection());
        assertEquals(AxisDirection.EAST,  cs.getAxis(1).getDirection());
        assertEquals(AxisDirection.UP,    cs.getAxis(2).getDirection());
        assertSame("Cached value", crs, CommonCRS.WGS72.geographic3D());
    }

    /**
     * Tests the {@link CommonCRS#geocentric()} method.
     */
    @Test
    @DependsOnMethod("testGeographic3D")
    public void testGeocentric() {
        final GeocentricCRS crs = CommonCRS.WGS72.geocentric();
        Validators.validate(crs);
        assertEquals("WGS 72", crs.getName().getCode());
        assertSame   (CommonCRS.WGS72.geographic().getDatum(), crs.getDatum());
        assertNotSame(CommonCRS.WGS84.geographic().getDatum(), crs.getDatum());

        final CoordinateSystem cs = crs.getCoordinateSystem();
        final String name = cs.getName().getCode();
        assertTrue(name, name.startsWith("Earth centred"));
        assertEquals("dimension", 3, cs.getDimension());
        assertEquals(AxisDirection.GEOCENTRIC_X, cs.getAxis(0).getDirection());
        assertEquals(AxisDirection.GEOCENTRIC_Y, cs.getAxis(1).getDirection());
        assertEquals(AxisDirection.GEOCENTRIC_Z, cs.getAxis(2).getDirection());
        assertSame("Cached value", crs, CommonCRS.WGS72.geocentric());
    }

    /**
     * Verifies the vertical datum enumeration.
     */
    @Test
    public void testVertical() {
        for (final CommonCRS.Vertical e : CommonCRS.Vertical.values()) {
            final VerticalDatumType datumType;
            final String axisName, datumName;
            switch (e) {
                case BAROMETRIC:     axisName = "Barometric altitude";    datumName = "Constant pressure surface"; datumType = VerticalDatumType. BAROMETRIC;    break;
                case MEAN_SEA_LEVEL: axisName = "Gravity-related height"; datumName = "Mean Sea Level";            datumType = VerticalDatumType. GEOIDAL;       break;
                case DEPTH:          axisName = "Depth";                  datumName = "Mean Sea Level";            datumType = VerticalDatumType. GEOIDAL;       break;
                case ELLIPSOIDAL:    axisName = "Ellipsoidal height";     datumName = "Ellipsoid";                 datumType = VerticalDatumTypes.ELLIPSOIDAL;   break;
                case OTHER_SURFACE:  axisName = "Height";                 datumName = "Other surface";             datumType = VerticalDatumType. OTHER_SURFACE; break;
                default: throw new AssertionError(e);
            }
            final String        name  = e.name();
            final VerticalDatum datum = e.datum();
            final VerticalCRS   crs   = e.crs();
            if (e.isEPSG) {
                /*
                 * BAROMETRIC, ELLIPSOIDAL and OTHER_SURFACE uses an axis named "Height", which is not
                 * a valid axis name according ISO 19111. We skip the validation test for those enums.
                 */
                Validators.validate(crs);
            }
            assertSame  (name, datum,          e.datum()); // Datum before CRS creation.
            assertSame  (name, crs.getDatum(), e.datum()); // Datum after CRS creation.
            assertEquals(name, datumName, datum.getName().getCode());
            assertEquals(name, datumType, datum.getVerticalDatumType());
            assertEquals(name, axisName,  crs.getCoordinateSystem().getAxis(0).getName().getCode());
        }
    }

    /**
     * Verifies the epoch values of temporal enumeration compared to the Julian epoch.
     *
     * @see <a href="http://en.wikipedia.org/wiki/Julian_day">Wikipedia: Julian day</a>
     */
    @Test
    public void testTemporal() {
        final double julianEpoch = CommonCRS.Temporal.JULIAN.datum().getOrigin().getTime() / DAY_LENGTH;
        assertTrue(julianEpoch < 0);
        for (final CommonCRS.Temporal e : CommonCRS.Temporal.values()) {
            final String epoch;
            final double days;
            switch (e) {
                case JAVA:             // Fall through
                case UNIX:             epoch = "1970-01-01 00:00:00"; days = 2440587.5; break;
                case TRUNCATED_JULIAN: epoch = "1968-05-24 00:00:00"; days = 2440000.5; break;
                case DUBLIN_JULIAN:    epoch = "1899-12-31 12:00:00"; days = 2415020.0; break;
                case MODIFIED_JULIAN:  epoch = "1858-11-17 00:00:00"; days = 2400000.5; break;
                case JULIAN:           epoch = "4713-01-01 12:00:00"; days = 0;         break;
                default: throw new AssertionError(e);
            }
            final String        name   = e.name();
            final TemporalDatum datum  = e.datum();
            final TemporalCRS   crs    = e.crs();
            final Date          origin = datum.getOrigin();
            Validators.validate(crs);
            assertSame  (name, datum,          e.datum()); // Datum before CRS creation.
            assertSame  (name, crs.getDatum(), e.datum()); // Datum after CRS creation.
            assertEquals(name, epoch, format(origin));
            assertEquals(name, days, origin.getTime() / DAY_LENGTH - julianEpoch, 0);
        }
    }

    /**
     * Tests {@link CommonCRS#forCode(String, String, FactoryException)}.
     *
     * @throws FactoryException If a CRS can not be constructed.
     *
     * @see CRSTest#testForEpsgCode()
     * @see CRSTest#testForCrsCode()
     *
     * @since 0.5
     */
    @Test
    public void testForCode() throws FactoryException {
        verifyForCode(CommonCRS.WGS84 .geographic(),            "EPSG", "4326");
        verifyForCode(CommonCRS.WGS72 .geographic(),            "EPSG", "4322");
        verifyForCode(CommonCRS.SPHERE.geographic(),            "EPSG", "4047");
        verifyForCode(CommonCRS.NAD83 .geographic(),            "EPSG", "4269");
        verifyForCode(CommonCRS.NAD27 .geographic(),            "EPSG", "4267");
        verifyForCode(CommonCRS.ETRS89.geographic(),            "EPSG", "4258");
        verifyForCode(CommonCRS.ED50  .geographic(),            "EPSG", "4230");
        verifyForCode(CommonCRS.WGS84 .geocentric(),            "EPSG", "4978");
        verifyForCode(CommonCRS.WGS72 .geocentric(),            "EPSG", "4984");
        verifyForCode(CommonCRS.ETRS89.geocentric(),            "EPSG", "4936");
        verifyForCode(CommonCRS.WGS84 .geographic3D(),          "EPSG", "4979");
        verifyForCode(CommonCRS.WGS72 .geographic3D(),          "EPSG", "4985");
        verifyForCode(CommonCRS.ETRS89.geographic3D(),          "EPSG", "4937");
        verifyForCode(CommonCRS.Vertical.MEAN_SEA_LEVEL.crs(),  "EPSG", "5714");
        verifyForCode(CommonCRS.Vertical.DEPTH.crs(),           "EPSG", "5715");
        verifyForCode(CommonCRS.WGS84.normalizedGeographic(),   "CRS",  "84");
        verifyForCode(CommonCRS.NAD83.normalizedGeographic(),   "CRS",  "83");
        verifyForCode(CommonCRS.NAD27.normalizedGeographic(),   "CRS",  "27");
    }

    /**
     * Asserts that the result of {@link CommonCRS#forCode(String, String, FactoryException)} is the given CRS.
     */
    private static void verifyForCode(final SingleCRS expected, final String authority, final String code) throws FactoryException {
        assertSame(code, expected, CommonCRS.forCode(authority, code, null));
    }
}
