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
package org.apache.sis.util.iso;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.Locale;
import org.apache.sis.util.Immutable;

import static org.apache.sis.util.ArgumentChecks.ensureNonNull;

// Related to JDK7
import java.util.Objects;


/**
 * An international string consisting of a single string for all locales.
 * For such a particular case, this implementation is more effective than
 * other implementations provided in this package.
 *
 * {@section Instantiation}
 * If the characters sequence to wrap is known to be a {@code String} instance, then
 * the {@link #SimpleInternationalString(String)} constructor is okay. Otherwise use
 * the {@link Types#toInternationalString(CharSequence)} method.
 *
 * @author  Martin Desruisseaux (IRD, Geomatys)
 * @since   0.3 (derived from geotk-2.1)
 * @version 0.3
 * @module
 */
@Immutable
public class SimpleInternationalString extends AbstractInternationalString implements Serializable {
    /**
     * Serial number for inter-operability with different versions.
     */
    private static final long serialVersionUID = 7600371840915793379L;

    /**
     * Creates a new instance from the given string. If the type of the text
     * to wrap is the more generic {@code CharSequence} interface, then use
     * the {@link Types#toInternationalString(CharSequence)} method instead.
     *
     * @param text The string for all locales.
     */
    public SimpleInternationalString(final String text) {
        ensureNonNull("text", text);
        defaultValue = text;
    }

    /**
     * Returns the string representation, which is unique for all locales.
     */
    @Override
    public String toString() {
        return defaultValue;
    }

    /**
     * Returns the same string for all locales. This is the string given to the constructor.
     *
     * @param locale Ignored in the {@code SampleInternationalString} implementation.
     */
    @Override
    public String toString(final Locale locale) {
        return defaultValue;
    }

    /**
     * Compares this international string with the specified object for equality.
     *
     * @param object The object to compare with this international string.
     * @return {@code true} if the given object is equal to this string.
     */
    @Override
    public boolean equals(final Object object) {
        if (object != null && object.getClass() == getClass()) {
            final SimpleInternationalString that = (SimpleInternationalString) object;
            return Objects.equals(this.defaultValue, that.defaultValue);
        }
        return false;
    }

    /**
     * Returns a hash code value for this international text.
     *
     * @return The hash code value.
     */
    @Override
    public int hashCode() {
        return defaultValue.hashCode() ^ (int) serialVersionUID;
    }

    /**
     * Writes the string. This is required since {@link #defaultValue} is not serialized.
     */
    private void writeObject(final ObjectOutputStream out) throws IOException {
        out.defaultWriteObject();
        out.writeUTF(defaultValue);
    }

    /**
     * Reads the string. This is required since {@link #defaultValue} is not serialized.
     */
    private void readObject(final ObjectInputStream in) throws IOException, ClassNotFoundException {
        in.defaultReadObject();
        defaultValue = in.readUTF();
    }
}
