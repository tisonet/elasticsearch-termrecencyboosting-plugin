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
import org.elasticsearch.common.settings.Settings;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

import static java.util.Arrays.asList;


public class RecencySimilarityDecorator extends Similarity {
    public static final Instant RecencyEpoch = Instant.parse("2018-01-01T00:00:00.00Z");

    private final Settings settings;
    private final Similarity decoratedSimilarity;

    RecencySimilarityDecorator(Settings settings, Similarity decoratedSimilarity) {
        this.settings = settings;
        this.decoratedSimilarity = decoratedSimilarity;
    }

    @Override
    public String toString() {
        return "RecencySimilarityDecorator";
    }

    @Override
    public long computeNorm(FieldInvertState state) {
        // ignore length during indexing use just field boost
        return SmallFloat.floatToByte315(state.getBoost());
    }

    @Override
    public SimWeight computeWeight(CollectionStatistics collectionStats, TermStatistics... termStats) {
        SimWeight simWeight = this.decoratedSimilarity.computeWeight(collectionStats, termStats);
        return new RecencyStats(simWeight, collectionStats.field(), termStats);
    }

    @Override
    public final SimScorer simScorer(SimWeight weight, LeafReaderContext context) throws IOException {
        RecencyStats recencyStats = (RecencyStats) weight;

        SimScorer bm25simScorer = this.decoratedSimilarity.simScorer(recencyStats.decoratedSimWeight, context);
        return new RecencySimScorer(bm25simScorer, recencyStats, context);
    }

    private static class RecencyStats extends SimWeight {
        private final String field;
        private final SimWeight decoratedSimWeight;
        private final TermStatistics[] termStats;

        RecencyStats(SimWeight decoratedSimWeight, String field, TermStatistics... termStats) {
            this.decoratedSimWeight = decoratedSimWeight;
            this.field = field;
            this.termStats = termStats;
        }

        @Override
        public float getValueForNormalization() {
            return decoratedSimWeight.getValueForNormalization();
        }

        @Override
        public void normalize(float queryNorm, float boost) {
            decoratedSimWeight.normalize(queryNorm, boost);
        }
    }

    private final class RecencySimScorer extends SimScorer {

        private final int DEFAULT_RECENCY = 0;
        private final Logger logger = Loggers.getLogger(this.getClass());
        private final SimScorer decoratedSimScorer;
        private final RecencyStats recencyStats;
        private final LeafReaderContext context;
        private Explanation recencyBoostExplanation;

        RecencySimScorer(SimScorer decoratedSimScorer, RecencyStats recencyStats, LeafReaderContext context) throws IOException {
            this.decoratedSimScorer = decoratedSimScorer;
            this.recencyStats = recencyStats;
            this.context = context;
        }

        @Override
        public float score(int doc, float freq) {
            return decoratedSimScorer.score(doc, freq) * computeTermRecencyBoostScore(doc);
        }

        private float computeTermRecencyBoostScore(int doc) {
            float termRecencyBoost = 1.0f;
            long termRecencyInHours = 0;
            int mostRecentPayload = 0;

            for (int i = 0; i < recencyStats.termStats.length; i++) {
                int recencyPayload = getTermRecencyPayload(doc, recencyStats.termStats[i].term());
                mostRecentPayload = Math.max(recencyPayload, mostRecentPayload);
            }

            if (mostRecentPayload != DEFAULT_RECENCY) {
                termRecencyInHours = getTermRecency(Instant.now(), mostRecentPayload);
                termRecencyBoost = computeRecencyBoostFactor(termRecencyInHours);
            }

            recencyBoostExplanation = Explanation.match(
                    termRecencyBoost,
                    "score(recency boost=" + termRecencyBoost + ", payload=" + mostRecentPayload +
                            ", recency in hours=" + termRecencyInHours + ")"
            );

            return termRecencyBoost;
        }

        @Override
        public float computeSlopFactor(int distance) {
            return decoratedSimScorer.computeSlopFactor(distance);
        }

        @Override
        public float computePayloadFactor(int doc, int start, int end, BytesRef payload) {
            return decoratedSimScorer.computePayloadFactor(doc, start, end, payload);
        }

        private int getTermRecencyPayload(int doc, BytesRef term) {
            try {
                Terms terms = context.reader().getTermVector(doc, recencyStats.field);
                if (terms == null) {
                    logger.error("term vector not exists, returning default recency = " + DEFAULT_RECENCY +
                            " in field = "+ recencyStats.field);
                    return DEFAULT_RECENCY;
                }

                TermsEnum termsEnum = terms.iterator();
                if (!termsEnum.seekExact(term)) {
                    logger.error("seekExact failed, returning default recency = " + DEFAULT_RECENCY + " in field = "
                            + recencyStats.field);
                    return DEFAULT_RECENCY;
                }

                PostingsEnum dpEnum = termsEnum.postings(null, PostingsEnum.ALL);
                dpEnum.nextDoc();
                dpEnum.nextPosition();
                BytesRef payload = dpEnum.getPayload();
                if (payload == null) {
                    logger.error("getPayload failed, returning default recency = " + DEFAULT_RECENCY + " in field = "
                            + recencyStats.field);
                    return DEFAULT_RECENCY;
                }
                return PayloadHelper.decodeInt(payload.bytes, payload.offset);
            } catch (Exception ex) {
                logger.error("Unexpected exception, returning default position = " + DEFAULT_RECENCY +
                        " in field = " + recencyStats.field, ex);

                return DEFAULT_RECENCY;
            }
        }

        @Override
        public Explanation explain(int doc, Explanation freq) {
            float bootsScore = score(doc, freq.getValue());
            List<Explanation> subs = asList(recencyBoostExplanation, decoratedSimScorer.explain(doc,freq));

            return Explanation.match(bootsScore,
                    "recency boost score(doc="+doc+",freq="+freq+"), product of:", subs);
        }
    }

    private float computeRecencyBoostFactor(long termRecencyInHours) {
        if (termRecencyInHours <= 0) {
            return 1.0f;
        }

        if (termRecencyInHours <= 4){
            return 10.0f;
        }

        if (termRecencyInHours <= 24) {
            return 5.0f;
        }

        if (termRecencyInHours <= 48) {
            return 3.0f;
        }

        if (termRecencyInHours <= 72) {
            return 2.0f;
        }

        if (termRecencyInHours <= 120){
            return 1.2f;
        }

        return 1.0f;
    }

    private long getTermRecency(Instant now, int recencyPayload) {
        Instant termTimestamp = RecencyEpoch.plus(recencyPayload, ChronoUnit.HOURS);
        return Duration.between(termTimestamp, now).toHours();
    }
}
