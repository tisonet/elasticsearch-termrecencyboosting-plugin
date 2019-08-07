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

package tisonet.elasticsearch.termrecencyboosting.index.analysis;

import org.apache.lucene.analysis.Tokenizer;
import org.apache.lucene.analysis.core.WhitespaceTokenizer;
import org.elasticsearch.index.analysis.TokenizerFactory;

public class WhitespaceTokenizerFactory implements TokenizerFactory {
    @Override
    public String name() {
        return "whitespace";
    }

    @Override
    public Tokenizer create() {
        return new WhitespaceTokenizer();
    }
}
