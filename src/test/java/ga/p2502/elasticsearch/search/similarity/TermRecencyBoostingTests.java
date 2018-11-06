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

import ga.p2502.elasticsearch.index.analysis.RecencyPayloadAnalyzerFactory;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.TopDocs;

import org.apache.lucene.search.similarities.BM25Similarity;
import org.apache.lucene.search.similarities.Similarity;
import org.apache.lucene.store.Directory;
import org.apache.lucene.util.IOUtils;
import org.apache.lucene.util.LuceneTestCase;

public class TermRecencyBoostingTests extends LuceneTestCase {
    private Similarity decoratedSimilarity;
    private Similarity termRecencyBoosting;
    private Directory directory;
    private IndexReader indexReader;
    private IndexSearcher indexSearcher;


    @Override
    public void setUp() throws Exception {
        super.setUp();
        this.decoratedSimilarity = new BM25Similarity();
        this.termRecencyBoosting = new TermRecencyBoosting(decoratedSimilarity);

        directory = newDirectory();
        try (IndexWriter indexWriter = new IndexWriter(directory, newIndexWriterConfig(RecencyPayloadAnalyzerFactory.create()))) {
            Document document = new Document();
            document.add(new TextFieldWithPayload("web_kw", "auto motto", Field.Store.YES));
            document.add(new TextFieldWithPayload("web_kw",
                    "java|" + createTermTimestamp(1)
                            + " python|"+ createTermTimestamp(24) , Field.Store.YES));


            indexWriter.addDocument(document);
            indexWriter.commit();
        }
        indexReader = DirectoryReader.open(directory);
        indexSearcher = newSearcher(indexReader);
    }

    @Override
    public void tearDown() throws Exception {
        IOUtils.close(indexReader, directory);
        super.tearDown();
    }

    public void testWhenPayloadNotExistsReturnsTheSameScoreAsDecoratedSimilarity() throws Exception {
        BooleanQuery.Builder query = new BooleanQuery.Builder();
        query.add(new TermQuery(new Term("web_kw", "auto")), BooleanClause.Occur.SHOULD);
        query.add(new TermQuery(new Term("web_kw", "motto")), BooleanClause.Occur.SHOULD);

        indexSearcher.setSimilarity(decoratedSimilarity);
        TopDocs results1 = indexSearcher.search(query.build(), 10);

        indexSearcher.setSimilarity(termRecencyBoosting);
        TopDocs results2 = indexSearcher.search(query.build(), 10);

        assertEquals(results1.scoreDocs[0].score, results2.scoreDocs[0].score, 0);
    }

    public void testRecencyBoosting() throws Exception {
        BooleanQuery.Builder query = new BooleanQuery.Builder();
        query.add(new TermQuery(new Term("web_kw", "python")), BooleanClause.Occur.SHOULD);
        query.add(new TermQuery(new Term("web_kw", "java")), BooleanClause.Occur.SHOULD);

        indexSearcher.setSimilarity(decoratedSimilarity);
        TopDocs results1 = indexSearcher.search(query.build(), 10);

        indexSearcher.setSimilarity(termRecencyBoosting);
        TopDocs results2 = indexSearcher.search(query.build(), 10);

        assertTrue(results1.scoreDocs[0].score < results2.scoreDocs[0].score);
    }


    public Long createTermTimestamp(int recencyInHours) {
        Instant termTimestamp = Instant.now().minus(recencyInHours, ChronoUnit.HOURS);
        return termTimestamp.getEpochSecond() / 3600;
    }

}
