FROM postgres

EXPOSE 5433

RUN rm -rf /docker-entrypoint-initdb.d/*

ADD ./scripts/initdb.sql /docker-entrypoint-initdb.d