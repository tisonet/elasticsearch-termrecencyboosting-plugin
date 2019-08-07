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

import org.apache.lucene.util.LuceneTestCase;

import java.time.Instant;

public class RecencyCalculatorTests extends LuceneTestCase {

    public void testCalculateRecency() throws Exception {
        Instant now = Instant.parse("2018-11-06T12:00:00.000Z");

        assertEquals(RecencyCalculator.calculateRecency(now, 428196), 0);
        assertEquals(RecencyCalculator.calculateRecency(now, 428195), 1);
        assertEquals(RecencyCalculator.calculateRecency(now, 428180), 16);
    }
}
