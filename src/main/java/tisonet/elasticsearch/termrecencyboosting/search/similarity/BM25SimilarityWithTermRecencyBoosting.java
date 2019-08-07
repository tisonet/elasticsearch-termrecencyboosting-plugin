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

import org.apache.lucene.search.similarities.BM25Similarity;
import org.apache.lucene.search.similarities.Similarity;
import org.elasticsearch.common.inject.Inject;
import org.elasticsearch.common.inject.assistedinject.Assisted;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.index.similarity.AbstractSimilarityProvider;


public class BM25SimilarityWithTermRecencyBoosting extends AbstractSimilarityProvider {
    private final TermRecencyBoosting similarity;

    @Inject
    public BM25SimilarityWithTermRecencyBoosting(@Assisted String name, @Assisted Settings settings) {
        super(name);
        String decayFunction = settings.get("decay_function", TermRecencyBoosting.DEFAULT_DECAY_FUNCTION);
        Double scale = settings.getAsDouble("scale", TermRecencyBoosting.DEFAULT_SCALE);
        Double decay = settings.getAsDouble("decay", TermRecencyBoosting.DEFAULT_DECAY);
        Double weight = settings.getAsDouble("weight", TermRecencyBoosting.DEFAULT_WEIGHT);

        // BM25 field length normalization is disabled, because it doesn't make sense for fields with recency boosting.
        Similarity bm25WithDisabledFieldLengthNorm =  new BM25Similarity(1.2F, 0.0F);
        this.similarity = new TermRecencyBoosting(bm25WithDisabledFieldLengthNorm, decayFunction, scale, decay, weight);
    }

    public TermRecencyBoosting get() {
        return similarity;
    }
}