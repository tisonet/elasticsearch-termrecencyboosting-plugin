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


package ga.p2502.elasticsearch.search.similarity;

import org.apache.logging.log4j.Logger;
import org.apache.lucene.analysis.payloads.PayloadHelper;
import org.apache.lucene.index.FieldInvertState;
import org.apache.lucene.index.LeafReaderContext;
import org.apache.lucene.index.PostingsEnum;
import org.apache.lucene.index.Terms;
import org.apache.lucene.index.TermsEnum;
import org.apache.lucene.search.CollectionStatistics;
import org.apache.lucene.search.Explanation;
import org.apache.lucene.search.TermStatistics;
import org.apache.lucene.search.similarities.Similarity;
import org.apache.lucene.util.BytesRef;
import org.apache.lucene.util.SmallFloat;
import org.elasticsearch.common.logging.Loggers;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;


public class TermRecencyBoosting extends Similarity {
    static String DEFAULT_DECAY_FUNCTION = "linear";
    
    // 24 hours from now (scale) term relevance decreases to half 0.5 (decay)
    static Double DEFAULT_DECAY = 0.5;
    static Double DEFAULT_SCALE = 24.0; // 24 hours since now() returns decay function a given decay value.
    static Double DEFAULT_WEIGHT = 1.0; // Decay value multiplier to strengthen boosting.

    private final Similarity similarity;
    private final DecayBooster recencyBooster;


    TermRecencyBoosting(Similarity similarity){
        this(similarity, DEFAULT_DECAY_FUNCTION, DEFAULT_SCALE, DEFAULT_DECAY, DEFAULT_WEIGHT);
    }

    TermRecencyBoosting(Similarity similarity, String decayFunction, Double scale, Double decay, Double maxBoost) {
        this.similarity = similarity;
        this.recencyBooster = new DecayBooster(decayFunction, scale, decay, maxBoost);
    }

    @Override
    public String toString() {
        return "TermRecencyBoosting";
    }

    @Override
    public long computeNorm(FieldInvertState state) {
        // Ignore length during indexing use just field boost
        return SmallFloat.floatToByte315(state.getBoost());
    }

    @Override
    public SimWeight computeWeight(CollectionStatistics collectionStats, TermStatistics... termStats) {
        SimWeight simWeight = this.similarity.computeWeight(collectionStats, termStats);
        return new RecencyStats(simWeight, collectionStats.field(), termStats);
    }

    @Override
    public final SimScorer simScorer(SimWeight weight, LeafReaderContext context) throws IOException {
        RecencyStats recencyStats = (RecencyStats) weight;

        SimScorer bm25simScorer = this.similarity.simScorer(recencyStats.similaritySimWeight, context);
        return new RecencySimScorer(bm25simScorer, recencyStats, context);
    }

    private static class RecencyStats extends SimWeight {
        private final String field;
        private final SimWeight similaritySimWeight;
        private final TermStatistics[] termStats;

        RecencyStats(SimWeight similaritySimWeight, String field, TermStatistics... termStats) {
            this.similaritySimWeight = similaritySimWeight;
            this.field = field;
            this.termStats = termStats;
        }

        @Override
        public float getValueForNormalization() {
            return similaritySimWeight.getValueForNormalization();
        }

        @Override
        public void normalize(float queryNorm, float boost) {
            similaritySimWeight.normalize(queryNorm, boost);
        }
    }

    private final class RecencySimScorer extends SimScorer {

        private static final int DEFAULT_TERM_TIMESTAMP = 0;

        private final Logger logger = Loggers.getLogger(this.getClass());
        private final SimScorer similaritySimScorer;
        private final RecencyStats recencyStats;
        private final LeafReaderContext context;

        RecencySimScorer(SimScorer similaritySimScorer, RecencyStats recencyStats, LeafReaderContext context) throws IOException {
            this.similaritySimScorer = similaritySimScorer;
            this.recencyStats = recencyStats;
            this.context = context;
        }

        @Override
        public float score(int doc, float freq) {
            return similaritySimScorer.score(doc, freq) * scoreRecency(doc);
        }

        
        @Override
        public float computeSlopFactor(int distance) {
            return similaritySimScorer.computeSlopFactor(distance);
        }

        @Override
        public float computePayloadFactor(int doc, int start, int end, BytesRef payload) {
            return similaritySimScorer.computePayloadFactor(doc, start, end, payload);
        }


        @Override
        public Explanation explain(int doc, Explanation freq) {
            float bootsScore = score(doc, freq.getValue());

            Explanation similarityExp = similaritySimScorer.explain(doc,freq);
            List<Explanation> subs = new ArrayList<>();
            subs.add(similarityExp);

            List<Explanation> recencyScoreSubs = new ArrayList<>();
            long termRecency = getTermRecency(doc);
            recencyScoreSubs.add(Explanation.match((float)getTermRecency(doc), "termRecency"));
            if (termRecency >= 0) {
                float termRecencyBoost = recencyBooster.getBoost(termRecency);
                recencyScoreSubs.add(Explanation.match(termRecencyBoost, "termRecencyBoost",
                        recencyBooster.explain(termRecency)));
            }

            Explanation recencyExp = Explanation.match(scoreRecency(doc),
                    "recencyScore, computed as 1.0 + termRecencyBoost from:", recencyScoreSubs);
            subs.add(recencyExp);

            return Explanation.match(bootsScore,"score(doc="+doc+",freq="+freq+"), product of:", subs);

        }

        private float scoreRecency(int doc){
            float termRecencyBoost = 0.0f;
            long termRecency = getTermRecency(doc);
            if (termRecency >= 0) {
                termRecencyBoost = recencyBooster.getBoost(termRecency);
            }

            return 1.0f + termRecencyBoost;
        }

        private long getTermRecency(int doc){
            int termTimestamp = getLatestTermTimestamp(doc);

            if (termTimestamp != DEFAULT_TERM_TIMESTAMP) {
                return RecencyCalculator.calculateRecency(termTimestamp);
            }

            return -1;
        }

        private int getLatestTermTimestamp(int doc) {
            int latestPayload = 0;

            for (int i = 0; i < recencyStats.termStats.length; i++) {
                int recencyPayload = readTermTimestampFromPayload(doc, recencyStats.termStats[i].term());
                latestPayload = Math.max(recencyPayload, latestPayload);
            }

            return latestPayload;
        }

        private int readTermTimestampFromPayload(int doc, BytesRef term) {
            try {
                Terms terms = context.reader().getTermVector(doc, recencyStats.field);
                if (terms == null) {
                    return DEFAULT_TERM_TIMESTAMP;
                }

                TermsEnum termsEnum = terms.iterator();
                if (!termsEnum.seekExact(term)) {
                    return DEFAULT_TERM_TIMESTAMP;
                }

                PostingsEnum dpEnum = termsEnum.postings(null, PostingsEnum.ALL);
                dpEnum.nextDoc();
                dpEnum.nextPosition();
                BytesRef payload = dpEnum.getPayload();
                if (payload == null) {
                    return DEFAULT_TERM_TIMESTAMP;
                }

                return PayloadHelper.decodeInt(payload.bytes, payload.offset);
            } catch (Exception ex) {
                logger.error("Unexpected exception in field = " + recencyStats.field, ex);

                return DEFAULT_TERM_TIMESTAMP;
            }
        }
    }
}
