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
package org.apache.sis.internal.jdk8;


/**
 * Placeholder for the {@link java.util.function.BiFunction} interface.
 *
 * @param <T> The type of first parameter  (source type 1).
 * @param <U> The type of second parameter (source type 2).
 * @param <R> The type of return values    (target type).
 */
public interface BiFunction<T,U,R> {
    /**
     * Computes a value for the given arguments.
     *
     * @param v1 The first source value.
     * @param v2 The second source value.
     * @return The target value.
     */
    R apply(T v1, U v2);
}
