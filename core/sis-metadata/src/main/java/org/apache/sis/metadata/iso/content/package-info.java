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

/**
 * Description of the dataset content.
 * An explanation for this package is provided in the {@linkplain org.opengis.metadata.content OpenGIS® javadoc}.
 * The remaining discussion on this page is specific to the SIS implementation.
 *
 * {@section Overview}
 * For a global overview of metadata in SIS, see the {@link org.apache.sis.metadata} package javadoc.
 *
 * <table class="sis">
 * <caption>Package overview</caption>
 * <tr>
 *   <th>Class hierarchy</th>
 *   <th class="sep">Aggregation hierarchy</th>
 * </tr><tr><td style="width: 50%; white-space: nowrap">
 * {@linkplain org.apache.sis.metadata.iso.ISOMetadata ISO-19115 metadata}<br>
 * {@code  ├─}         {@linkplain org.apache.sis.metadata.iso.content.AbstractContentInformation         Content information} «abstract»<br>
 * {@code  │   ├─}                                                                                        Feature catalogue<br>
 * {@code  │   ├─}     {@linkplain org.apache.sis.metadata.iso.content.DefaultFeatureCatalogueDescription Feature catalogue description}<br>
 * {@code  │   └─}     {@linkplain org.apache.sis.metadata.iso.content.DefaultCoverageDescription         Coverage description}<br>
 * {@code  │       └─} {@linkplain org.apache.sis.metadata.iso.content.DefaultImageDescription            Image description}<br>
 * {@code  ├─}         {@linkplain org.apache.sis.metadata.iso.content.DefaultFeatureTypeInfo             Feature type info}<br>
 * {@code  ├─}         {@linkplain org.apache.sis.metadata.iso.content.DefaultRangeDimension              Range dimension}<br>
 * {@code  │   └─}     {@linkplain org.apache.sis.metadata.iso.content.DefaultSampleDimension             Sample dimension}<br>
 * {@code  │       └─} {@linkplain org.apache.sis.metadata.iso.content.DefaultBand                        Band}<br>
 * {@code  └─}         {@linkplain org.apache.sis.metadata.iso.content.DefaultRangeElementDescription     Range element description}<br>
 * {@linkplain org.opengis.util.CodeList Code list}<br>
 * {@code  ├─} {@linkplain org.opengis.metadata.content.BandDefinition          Band definition}<br>
 * {@code  ├─} {@linkplain org.opengis.metadata.content.CoverageContentType     Coverage content type}<br>
 * {@code  ├─} {@linkplain org.opengis.metadata.content.ImagingCondition        Imaging condition}<br>
 * {@code  ├─} {@linkplain org.opengis.metadata.content.PolarizationOrientation Polarization orientation}<br>
 * {@code  └─} {@linkplain org.opengis.metadata.content.TransferFunctionType    Transfer function type}<br>
 * </td><td class="sep" style="width: 50%; white-space: nowrap">
 *                 {@linkplain org.apache.sis.metadata.iso.content.AbstractContentInformation         Content information} «abstract»<br>
 *                 {@linkplain org.apache.sis.metadata.iso.content.DefaultFeatureCatalogueDescription Feature catalogue description}<br>
 * {@code  └─}     {@linkplain org.apache.sis.metadata.iso.content.DefaultFeatureTypeInfo             Feature type info}<br>
 *                 {@linkplain org.apache.sis.metadata.iso.content.DefaultCoverageDescription         Coverage description}<br>
 * {@code  ├─}     {@linkplain org.apache.sis.metadata.iso.content.DefaultAttributeGroup              Attribute group}<br>
 * {@code  │   ├─} {@linkplain org.opengis.metadata.content.CoverageContentType                       Coverage content type} «code list»<br>
 * {@code  │   └─} {@linkplain org.apache.sis.metadata.iso.content.DefaultRangeDimension              Range dimension}<br>
 * {@code  └─}     {@linkplain org.apache.sis.metadata.iso.content.DefaultRangeElementDescription     Range element description}<br>
 *                 {@linkplain org.apache.sis.metadata.iso.content.DefaultBand                        Band}<br>
 * {@code  ├─}     {@linkplain org.opengis.metadata.content.BandDefinition                            Band definition} «code list»<br>
 * {@code  ├─}     {@linkplain org.opengis.metadata.content.PolarizationOrientation                   Polarization orientation} «code list»<br>
 * {@code  └─}     {@linkplain org.opengis.metadata.content.TransferFunctionType                      Transfer function type} «code list»<br>
 *                 {@linkplain org.apache.sis.metadata.iso.content.DefaultImageDescription            Image description}<br>
 * {@code  └─}     {@linkplain org.opengis.metadata.content.ImagingCondition                          Imaging condition} «code list»<br>
 * </td></tr></table>
 *
 * {@section Bands in gridded data}
 * ISO 19115 defines a {@link org.opengis.metadata.content.Band} interface
 * which expresses the range of wavelengths in the electromagnetic spectrum.
 * For the needs of Image I/O, an additional interface has been defined with a subset
 * of the {@code Band} API and the restriction to electromagnetic spectrum removed.
 * That interface is named {@link org.apache.sis.image.io.metadata.SampleDimension}.
 * Both {@code Band} and {@code SampleDimension} interfaces extend the same parent,
 * {@link org.opengis.metadata.content.RangeDimension}.
 *
 * {@section Null values, nil objects and collections}
 * All constructors and setter methods accept {@code null} arguments.
 * A null argument value means that the metadata element can not be provided, and the reason for that is unspecified.
 * Alternatively, users can specify why a metadata element is missing by providing a value created by
 * {@link org.apache.sis.xml.NilReason#createNilObject NilReason.createNilObject(Class)}.
 *
 * <p>Unless otherwise noted in the Javadoc, all getter methods may return an empty collection,
 * an empty array or {@code null} if the type is neither a collection or an array.
 * Note that non-null values may be {@link org.apache.sis.xml.NilObject}s.</p>
 *
 * <p>Unless the metadata object has been marked as unmodifiable and unless otherwise noted in the Javadoc,
 * all collections returned by getter methods are <cite>live</cite>: adding new elements in the collection
 * modify directly the underlying metadata object.</p>
 *
 * @author  Martin Desruisseaux (IRD, Geomatys)
 * @author  Touraïvane (IRD)
 * @author  Cédric Briançon (Geomatys)
 * @since   0.3 (derived from geotk-2.1)
 * @version 0.5
 * @module
 */
