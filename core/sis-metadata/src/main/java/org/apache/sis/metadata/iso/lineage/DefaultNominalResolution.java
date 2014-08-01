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
package org.apache.sis.metadata.iso.lineage;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;
import org.opengis.metadata.lineage.NominalResolution;
import org.apache.sis.metadata.iso.ISOMetadata;
import org.apache.sis.measure.ValueRange;
import org.apache.sis.xml.Namespaces;


/**
 * Distance between consistent parts of (centre, left side, right side) adjacent pixels.
 *
 * <p><b>Limitations:</b></p>
 * <ul>
 *   <li>Instances of this class are not synchronized for multi-threading.
 *       Synchronization, if needed, is caller's responsibility.</li>
 *   <li>Serialized objects of this class are not guaranteed to be compatible with future Apache SIS releases.
 *       Serialization support is appropriate for short term storage or RMI between applications running the
 *       same version of Apache SIS. For long term storage, use {@link org.apache.sis.xml.XML} instead.</li>
 * </ul>
 *
 * @author  Cédric Briançon (Geomatys)
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.3 (derived from geotk-3.03)
 * @version 0.3
 * @module
 */
@XmlType(name = "LE_NominalResolution_Type", propOrder = {
    "scanningResolution",
    "groundResolution"
})
@XmlRootElement(name = "LE_NominalResolution", namespace = Namespaces.GMI)
public class DefaultNominalResolution extends ISOMetadata implements NominalResolution {
    /**
     * Serial number for inter-operability with different versions.
     */
    private static final long serialVersionUID = -4000422414866855607L;

    /**
     * Distance between consistent parts of (centre, left side, right side) adjacent pixels
     * in the scan plane.
     */
    private Double scanningResolution;

    /**
     * Distance between consistent parts of (centre, left side, right side) adjacent pixels
     * in the object space.
     */
    private Double groundResolution;

    /**
     * Constructs an initially empty nominal resolution.
     */
    public DefaultNominalResolution() {
    }

    /**
     * Constructs a new instance initialized with the values from the specified metadata object.
     * This is a <cite>shallow</cite> copy constructor, since the other metadata contained in the
     * given object are not recursively copied.
     *
     * @param object The metadata to copy values from, or {@code null} if none.
     *
     * @see #castOrCopy(NominalResolution)
     */
    public DefaultNominalResolution(final NominalResolution object) {
        super(object);
        if (object != null) {
            scanningResolution = object.getScanningResolution();
            groundResolution   = object.getGroundResolution();
        }
    }

    /**
     * Returns a SIS metadata implementation with the values of the given arbitrary implementation.
     * This method performs the first applicable action in the following choices:
     *
     * <ul>
     *   <li>If the given object is {@code null}, then this method returns {@code null}.</li>
     *   <li>Otherwise if the given object is already an instance of
     *       {@code DefaultNominalResolution}, then it is returned unchanged.</li>
     *   <li>Otherwise a new {@code DefaultNominalResolution} instance is created using the
     *       {@linkplain #DefaultNominalResolution(NominalResolution) copy constructor}
     *       and returned. Note that this is a <cite>shallow</cite> copy operation, since the other
     *       metadata contained in the given object are not recursively copied.</li>
     * </ul>
     *
     * @param  object The object to get as a SIS implementation, or {@code null} if none.
     * @return A SIS implementation containing the values of the given object (may be the
     *         given object itself), or {@code null} if the argument was null.
     */
    public static DefaultNominalResolution castOrCopy(final NominalResolution object) {
        if (object == null || object instanceof DefaultNominalResolution) {
            return (DefaultNominalResolution) object;
        }
        return new DefaultNominalResolution(object);
    }

    /**
     * Returns the distance between consistent parts of (centre, left side, right side)
     * adjacent pixels in the scan plane.
     *
     * @return Distance between consistent parts of adjacent pixels in the scan plane, or {@code null}.
     */
    @Override
    @ValueRange(minimum=0, isMinIncluded=false)
    @XmlElement(name = "scanningResolution", namespace = Namespaces.GMI, required = true)
    public Double getScanningResolution() {
        return scanningResolution;
    }

    /**
     * Sets the distance between consistent parts of (centre, left side, right side) adjacent
     * pixels in the scan plane.
     *
     * @param newValue The new scanning resolution value.
     */
    public void setScanningResolution(final Double newValue) {
        checkWritePermission();
        scanningResolution = newValue;
    }

    /**
     * Returns the distance between consistent parts of (centre, left side, right side) adjacent
     * pixels in the object space.
     *
     * @return Distance between consistent parts of adjacent pixels in the object space, or {@code null}.
     */
    @Override
    @ValueRange(minimum=0, isMinIncluded=false)
    @XmlElement(name = "groundResolution", namespace = Namespaces.GMI, required = true)
    public Double getGroundResolution() {
        return groundResolution;
    }

    /**
     * Sets the distance between consistent parts of (centre, left side, right side) adjacent pixels
     * in the object space.
     *
     * @param newValue The new ground resolution value.
     */
    public void setGroundResolution(final Double newValue) {
        checkWritePermission();
        groundResolution = newValue;
    }
}
