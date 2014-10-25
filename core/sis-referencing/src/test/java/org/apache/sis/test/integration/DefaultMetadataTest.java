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
package org.apache.sis.test.integration;

import java.net.URI;
import java.net.URL;
import java.util.Arrays;
import java.util.Locale;
import java.io.StringWriter;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import javax.xml.bind.JAXBException;
import javax.measure.unit.SI;

import org.opengis.metadata.*;
import org.opengis.metadata.citation.*;
import org.opengis.metadata.constraint.*;
import org.opengis.metadata.identification.*;
import org.opengis.metadata.maintenance.*;
import org.opengis.metadata.spatial.*;
import org.opengis.referencing.cs.AxisDirection;
import org.opengis.referencing.datum.VerticalDatumType;

import org.apache.sis.metadata.iso.*;
import org.apache.sis.metadata.iso.citation.*;
import org.apache.sis.metadata.iso.constraint.*;
import org.apache.sis.metadata.iso.content.*;
import org.apache.sis.metadata.iso.distribution.*;
import org.apache.sis.metadata.iso.extent.*;
import org.apache.sis.metadata.iso.identification.*;
import org.apache.sis.metadata.iso.spatial.*;
import org.apache.sis.referencing.datum.DefaultVerticalDatum;
import org.apache.sis.referencing.cs.DefaultCoordinateSystemAxis;
import org.apache.sis.referencing.cs.DefaultVerticalCS;
import org.apache.sis.referencing.crs.DefaultVerticalCRS;
import org.apache.sis.internal.jaxb.metadata.replace.ReferenceSystemMetadata;
import org.apache.sis.internal.jaxb.gmx.Anchor;
import org.apache.sis.referencing.NamedIdentifier;
import org.apache.sis.util.iso.SimpleInternationalString;
import org.apache.sis.util.ComparisonMode;
import org.apache.sis.xml.Namespaces;
import org.apache.sis.xml.MarshallerPool;
import org.apache.sis.test.TestUtilities;
import org.apache.sis.test.XMLComparator;
import org.apache.sis.test.XMLTestCase;
import org.apache.sis.test.DependsOn;
import org.junit.Test;

import static org.apache.sis.test.Assert.*;
import static java.util.Collections.singleton;
import static java.util.Collections.singletonMap;

// Branch-dependent imports
import org.apache.sis.internal.jdk7.StandardCharsets;


/**
 * Tests XML (un)marshalling of a metadata object containing various elements
 * in addition to Coordinate Reference System (CRS) elements.
 *
 * @author  Guilhem Legal (Geomatys)
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.5
 * @version 0.5
 * @module
 *
 * @see org.apache.sis.metadata.iso.DefaultMetadataTest
 */
