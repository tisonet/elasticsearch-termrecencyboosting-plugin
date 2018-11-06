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

import org.apache.lucene.util.LuceneTestCase;

public class DecayBoosterTests extends LuceneTestCase {

    public void testLinearBoosting() throws Exception {
        DecayBooster booster = new DecayBooster("linear", 24, 0.5, 1);

        assertEquals(booster.getBoost(0), 1, 0);
        assertEquals(booster.getBoost(12), 0.75, 0);
        assertEquals(booster.getBoost(24), 0.5, 0);
        assertEquals(booster.getBoost(48), 0, 0);
        assertEquals(booster.getBoost(72), 0, 0);
    }

    public void testLinearBoostingWithWeight() throws Exception {
        DecayBooster booster = new DecayBooster("linear", 24, 0.5, 10);

        assertEquals(booster.getBoost(0), 10, 0);
        assertEquals(booster.getBoost(12), 7.5, 0);
        assertEquals(booster.getBoost(24), 5, 0);
        assertEquals(booster.getBoost(48), 0, 0);
        assertEquals(booster.getBoost(72), 0, 0);
    }

    public void testExpBoosting() throws Exception {
        DecayBooster booster = new DecayBooster("exp", 24, 0.5, 1);

        assertEquals(booster.getBoost(0), 1, 0);
        assertEquals(booster.getBoost(12), 0.70, 0.1);
        assertEquals(booster.getBoost(24), 0.5, 0);
        assertEquals(booster.getBoost(48), 0.25, 0);
        assertEquals(booster.getBoost(72), 0.125, 0);
    }

    public void testGaussBoosting() throws Exception {
        DecayBooster booster = new DecayBooster("gauss", 24, 0.5, 1);

        assertEquals(booster.getBoost(0), 1, 0);
        assertEquals(booster.getBoost(12), 0.85, 0.1);
        assertEquals(booster.getBoost(24), 0.5, 0);
        assertEquals(booster.getBoost(48), 0.0625, 0);
        assertEquals(booster.getBoost(72), 0.002, 0.001);
    }

}
