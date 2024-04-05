CREATE DATABASE CentralDB;

DROP TABLE IF EXISTS users CASCADE;
DROP TABLE IF EXISTS administrator CASCADE;
DROP TABLE IF EXISTS operator CASCADE;
DROP TABLE IF EXISTS comment CASCADE;
DROP TABLE IF EXISTS notify CASCADE;
DROP TABLE IF EXISTS report CASCADE;
DROP TABLE IF EXISTS certification CASCADE;
DROP TABLE IF EXISTS animal CASCADE;
DROP TABLE IF EXISTS SpecialEvent CASCADE;
DROP TABLE IF EXISTS alarm CASCADE;
DROP TYPE IF EXISTS evaluation;

CREATE TYPE evaluation AS ENUM ('0', '1','2', '3', '4', '5');

CREATE TABLE animal (
    a_id serial PRIMARY KEY,
    a_name varchar(255) not null,
    details varchar(2000) not null,
    regions varchar(2000),
    std_points int not null
);

CREATE TABLE users (
    user_id serial PRIMARY KEY,
    email varchar(255) not null,
    passw varchar(255) not null,
    username varchar(255) not null,
    firstname varchar(255),
    surname varchar(255),
    points int default 0,
    birthday date,
    fav_animal int references animal(a_id),
    forum_notify boolean default false,
    emergency_notify boolean default false,
    userImage bytea,
    administrator boolean default false,
    unique(email)
);

CREATE TABLE operator (
    o_email varchar(255) PRIMARY KEY,
    code varchar(255) not null,
	unique(code)
);

CREATE TABLE comment (
    c_id serial PRIMARY KEY,
    c_date date,
    c_content varchar(2000)    
);

CREATE TABLE notify (
    n_id serial PRIMARY KEY,
    n_content varchar(255) 
);

CREATE TABLE report (
    r_id serial PRIMARY KEY,
    r_date date,
    r_status varchar(255)    
);

CREATE TABLE certification (
    animal_id int references animal(a_id),
    user_id int references users(user_id),
    cert_date date not null,
    primary key (animal_id,user_id)
);

CREATE TABLE SpecialEvent (
    e_id serial PRIMARY KEY,
    StartDate date not null,
    EndDate date not null,
    BonusPoints int not null,
    animal_id int not null,
    CONSTRAINT fk_even_animal foreign key (animal_id) references animal(a_id)      
);

CREATE TABLE alarm (
    alarm_id serial PRIMARY KEY,
    position varchar(255),
    alarm_date date,
    alarm_status varchar(255),
    info varchar(2000),
    UserEval evaluation,
    OperatorEval evaluation
);

INSERT INTO animal (a_name, details, regions, std_points)
VALUES
    ('Dog', 'Domestic mammal with four legs.', 'Worldwide', 100),
    ('Cat', 'Domestic feline with independent behaviors.', 'Worldwide', 90),
    ('Bear', 'Large and robust mammal inhabiting forests.', 'North America, Europe, Asia', 150),
    ('Pigeon', 'Medium-sized bird adaptable to urban environments.', 'Worldwide', 80);

INSERT INTO users (user_id,email, passw, username, firstname, surname, birthday, fav_animal, forum_notify, emergency_notify, userImage, administrator)
VALUES (999,'example@example.com', 'password123', 'example_user', 'John', 'Doe', '1990-01-01', 1, true, false, NULL, false);

