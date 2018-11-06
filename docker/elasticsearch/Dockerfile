FROM docker.elastic.co/elasticsearch/elasticsearch:5.6.10

ADD plugins/ga-2502-elasticsearch-plugins-5.6.10.zip /tmp/plugins/
RUN elasticsearch-plugin install file:///tmp/plugins/ga-2502-elasticsearch-plugins-5.6.10.zip