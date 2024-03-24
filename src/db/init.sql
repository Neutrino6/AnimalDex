CREATE DATABASE ANIMALDEXdb;

DROP TABLE IF EXISTS user;
DROP TABLE IF EXISTS administrator;
DROP TABLE IF EXISTS operator;
DROP TABLE IF EXISTS comment;
DROP TABLE IF EXISTS notify;
DROP TABLE IF EXISTS report;
DROP TABLE IF EXISTS certification;
DROP TABLE IF EXISTS animal;
DROP TABLE IF EXISTS SpecialEvent;
DROP TABLE IF EXISTS alarm;


CREATE TABLE users (
    email varchar(255) PRIMARY KEY,
    passw varchar(255),
    username varchar(255),
    firstname varchar(255),
    surname varchar(255),
    points int,
    birthday date,
    fav_animal varchar(255),
    forum_notify boolean,
    emergency_notify boolean,
    userImage bytea
);

CREATE TABLE administrator (
    a_email varchar(255) PRIMARY KEY,
    CONSTRAINT fk_adm_user FOREIGN KEY(a_email) REFERENCES user(email)    
);

CREATE TABLE operator (
    o_email varchar(255) PRIMARY KEY,
    code varchar(255) PRIMARY KEY
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

CREATE TABLE certification (
    cert_image bytea PRIMARY KEY,
    animal_id int,
    CONSTRAINT fk_cert_animal foreign key (animal_id) references animal(a_id)   
);

CREATE TABLE animal (
    a_id int PRIMARY KEY,
    a_name varchar(255),
    details varchar(2000),
    regions varchar(2000),
    std_points int
);

CREATE TABLE SpecialEvent (
    e_id int PRIMARY KEY,
    StartDate date,
    EndDate date,
    BonusPoints int,
    animal_id int,
    CONSTRAINT fk_even_animal foreign key (animal_id) references animal(a_id);      
);

CREATE TABLE alarm (
    alarm_id int PRIMARY KEY,
    position varchar(255),
    alarm_date date,
    alarm_status varchar(255),
    info varchar(2000),
    UserEval ENUM ('0', '1', '2', '3', '4', '5'),
    OperatorEval ENUM ('0', '1', '2', '3', '4', '5'),
);
