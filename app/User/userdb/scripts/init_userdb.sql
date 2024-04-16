DROP TABLE IF EXISTS certification;
DROP TABLE IF EXISTS animal;

CREATE TABLE animal (
    a_id int PRIMARY KEY,
    a_name varchar(255),
    details varchar(2000),
    regions varchar(2000),
    std_points int not null
);

CREATE TABLE certification (
    cert_image bytea not null,
    animal_id int references animal(a_id),
    user_id int,
    cert_date date not null,
    primary key (animal_id, user_id)
);