@DependsOn(ReferencingInMetadataTest.class)
public strictfp class DefaultMetadataTest extends XMLTestCase {
    /**
     * Sets the temporal extent. The current implementation does nothing, because {@code sis-metadata} does not have
     * any dependency to {@code sis-temporal}. However a future version or an other module may implement this method.
     *
     * @param extent    The extent to set.
     * @param startTime The start time in the {@code "yyy-mm-dd"} format.
     * @param endTime   The end time in the {@code "yyy-mm-dd"} format.
     */
    protected void setTemporalBounds(final DefaultTemporalExtent extent, final String startTime, final String endTime) {
        /*
         * Note: if this DefaultMetadataTest class is made final and this method removed,
         *       then testUnmarshalling() can be simplified.
         */
    }

    /**
     * Creates a telephone number of the given type.
     *
     * @param type Either {@code "VOICE"}, {@code "FACSIMILE"} or {@code "SMS"}.
     */
    private static DefaultTelephone telephone(final String number, final String type) {
        final DefaultTelephone tel = new DefaultTelephone();
        tel.setNumber(number);
        tel.setNumberType(type);
        return tel;
    }

    /**
     * Programmatically creates the metadata to marshall, or to compare against the unmarshalled metadata.
     *
     * @return The hard-coded representation of {@code "Metadata.xml"} content.
     */
    private DefaultMetadata createHardCoded() {
        final DefaultMetadata metadata = new DefaultMetadata();
        metadata.setMetadataIdentifier(new DefaultIdentifier("Apache SIS/Metadata test"));
        metadata.setLanguages(singleton(Locale.ENGLISH));
        metadata.setCharacterSets(singleton(StandardCharsets.UTF_8));
        metadata.setMetadataScopes(singleton(new DefaultMetadataScope(ScopeCode.DATASET, "Common Data Index record")));
        metadata.setDates(singleton(new DefaultCitationDate(TestUtilities.date("2009-01-01 04:00:00"), DateType.CREATION)));
        /*
         * Contact information for the author. The same party will be used for custodian and distributor,
         * with only the role changed. Note that we need to create an instance of the deprecated class,
         * because this is what will be unmarshalled from the XML document.
         */
        @SuppressWarnings("deprecation")
        final DefaultResponsibleParty author = new DefaultResponsibleParty(Role.AUTHOR);
        final Anchor country = new Anchor(URI.create("SDN:C320:2:FR"), "France"); // Non-public SIS class.
        {
            final DefaultOnlineResource online = new DefaultOnlineResource(URI.create("http://www.ifremer.fr/sismer/"));
            online.setProtocol("http");
            final DefaultContact contact = new DefaultContact(online);
            contact.setPhones(Arrays.asList(
                    telephone("+33 (0)2 xx.xx.xx.x6", "VOICE"),
                    telephone("+33 (0)2 xx.xx.xx.x4", "FACSIMILE")
            ));
            final DefaultAddress address = new DefaultAddress();
            address.setDeliveryPoints(singleton("Brest institute"));
            address.setCity(new SimpleInternationalString("Plouzane"));
            address.setPostalCode("29280");
            address.setCountry(country);
            address.setElectronicMailAddresses(singleton("xx@xx.fr"));
            contact.setAddresses(singleton(address));
            author.setParties(Arrays.asList(
                    new DefaultOrganisation("Marine institutes", null, null, contact)
            ));
            metadata.setContacts(singleton(author));
        }
        /*
         * Data indentification.
         */
        {
            final DefaultCitation citation = new DefaultCitation("90008411.ctd");
            citation.setAlternateTitles(singleton(new SimpleInternationalString("42292_5p_19900609195600")));
            citation.setDates(Arrays.asList(
                    new DefaultCitationDate(TestUtilities.date("1990-06-04 22:00:00"), DateType.REVISION),
                    new DefaultCitationDate(TestUtilities.date("1979-08-02 22:00:00"), DateType.CREATION)));
            {
                @SuppressWarnings("deprecation")
                final DefaultResponsibleParty originator = new DefaultResponsibleParty(Role.ORIGINATOR);
                final DefaultOnlineResource online = new DefaultOnlineResource(URI.create("http://www.com.univ-mrs.fr/LOB/"));
                online.setProtocol("http");
                final DefaultContact contact = new DefaultContact(online);
                contact.setPhones(Arrays.asList(
                        telephone("+33 (0)4 xx.xx.xx.x5", "VOICE"),
                        telephone("+33 (0)4 xx.xx.xx.x8", "FACSIMILE")
                ));
                final DefaultAddress address = new DefaultAddress();
                address.setDeliveryPoints(singleton("Oceanology institute"));
                address.setCity(new SimpleInternationalString("Marseille"));
                address.setPostalCode("13288");
                address.setCountry(country);
                contact.setAddresses(singleton(address));
                originator.setParties(Arrays.asList(
                        new DefaultOrganisation("Oceanology laboratory", null, null, contact)
                ));
                citation.setCitedResponsibleParties(singleton(originator));
            }
            final DefaultDataIdentification identification = new DefaultDataIdentification(
                    citation,                   // Citation
                    "CTD NEDIPROD VI 120",      // Abstract
                    Locale.ENGLISH,             // Language,
                    TopicCategory.OCEANS);      // Topic category
            {
                @SuppressWarnings("deprecation")
                final DefaultResponsibleParty custodian = new DefaultResponsibleParty((DefaultResponsibility) author);
                custodian.setRole(Role.CUSTODIAN);
                identification.setPointOfContacts(singleton(custodian));
            }
            /*
             * Data indentification / Keywords.
             */
            {
                final DefaultKeywords keyword = new DefaultKeywords(
                        new Anchor(URI.create("SDN:P021:35:ATTN"), "Transmittance and attenuance of the water column"));
                keyword.setType(KeywordType.THEME);
                final DefaultCitation thesaurus = new DefaultCitation("BODC Parameter Discovery Vocabulary");
                thesaurus.setAlternateTitles(singleton(new SimpleInternationalString("P021")));
                thesaurus.setDates(singleton(new DefaultCitationDate(TestUtilities.date("2008-11-25 23:00:00"), DateType.REVISION)));
                thesaurus.setEdition(new Anchor(URI.create("SDN:C371:1:35"), "35"));
                thesaurus.setIdentifiers(singleton(new ImmutableIdentifier(null, null, "http://www.seadatanet.org/urnurl/")));
                keyword.setThesaurusName(thesaurus);
                identification.setDescriptiveKeywords(singleton(keyword));
            }
            /*
             * Data indentification / Browse graphic.
             */
            {
                final DefaultBrowseGraphic g = new DefaultBrowseGraphic(URI.create("file:///thumbnail.png"));
                g.setFileDescription(new SimpleInternationalString("Arbitrary thumbnail for this test only."));
                identification.setGraphicOverviews(Arrays.asList(g));
            }
            /*
             * Data indentification / Resource constraint.
             */
            {
                final DefaultLegalConstraints constraint = new DefaultLegalConstraints();
                constraint.setAccessConstraints(singleton(Restriction.LICENSE));
                identification.setResourceConstraints(singleton(constraint));
            }
            /*
             * Data indentification / Aggregate information.
             */
            {
                @SuppressWarnings("deprecation")
                final DefaultAggregateInformation aggregateInfo = new DefaultAggregateInformation();
                final DefaultCitation name = new DefaultCitation("MEDIPROD VI");
                name.setAlternateTitles(singleton(new SimpleInternationalString("90008411")));
                name.setDates(singleton(new DefaultCitationDate(TestUtilities.date("1990-06-04 22:00:00"), DateType.REVISION)));
                aggregateInfo.setName(name);
                aggregateInfo.setInitiativeType(InitiativeType.CAMPAIGN);
                aggregateInfo.setAssociationType(AssociationType.LARGER_WORD_CITATION);
                identification.setAggregationInfo(singleton(aggregateInfo));
            }
            /*
             * Data indentification / Extent.
             */
            {
                final DefaultCoordinateSystemAxis axis = new DefaultCoordinateSystemAxis(
                        singletonMap(DefaultCoordinateSystemAxis.NAME_KEY, new NamedIdentifier(null, "Depth")),
                        "d", AxisDirection.DOWN, SI.METRE);

                final DefaultVerticalCS cs = new DefaultVerticalCS(
                        singletonMap(DefaultVerticalCS.NAME_KEY, new NamedIdentifier(null, "Depth")),
                        axis);

                final DefaultVerticalDatum datum = new DefaultVerticalDatum(
                        singletonMap(DefaultVerticalDatum.NAME_KEY, new NamedIdentifier(null, "D28")),
                        VerticalDatumType.OTHER_SURFACE);

                final DefaultVerticalCRS vcrs = new DefaultVerticalCRS(
                        singletonMap(DefaultVerticalCRS.NAME_KEY, new NamedIdentifier(null, "Depth below D28")),
                        datum, cs);

                final DefaultTemporalExtent temporal = new DefaultTemporalExtent();
                setTemporalBounds(temporal, "1990-06-05", "1990-07-02");
                identification.setExtents(singleton(new DefaultExtent(
                        null,
                        new DefaultGeographicBoundingBox(1.1667, 1.1667, 36.6, 36.6),
                        new DefaultVerticalExtent(Double.NaN, Double.NaN, vcrs),
                        temporal)));
            }
            metadata.setIdentificationInfo(singleton(identification));
        }
        /*
         * Information about spatial representation.
         */
        {
            final DefaultVectorSpatialRepresentation rep = new DefaultVectorSpatialRepresentation();
            final DefaultGeometricObjects geoObj = new DefaultGeometricObjects(GeometricObjectType.POINT);
            rep.setGeometricObjects(singleton(geoObj));
            metadata.setSpatialRepresentationInfo(singleton(rep));
        }
        /*
         * Information about Coordinate Reference System.
         */
        {
            final DefaultCitation citation = new DefaultCitation("A geographic coordinate reference frames");
            citation.setAlternateTitles(singleton(new SimpleInternationalString("L101")));
            citation.setIdentifiers(singleton(new ImmutableIdentifier(null, null, "http://www.seadatanet.org/urnurl/")));
            citation.setEdition(new Anchor(URI.create("SDN:C371:1:2"), "2"));
            metadata.setReferenceSystemInfo(singleton(
                    new ReferenceSystemMetadata(new ImmutableIdentifier(citation, "L101", "EPSG:4326"))));
        }
        /*
         * Information about content.
         */
        {
            final DefaultImageDescription contentInfo = new DefaultImageDescription();
            contentInfo.setCloudCoverPercentage(50.0);
            metadata.setContentInfo(singleton(contentInfo));
        }
        /*
         * Extension to metadata.
         */
        {
            final DefaultMetadataExtensionInformation extensionInfo = new DefaultMetadataExtensionInformation();
            extensionInfo.setExtendedElementInformation(singleton(new DefaultExtendedElementInformation(
                    "SDN:EDMO::",                           // Name
                    "http://www.seadatanet.org/urnurl/",    // Definition
                    null,                                   // Condition
                    Datatype.CODE_LIST,                     // Data type
                    "SeaDataNet",                           // Parent entity
                    null,                                   // Rule
                    null)));                                // Source
            metadata.setMetadataExtensionInfo(singleton(extensionInfo));
        }
        /*
         * Distribution information.
         */
        {
            @SuppressWarnings("deprecation")
            final DefaultResponsibleParty distributor = new DefaultResponsibleParty((DefaultResponsibility) author);
            final DefaultDistribution distributionInfo = new DefaultDistribution();
            distributor.setRole(Role.DISTRIBUTOR);
            distributionInfo.setDistributors(singleton(new DefaultDistributor(distributor)));
            distributionInfo.setDistributionFormats(singleton(
                    new DefaultFormat(new Anchor(URI.create("SDN:L241:1:MEDATLAS"), "MEDATLAS ASCII"), "1.0")));
            final DefaultDigitalTransferOptions transfer = new DefaultDigitalTransferOptions();
            transfer.setTransferSize(2.431640625);
            final DefaultOnlineResource onlines = new DefaultOnlineResource(URI.create(
                    "http://www.ifremer.fr/data/something"));
            onlines.setDescription(new SimpleInternationalString("CTDF02"));
            onlines.setFunction(OnLineFunction.DOWNLOAD);
            onlines.setProtocol("http");
            transfer.setOnLines(singleton(onlines));
            distributionInfo.setTransferOptions(singleton(transfer));
            metadata.setDistributionInfo(distributionInfo);
        }
        return metadata;
    }

    /**
     * Returns the URL to the {@code "Metadata.xml"} file to use for this test.
     *
     * @return The URL to {@code "Metadata.xml"} test file.
     */
    private URL getResource() {
        return DefaultMetadataTest.class.getResource("Metadata.xml");
    }

    /**
     * Tests marshalling of a XML document.
     *
     * @throws Exception If an error occurred during marshalling.
     */
    @Test
    public void testMarshalling() throws Exception {
        final MarshallerPool pool   = getMarshallerPool();
        final Marshaller     ms     = pool.acquireMarshaller();
        final StringWriter   writer = new StringWriter(25000);
        ms.marshal(createHardCoded(), writer);
        pool.recycle(ms);
        /*
         * Apache SIS can marshal CharSequence as Anchor only if the property type is InternationalString.
         * But the 'Metadata.hierarchyLevelName' and 'Identifier.code' properties are String, which we can
         * not subclass. Concequently SIS currently marshals them as plain string. Replace those strings
         * by the anchor version so we can compare the XML with the "Metadata.xml" file content.
         */
        final StringBuffer xml = writer.getBuffer();
        replace(xml, "<gco:CharacterString>Common Data Index record</gco:CharacterString>",
                     "<gmx:Anchor xlink:href=\"SDN:L231:3:CDI\">Common Data Index record</gmx:Anchor>");
        replace(xml, "<gco:CharacterString>EPSG:4326</gco:CharacterString>",
                     "<gmx:Anchor xlink:href=\"SDN:L101:2:4326\">EPSG:4326</gmx:Anchor>");
        replace(xml, "License", "Licence");
        /*
         * The <gmd:EX_TemporalExtent> block can not be marshalled yet, since it requires the sis-temporal module.
         * We need to instruct the XML comparator to ignore this block during the comparison. We also ignore for
         * now the "gml:id" attribute since SIS generates different values than the ones in oyr test XML file,
         * and those values may change in future SIS version.
         */
        final XMLComparator comparator = new XMLComparator(getResource(), xml.toString());
        comparator.ignoredNodes.add(Namespaces.GMD + ":temporalElement");
        comparator.ignoredAttributes.add("http://www.w3.org/2000/xmlns:*");
        comparator.ignoredAttributes.add(Namespaces.XSI + ":schemaLocation");
        comparator.ignoredAttributes.add(Namespaces.GML + ":id");
        comparator.ignoreComments = true;
        comparator.compare();
    }

    /**
     * Replaces the first occurrence of the given string by an other one.
     *
     * @param buffer    The buffer in which to perform the replacement.
     * @param toSearch  The string to search.
     * @param replaceBy The value to use as a replacement.
     */
    private static void replace(final StringBuffer buffer, final String toSearch, final String replaceBy) {
        final int i = buffer.indexOf(toSearch);
        assertTrue("String to replace not found.", i >= 0);
        buffer.replace(i, i+toSearch.length(), replaceBy);
    }

    /**
     * Tests unmarshalling of a XML document.
     *
     * @throws JAXBException If an error occurred during unmarshalling.
     */
    @Test
    public void testUnmarshalling() throws JAXBException {
        /*
         * Note: if this DefaultMetadataTest class is made final, then all following lines
         * until pool.recycle(…) can be replaced by a call to unmarshallFile("Metadata.xml").
         */
        final MarshallerPool pool = getMarshallerPool();
        final Unmarshaller unmarshaller = pool.acquireUnmarshaller();
        final DefaultMetadata metadata = (DefaultMetadata) unmarshaller.unmarshal(getResource());
        pool.recycle(unmarshaller);
        final DefaultMetadata expected = createHardCoded();
        assertTrue(metadata.equals(expected, ComparisonMode.DEBUG));
    }
}
