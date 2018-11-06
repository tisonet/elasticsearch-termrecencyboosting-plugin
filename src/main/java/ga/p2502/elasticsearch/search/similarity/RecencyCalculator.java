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

import java.time.Duration;
import java.time.Instant;

class RecencyCalculator {

    private static int SECONDS_IN_HOUR =  60 * 60;

    static long calculateRecency(Instant now, int timestampInHours) {
        Instant termInstant = Instant.ofEpochSecond(timestampInHours * SECONDS_IN_HOUR);
        return Duration.between(termInstant, now).toHours();
    }

    static long calculateRecency(int timestampInHours) {
        return calculateRecency(Instant.now(), timestampInHours);
    }

}
