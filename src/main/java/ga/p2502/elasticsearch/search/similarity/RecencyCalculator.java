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
