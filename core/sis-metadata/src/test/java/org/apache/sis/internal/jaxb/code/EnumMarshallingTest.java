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
package org.apache.sis.internal.jaxb.code;

import java.util.Arrays;
import javax.xml.bind.JAXBException;
import org.opengis.metadata.identification.TopicCategory;
import org.apache.sis.metadata.iso.identification.DefaultDataIdentification;
import org.apache.sis.xml.Namespaces;
import org.apache.sis.test.XMLTestCase;
import org.junit.Test;

import static org.apache.sis.test.Assert.*;


/**
 * Tests the XML marshaling of {@code Enum}.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.5
 * @version 0.5
 * @module
 */
public final strictfp class EnumMarshallingTest extends XMLTestCase {
    /**
     * Tests marshaling of a code list which is not in the list of standard codes.
     *
     * @throws JAXBException If an error occurred while marshaling the XML.
     */
    @Test
    public void testEnums() throws JAXBException {
        final DefaultDataIdentification id = new DefaultDataIdentification();
        id.setTopicCategories(Arrays.asList(
                TopicCategory.OCEANS,
                TopicCategory.ENVIRONMENT,
                TopicCategory.HEALTH));

        final String xml = marshal(id);
        assertXmlEquals(
                "<gmd:MD_DataIdentification xmlns:gmd=\"" + Namespaces.GMD + "\">\n" +
                "  <gmd:topicCategory>\n" +
                "    <gmd:MD_TopicCategoryCode>environment</gmd:MD_TopicCategoryCode>\n" +
                "  </gmd:topicCategory>\n" +
                "  <gmd:topicCategory>\n" +
                "    <gmd:MD_TopicCategoryCode>health</gmd:MD_TopicCategoryCode>\n" +
                "  </gmd:topicCategory>\n" +
                "  <gmd:topicCategory>\n" +
                "    <gmd:MD_TopicCategoryCode>oceans</gmd:MD_TopicCategoryCode>\n" +
                "  </gmd:topicCategory>\n" +
                "</gmd:MD_DataIdentification>",
                xml, "xmlns:*");
    }
}
