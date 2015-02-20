FROM java

MAINTAINER Larry Ogrodnek <ogrodnek@gmail.com>

ADD target/dependency-repository-indexer-*.tar.gz /opt

RUN ln -s /opt/dependency-repository-indexer* /opt/dependency-repository-indexer

ENTRYPOINT ["/opt/dependency-repository-indexer/bin/s3-indexer"]