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

import org.apache.lucene.analysis.Analyzer;
import org.elasticsearch.index.analysis.CharFilterFactory;
import org.elasticsearch.index.analysis.CustomAnalyzer;
import org.elasticsearch.index.analysis.TokenFilterFactory;

public class RecencyPayloadAnalyzerFactory  {

    public static Analyzer create() {
        CharFilterFactory[] charFilterFactories = new CharFilterFactory[] {};
        TokenFilterFactory[] tokenFilterFactories = new TokenFilterFactory[] {
                new StandardTokenFilterFactory(),
                new DelimitedPayloadTokenFilterFactory()
        };

        return new CustomAnalyzer(
                new WhitespaceTokenizerFactory(),
                charFilterFactories,
                tokenFilterFactories
        );

    }
}