@XmlSchema(elementFormDefault = XmlNsForm.QUALIFIED, namespace = Namespaces.GMD, xmlns = {
    @XmlNs(prefix = "gmi", namespaceURI = Namespaces.GMI),
    @XmlNs(prefix = "gmd", namespaceURI = Namespaces.GMD),
    @XmlNs(prefix = "gco", namespaceURI = Namespaces.GCO),
    @XmlNs(prefix = "xsi", namespaceURI = Namespaces.XSI)
})
@XmlAccessorType(XmlAccessType.NONE)
@XmlJavaTypeAdapters({
    @XmlJavaTypeAdapter(CI_Citation.class),
    @XmlJavaTypeAdapter(MD_CoverageContentTypeCode.class),
    @XmlJavaTypeAdapter(MD_Identifier.class),
    @XmlJavaTypeAdapter(MD_ImagingConditionCode.class),
    @XmlJavaTypeAdapter(MD_RangeDimension.class),
    @XmlJavaTypeAdapter(MI_BandDefinition.class),
    @XmlJavaTypeAdapter(MI_PolarizationOrientationCode.class),
    @XmlJavaTypeAdapter(MI_RangeElementDescription.class),
    @XmlJavaTypeAdapter(MI_TransferFunctionTypeCode.class),

    // Java types, primitive types and basic OGC types handling
    @XmlJavaTypeAdapter(UnitAdapter.class),
    @XmlJavaTypeAdapter(LocaleAdapter.class),
    @XmlJavaTypeAdapter(InternationalStringAdapter.class),
    @XmlJavaTypeAdapter(GO_GenericName.class),
    @XmlJavaTypeAdapter(GO_RecordType.class),
    @XmlJavaTypeAdapter(GO_Boolean.class), @XmlJavaTypeAdapter(type=boolean.class, value=GO_Boolean.class),
    @XmlJavaTypeAdapter(GO_Integer.class), @XmlJavaTypeAdapter(type=int.class,     value=GO_Integer.class),
    @XmlJavaTypeAdapter(GO_Real.class),    @XmlJavaTypeAdapter(type=double.class,  value=GO_Real.class)
})
package org.apache.sis.metadata.iso.content;

import javax.xml.bind.annotation.XmlNs;
import javax.xml.bind.annotation.XmlNsForm;
import javax.xml.bind.annotation.XmlSchema;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapters;
import org.apache.sis.xml.Namespaces;
import org.apache.sis.internal.jaxb.gco.*;
import org.apache.sis.internal.jaxb.gmd.*;
import org.apache.sis.internal.jaxb.code.*;
import org.apache.sis.internal.jaxb.metadata.*;
