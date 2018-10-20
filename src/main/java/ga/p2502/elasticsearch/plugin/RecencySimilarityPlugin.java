package ga.p2502.elasticsearch.plugin;

import ga.p2502.elasticsearch.index.similarity.RecencySimilarityProvider;
import org.elasticsearch.index.IndexModule;
import org.elasticsearch.plugins.Plugin;

public class RecencySimilarityPlugin extends Plugin {
    public void onIndexModule(IndexModule indexModule) {
        indexModule.addSimilarity("recency-similarity", RecencySimilarityProvider::new);
    }
}