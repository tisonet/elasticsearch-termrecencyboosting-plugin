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

package ga.p2502.elasticsearch.index.analysis;

import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.standard.StandardFilter;
import org.elasticsearch.index.analysis.TokenFilterFactory;


public class StandardTokenFilterFactory implements TokenFilterFactory {

    @Override
    public String name() {
        return "standard";
    }

    @Override
    public TokenStream create(TokenStream tokenStream) {
        return new StandardFilter(tokenStream);
    }
}