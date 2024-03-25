CREATE DATABASE ANIMALDEXdb;

DROP TABLE IF EXISTS users;
DROP TABLE IF EXISTS administrator;
DROP TABLE IF EXISTS operator;
DROP TABLE IF EXISTS comment;
DROP TABLE IF EXISTS notify;
DROP TABLE IF EXISTS report;
DROP TABLE IF EXISTS certification;
DROP TABLE IF EXISTS animal;
DROP TABLE IF EXISTS SpecialEvent;
DROP TABLE IF EXISTS alarm;
DROP TYPE IF EXISTS evaluation;

CREATE TYPE evaluation AS ENUM ('0', '1','2', '3', '4', '5');

CREATE TABLE users (
    user_id int PRIMARY KEY,
    email varchar(255),
    passw varchar(255),
    username varchar(255),
    firstname varchar(255),
    surname varchar(255),
    points int,
    birthday date,
    fav_animal varchar(255),
    forum_notify boolean,
    emergency_notify boolean,
    userImage bytea,
    unique(email)
);

CREATE TABLE administrator (
    a_email varchar(255) PRIMARY KEY,
    CONSTRAINT fk_adm_user FOREIGN KEY(a_email) REFERENCES users(email)    
);

CREATE TABLE operator (
    o_email varchar(255) PRIMARY KEY,
    code varchar(255),
	unique(code)
);

CREATE TABLE comment (
    c_id int PRIMARY KEY,
    c_date date,
    c_content varchar(2000)    
);

CREATE TABLE notify (
    n_id int PRIMARY KEY,
    n_content varchar(255) 
);

CREATE TABLE report (
    r_id int PRIMARY KEY,
    r_date date,
    r_status varchar(255)    
);

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
    user_id int references users(user_id),
    primary key (animal_id,user_id)
);

CREATE TABLE SpecialEvent (
    e_id int PRIMARY KEY,
    StartDate date,
    EndDate date,
    BonusPoints int,
    animal_id int,
    CONSTRAINT fk_even_animal foreign key (animal_id) references animal(a_id)      
);

CREATE TABLE alarm (
    alarm_id int PRIMARY KEY,
    position varchar(255),
    alarm_date date,
    alarm_status varchar(255),
    info varchar(2000),
    UserEval evaluation,
    OperatorEval evaluation
);

