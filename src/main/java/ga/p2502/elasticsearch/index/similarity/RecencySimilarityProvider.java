package ga.p2502.elasticsearch.index.similarity;

import org.elasticsearch.common.inject.Inject;
import org.elasticsearch.common.inject.assistedinject.Assisted;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.index.similarity.AbstractSimilarityProvider;


public class RecencySimilarityProvider extends AbstractSimilarityProvider {
    private final RecencySimilarity similarity;

    @Inject
    public RecencySimilarityProvider(@Assisted String name, @Assisted Settings settings) {
        super(name);
        this.similarity = new RecencySimilarity(settings);
    }

    public RecencySimilarity get() {
        return similarity;
    }
}