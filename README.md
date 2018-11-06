# Elasticsearch term recency boosting similarity plugin

Elasticsearch custom similarity plugin to calculate score based on TF * IDF  and term recency. So that terms with the most recent timestamp have higher scores.
Similarity uses Elasticsearch TF * IDF (BM25) similarity and multiply given score with term recency score.   

Plugin is inspired by https://github.com/sdauletau/elasticsearch-position-similarity 

## Build

    ./gradlew clean assemble
    
Plugin zip file is then located in build/distributions folder


## Install / Remove plugin from Elasticsearch
    elasticsearch-plugin install file:////ga-2502-elasticsearch-plugins-5.6.10.zip
    elasticsearch-plugin remove BM25-recency


## Plugin settings

**decay_function** - Decay functions score a term recency with a function that decays depending on the distance of current time. We have _exp_, _linear_ and _gauss_. Default linear.

**scale** - Defines the number of hours from now at which the computed score will equal decay parameter. Default 24.

**decay** - The decay parameter defines how terms are scored at the distance given at scale. Default 0.5.

**weight** - The recency score booster to enhance recency weight. Default 1.0.             

More about decay functions can be found on Elasticsearch page https://www.elastic.co/guide/en/elasticsearch/reference/current/query-dsl-function-score-query.html#function-decay


## Create index
```
PUT /test_index
{
  "settings": {
    "similarity": {
      "recencySimilarity": {
        "type": "BM25-recency",
        "decay_function": "exp",
        "scale": "24",
        "decay": "0.5",
        "weight": "1"
      }
    },
    "analysis": {
      "analyzer": {
        "recencyPayloadAnalyzer": {
          "type": "custom",
          "tokenizer": "whitespace",
          "filter": [
            "timestampPayloadFilter"
          ]
        }
      },
      "filter": {
        "timestampPayloadFilter": {
          "delimiter": "|",
          "encoding": "int",
          "type": "delimited_payload_filter"
        }
      }
    }
  }
}

```


## Create mapping
```
PUT /test_index/test_type/_mapping
{
  "test_type": {
    "properties": {
      "field1": {
        "type": "text"
      },
      "field2": {
        "type": "text",
        "norms": false,
        "term_vector": "with_positions_offsets_payloads",
        "analyzer": "recencyPayloadAnalyzer",
        "similarity": "recencySimilarity"
      }
    }
  }
}

``` 

## Add documents
Term timestamp is defined as a number of hours since epoch time.

Change term timestamp to something more recent:

Javascript
```
console.dir(parseInt(new Date().getTime() / 3600000))

```

Python
```python
import time
print(int(time.time() / 3600))
```


```
PUT /test_index/test_type/1
{"field1" : "bar foo", "field2" : "bar|428192 foo|428192"}


PUT /test_index/test_type/2
{"field1" : "foo foo bar bar bar", "field2" : "foo|428191 foo|428190 bar|428191 bar|428190 bar|428189"}


PUT /test_index/test_type/3
{"field1" : "bar bar foo foo", "field2" : "bar|428150 bar|428150 foo|428150 foo|428150"}

POST /test_index/_refresh

```

## Search
```
GET /test_index/test_type/_search?pretty=true
{
  "explain": true,
  "query": {
    "match": {
      "field2": "foo"
    }
  }
}
```