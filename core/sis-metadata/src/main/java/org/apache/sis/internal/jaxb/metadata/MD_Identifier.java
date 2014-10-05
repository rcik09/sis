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
package org.apache.sis.internal.jaxb.metadata;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementRef;
import org.opengis.metadata.Identifier;
import org.opengis.referencing.ReferenceIdentifier;
import org.apache.sis.metadata.iso.DefaultIdentifier;
import org.apache.sis.metadata.iso.ImmutableIdentifier;
import org.apache.sis.internal.jaxb.gco.PropertyType;


/**
 * JAXB adapter mapping implementing class to the GeoAPI interface. See
 * package documentation for more information about JAXB and interface.
 *
 * @author  Cédric Briançon (Geomatys)
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.3 (derived from geotk-2.5)
 * @version 0.5
 * @module
 */
public final class MD_Identifier extends PropertyType<MD_Identifier, Identifier> {
    /**
     * Empty constructor for JAXB only.
     */
    public MD_Identifier() {
    }

    /**
     * Returns the GeoAPI interface which is bound by this adapter.
     * This method is indirectly invoked by the private constructor
     * below, so it shall not depend on the state of this object.
     *
     * @return {@code Identifier.class}
     */
    @Override
    protected Class<Identifier> getBoundType() {
        return Identifier.class;
    }

    /**
     * Constructor for the {@link #wrap} method only.
     */
    private MD_Identifier(final Identifier metadata) {
        super(metadata);
    }

    /**
     * Invoked by {@link PropertyType} at marshalling time for wrapping the given metadata value
     * in a {@code <gmd:MD_Identifier>} XML element.
     *
     * @param  metadata The metadata element to marshall.
     * @return A {@code PropertyType} wrapping the given the metadata element.
     */
    @Override
    protected MD_Identifier wrap(final Identifier metadata) {
        return new MD_Identifier(metadata);
    }

    /**
     * Returns {@code true} if the identifier should be marshalled as a
     * {@code RS_Identifier} instead than {@code MD_Identifier}.
     */
    private boolean isRS() {
        return (metadata instanceof ReferenceIdentifier) || (metadata instanceof ImmutableIdentifier);
    }

    /**
     * Invoked by JAXB at marshalling time for getting the actual metadata to write
     * inside the {@code <gmd:MD_Identifier>} XML element.
     * This is the value or a copy of the value given in argument to the {@code wrap} method.
     *
     * @return The metadata to be marshalled.
     */
    @XmlElementRef
    public DefaultIdentifier getElement() {
        return isRS() ? null : DefaultIdentifier.castOrCopy(metadata);
    }

    /**
     * Invoked by JAXB at unmarshalling time for storing the result temporarily.
     *
     * @param metadata The unmarshalled metadata.
     */
    public void setElement(final DefaultIdentifier metadata) {
        this.metadata = metadata;
    }

    /**
     * An alternative to {@link #getElement()} when the metadata is actually
     * an instance of {@link ReferenceIdentifier}. In such case, the enclosing
     * XML element will be {@code RS_Identifier} instead of {@code MD_Identifier}.
     *
     * @return The metadata to be marshalled.
     */
    @XmlElement(name = "RS_Identifier")
    public ImmutableIdentifier getReferenceIdentifier() {
        return isRS() ? ImmutableIdentifier.castOrCopy(metadata) : null;
    }

    /**
     * Invoked by JAXB at unmarshalling time for storing the result temporarily.
     *
     * @param metadata The unmarshalled metadata.
     */
    public void setReferenceIdentifier(final ImmutableIdentifier metadata) {
        this.metadata = metadata;
    }
}
