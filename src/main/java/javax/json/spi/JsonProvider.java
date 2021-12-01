/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package javax.json.spi;

import java.io.InputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.io.Writer;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Collection;
import java.util.Map;

import javax.json.JsonArray;
import javax.json.JsonArrayBuilder;
import javax.json.JsonBuilderFactory;
import javax.json.JsonMergePatch;
import javax.json.JsonNumber;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import javax.json.JsonPatch;
import javax.json.JsonPatchBuilder;
import javax.json.JsonPointer;
import javax.json.JsonReader;
import javax.json.JsonReaderFactory;
import javax.json.JsonString;
import javax.json.JsonStructure;
import javax.json.JsonValue;
import javax.json.JsonWriter;
import javax.json.JsonWriterFactory;
import javax.json.stream.JsonGenerator;
import javax.json.stream.JsonGeneratorFactory;
import javax.json.stream.JsonParser;
import javax.json.stream.JsonParserFactory;

import org.osgi.annotation.versioning.ProviderType;

@ProviderType
public abstract class JsonProvider {
    private static final class Holder {
        private static final JsonProvider DEFAULT = new org.apache.johnzon.core.JsonProviderImpl();
    }

    protected JsonProvider() {
        // no-op
    }

    public static JsonProvider provider() {
        return Holder.DEFAULT;
    }

    public abstract JsonParser createParser(Reader reader);

    public abstract JsonParser createParser(InputStream in);

    public abstract JsonParserFactory createParserFactory(Map<String, ?> config);

    public abstract JsonGenerator createGenerator(Writer writer);

    public abstract JsonGenerator createGenerator(OutputStream out);

    public abstract JsonGeneratorFactory createGeneratorFactory(Map<String, ?> config);

    public abstract JsonReader createReader(Reader reader);

    public abstract JsonReader createReader(InputStream in);

    public abstract JsonWriter createWriter(Writer writer);

    public abstract JsonWriter createWriter(OutputStream out);

    public abstract JsonWriterFactory createWriterFactory(Map<String, ?> config);

    public abstract JsonReaderFactory createReaderFactory(Map<String, ?> config);

    /**
     * Create an empty JsonObjectBuilder
     * @since 1.0
     */
    public abstract JsonObjectBuilder createObjectBuilder();

    /**
     * Creates a JSON object builder, initialized with the specified JsonObject.
     * @since 1.1
     */
    public JsonObjectBuilder createObjectBuilder(JsonObject jsonObject) {
        throw new UnsupportedOperationException();
    }

    /**
     * Creates a JSON object builder, initialized with the specified Map.
     * @since 1.1
     */
    public JsonObjectBuilder createObjectBuilder(Map<String, Object> map) {
        throw new UnsupportedOperationException();
    }

    public abstract JsonArrayBuilder createArrayBuilder();

    public JsonArrayBuilder createArrayBuilder(JsonArray initialData) {
        throw new UnsupportedOperationException();
    }

    public JsonArrayBuilder createArrayBuilder(Collection<?> initialData) {
        throw new UnsupportedOperationException();
    }

    /**
     * Create a {@link JsonPointer} based on the given path string
     *
     * @since 1.1
     */
    public JsonPointer createPointer(String path) {
        throw new UnsupportedOperationException();
    }

    public abstract JsonBuilderFactory createBuilderFactory(Map<String, ?> config);


    public JsonString createValue(String value) {
        throw new UnsupportedOperationException();
    }

    public JsonNumber createValue(int value) {
        throw new UnsupportedOperationException();
    }

    public JsonNumber createValue(long value) {
        throw new UnsupportedOperationException();
    }

    public JsonNumber createValue(double value) {
        throw new UnsupportedOperationException();
    }

    public JsonNumber createValue(BigDecimal value) {
        throw new UnsupportedOperationException();
    }

    public JsonNumber createValue(BigInteger value) {
        throw new UnsupportedOperationException();
    }

    /**
     * Create a {@link JsonPatch} as defined in
     * <a href="https://tools.ietf.org/html/rfc6902">RFC-6902</a>.
     *
     * @param array with the patch operations
     * @return the JsonPatch based on the given operations
     *
     * @see #createDiff(JsonStructure, JsonStructure)
     *
     * @since 1.1
     */
    public JsonPatch createPatch(JsonArray array) {
        throw new UnsupportedOperationException();
    }

    /**
     * Create a {@link JsonPatch} by comparing the source to the target as defined in
     * <a href="https://tools.ietf.org/html/rfc6902">RFC-6902</a>.
     *
     * Applying this {@link JsonPatch} to the source you will give you the target.
     *
     * @see #createPatch(JsonArray)
     *
     * @since 1.1
     */
    public JsonPatch createDiff(JsonStructure source, JsonStructure target) {
        throw new UnsupportedOperationException();
    }

    /**
     * Create a new JsonPatchBuilder
     * @since 1.1
     */
    public JsonPatchBuilder createPatchBuilder() {
        throw new UnsupportedOperationException();
    }

    /**
     * Create a new JsonPatchBuilder from initial data.
     * @param initialData the initial patch operations
     * @since 1.1
     */
    public JsonPatchBuilder createPatchBuilder(JsonArray initialData) {
        throw new UnsupportedOperationException();
    }

    /**
     * Create a merge patch based on the given JsonValue.
     *
     * If you have the following JsonMergePatch:
     *
     * <pre>
     * {
     *   "a":"z",
     *   "c": {
     *     "f": null
     *   }
     * }
     * </pre>
     *
     * and apply it to the following JSON
     *
     * <pre>
     * {
     *   "a": "b",
     *   "c": {
     *     "d": "e",
     *     "f": "g"
     *   }
     * }
     * </pre>
     *
     * you will get the following result:
     *
     * <pre>
     * {
     *   "a": "z",
     *   "c": {
     *     "d": "e",
     *   }
     * }
     * </pre>
     *
     * @see #createMergeDiff(JsonValue, JsonValue)
     *
     * @since 1.1
     */
    public JsonMergePatch createMergePatch(JsonValue patch) {
        throw new UnsupportedOperationException();
    }


    /**
     * Create a merge patch by comparing the source to the target.
     * Applying this JsonMergePatch to the source will give you the target.
     * A MergePatch is a JsonValue as defined in http://tools.ietf.org/html/rfc7396
     *
     * If you have a JSON like
     * <pre>
     * {
     *   "a": "b",
     *   "c": {
     *     "d": "e",
     *     "f": "g"
     *   }
     * }
     * </pre>
     *
     * and comparing it with
     *
     * <pre>
     * {
     *   "a": "z",
     *   "c": {
     *     "d": "e",
     *   }
     * }
     * </pre>
     *
     * you will get the following JsonMergePatch:
     *
     * <pre>
     * {
     *   "a":"z",
     *   "c": {
     *     "f": null
     *   }
     * }
     * </pre>
     *
     * @see #createMergePatch(JsonValue)
     *
     * @since 1.1
     */
    public JsonMergePatch createMergeDiff(JsonValue source, JsonValue target) {
        throw new UnsupportedOperationException();
    }
}

