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
package org.apache.sis.resources;

import java.util.List;
import java.util.Locale;
import java.util.Collections;
import java.util.ResourceBundle;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.io.IOException;


/**
 * Controls the resource bundle loading. This class looks for {@code .utf} files rather than
 * the Java default {@code .class} or {@code .properties} files.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.3 (derived from geotk-3.00)
 * @version 0.3
 * @module
 */
final class Loader extends ResourceBundle.Control {
    /**
     * The filename extension of resource files, without leading dot.
     */
    private static final String EXTENSION = "utf";

    /**
     * The formats supported by this loader.
     */
    private static final List<String> FORMATS = Collections.singletonList("apache-sis." + EXTENSION);

    /**
     * The singleton instance of the {@link Loader} class.
     */
    public static final Loader INSTANCE = new Loader();

    /**
     * Creates the unique instance of the SIS resource bundle loader.
     */
    private Loader() {
    }

    /**
     * Returns the formats supported by this loader.
     * The only supported format is {@code "apache-sis.utf"}.
     *
     * @param  baseName Ignored.
     * @return The supported formats.
     */
    @Override
    public List<String> getFormats(String baseName) {
        return FORMATS;
    }

    /**
     * Returns {@code false} in all cases, since our implementation never needs reload.
     */
    @Override
    public boolean needsReload(final String baseName, final Locale locale, final String format,
            final ClassLoader loader, final ResourceBundle bundle, long loadTime)
    {
        return false;
    }

    /**
     * Instantiates a new resource bundle.
     *
     * @param  baseName  The fully qualified name of the base resource bundle.
     * @param  locale    The locale for which the resource bundle should be instantiated.
     * @param  format    Ignored since this loader supports only one format.
     * @param  loader    The class loader to use.
     * @param  reload    Ignored since this loader do not supports resource expiration.
     * @return The resource bundle instance, or null if none could be found.
     */
    @Override
    public ResourceBundle newBundle(final String baseName, final Locale locale,
            final String format, final ClassLoader loader, final boolean reload)
            throws IllegalAccessException, InstantiationException, IOException
    {
        final Class<?> classe;
        try {
            classe = Class.forName(baseName, true, loader);
        } catch (ClassNotFoundException e) {
            return null; // This is the expected behavior as of Control.newBundle contract.
        }
        /*
         * Gets the filename relative to the class we created, since we assumes that UTF files
         * are in the same package. Then check for file existence and instantiate the resource
         * bundle only if the file is found.
         */
        final String classname = classe.getSimpleName();
        String filename = toResourceName(toBundleName(classname, locale), EXTENSION);
        if (classe.getResource(filename) == null) {
            if (!Locale.ENGLISH.equals(locale)) {
                return null;
            }
            // We have no explicit resources for English. We use the default one for that.
            filename = toResourceName(classname, EXTENSION);
            if (classe.getResource(filename) == null) {
                return null;
            }
        }
        /*
         * If the file exists, instantiate now the resource bundle. Note that the constructor
         * will not loads the data immediately, which is why we don't pass it the above URL.
         */
        final Constructor<?> c;
        try {
            c = classe.getDeclaredConstructor(String.class);
        } catch (NoSuchMethodException e) {
            throw instantiationFailure(e);
        }
        final ResourceBundle bundle;
        // Do not call c.setAccessible(true) - this is not allowed in Applet.
        try {
            bundle = (ResourceBundle) c.newInstance(filename);
        } catch (InvocationTargetException e) {
            throw instantiationFailure(e);
        }
        return bundle;
    }

    /**
     * Creates an exception for a resource bundle that can not be created.
     */
    private static InstantiationException instantiationFailure(final Exception cause) {
        InstantiationException exception = new InstantiationException(cause.getLocalizedMessage());
        exception.initCause(cause);
        return exception;
    }
}
