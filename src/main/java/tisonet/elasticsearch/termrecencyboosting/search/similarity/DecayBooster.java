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

import org.apache.lucene.search.Explanation;
import org.elasticsearch.index.query.functionscore.DecayFunction;
import org.elasticsearch.index.query.functionscore.ExponentialDecayFunctionBuilder;
import org.elasticsearch.index.query.functionscore.GaussDecayFunctionBuilder;
import org.elasticsearch.index.query.functionscore.LinearDecayFunctionBuilder;

class DecayBooster {
    private final DecayFunction decayFunction;
    private final double processedScale;
    private final double weight;

    DecayBooster(String decayFunctionName, double scale, double decay, double weight) {
        this.decayFunction = buildDecayFunction(decayFunctionName);
        this.processedScale = this.decayFunction.processScale(scale, decay);
        this.weight = weight;
    }

    private DecayFunction buildDecayFunction (String decayFunctionName) {
        if (decayFunctionName.equals(GaussDecayFunctionBuilder.NAME)){
            return GaussDecayFunctionBuilder.GAUSS_DECAY_FUNCTION;
        }

        if (decayFunctionName.equals(ExponentialDecayFunctionBuilder.NAME)){
            return ExponentialDecayFunctionBuilder.EXP_DECAY_FUNCTION;
        }

        return LinearDecayFunctionBuilder.LINEAR_DECAY_FUNCTION;
    }

    float getBoost(long recency){
       return (float)(this.weight * decayFunction.evaluate((double) recency, processedScale));
    }

    Explanation explain(long recency) {
        return Explanation.match(getBoost(recency), "term recency boost, computed as weight * decayScore(termRecency) from:",
                Explanation.match((float)weight, "weight"),
                Explanation.match((float)recency, "termRecency"),
                Explanation.match((float) decayFunction.evaluate((double) recency, processedScale), "decayScore(termRecency) from:",
                        decayFunction.explainFunction("termRecency", recency, processedScale)));
    }
}
