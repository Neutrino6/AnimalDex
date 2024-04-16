FROM postgres

RUN rm -rf /docker-entrypoint-initdb.d/*

ADD ./scripts/init_userdb.sql /docker-entrypoint-initdb.d