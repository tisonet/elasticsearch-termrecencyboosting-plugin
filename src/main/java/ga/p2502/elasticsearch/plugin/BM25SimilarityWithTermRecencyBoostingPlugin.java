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


package ga.p2502.elasticsearch.plugin;

import ga.p2502.elasticsearch.search.similarity.BM25SimilarityWithTermRecencyBoosting;
import org.elasticsearch.index.IndexModule;
import org.elasticsearch.plugins.Plugin;

public class BM25SimilarityWithTermRecencyBoostingPlugin extends Plugin {
    public void onIndexModule(IndexModule indexModule) {
        indexModule.addSimilarity("BM25-recency", BM25SimilarityWithTermRecencyBoosting::new);
    }
}