FROM docker.elastic.co/elasticsearch/elasticsearch:5.6.10

ADD plugins/*.zip /tmp/plugins/
RUN elasticsearch-plugin install file:///tmp/plugins/tisonet-elasticsearch-termrecencyboosting-plugin-5.6.10.zip
RUN elasticsearch-plugin install file:///tmp/plugins/rocchio-5.6.10-SNAPSHOT.zip
