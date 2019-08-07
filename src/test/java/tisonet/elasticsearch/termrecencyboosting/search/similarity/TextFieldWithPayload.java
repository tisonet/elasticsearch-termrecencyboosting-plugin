/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package tisonet.elasticsearch.termrecencyboosting.search.similarity;

import java.io.Reader;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.FieldType;
import org.apache.lucene.index.IndexOptions;


public class TextFieldWithPayload extends Field {

    /** Indexed, tokenized, not stored. */
    public static final FieldType TYPE_NOT_STORED = new FieldType();

    /** Indexed, tokenized, stored. */
    public static final FieldType TYPE_STORED = new FieldType();

    static {
        TYPE_NOT_STORED.setIndexOptions(IndexOptions.DOCS_AND_FREQS_AND_POSITIONS);
        TYPE_NOT_STORED.setTokenized(true);
        TYPE_NOT_STORED.setStoreTermVectors(true);
        TYPE_NOT_STORED.setStoreTermVectorPositions(true);
        TYPE_NOT_STORED.setStoreTermVectorPayloads(true);
        TYPE_NOT_STORED.freeze();

        TYPE_STORED.setIndexOptions(IndexOptions.DOCS_AND_FREQS_AND_POSITIONS);
        TYPE_STORED.setTokenized(true);
        TYPE_STORED.setStoreTermVectors(true);
        TYPE_STORED.setStoreTermVectorPositions(true);
        TYPE_STORED.setStoreTermVectorPayloads(true);
        TYPE_STORED.setStored(true);
        TYPE_STORED.freeze();
    }

    /** Creates a new un-stored TextField with Reader value.
     * @param name field name
     * @param reader reader value
     * @throws IllegalArgumentException if the field name is null
     * @throws NullPointerException if the reader is null
     */
    public TextFieldWithPayload(String name, Reader reader) {
        super(name, reader, TYPE_NOT_STORED);
    }

    /** Creates a new TextField with String value.
     * @param name field name
     * @param value string value
     * @param store Store.YES if the content should also be stored
     * @throws IllegalArgumentException if the field name or value is null.
     */
    public TextFieldWithPayload(String name, String value, Store store) {
        super(name, value, store == Store.YES ? TYPE_STORED : TYPE_NOT_STORED);
    }

    /** Creates a new un-stored TextField with TokenStream value.
     * @param name field name
     * @param stream TokenStream value
     * @throws IllegalArgumentException if the field name is null.
     * @throws NullPointerException if the tokenStream is null
     */
    public TextFieldWithPayload(String name, TokenStream stream) {
        super(name, stream, TYPE_NOT_STORED);
    }
}
