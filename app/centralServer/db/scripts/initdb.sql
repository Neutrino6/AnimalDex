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

INSERT INTO animal (a_name, details, regions, std_points) VALUES
    ('Wolf', 'The wolf is a carnivorous mammal that inhabits various regions across Italy.', 'Northern Italy, Central Italy, Southern Italy, Islands of Italy', 100),
    ('Sheep', 'The sheep is a domesticated ruminant mammal commonly found in agricultural regions of Northern Italy and Central Italy.', 'Northern Italy, Central Italy', 80),
    ('Dog', 'The dog is a domesticated carnivorous mammal that is popular across all regions of Italy.', 'Northern Italy, Central Italy, Southern Italy, Islands of Italy', 90),
    ('Cat', 'The cat is a small carnivorous mammal commonly found in households across Italy.', 'All regions of Italy', 85),
    ('Pigeon', 'The pigeon is a stout-bodied bird commonly found in urban areas across all regions of Italy.', 'All regions of Italy', 70),
    ('Snake', 'The snake is a long, limbless reptile found in various habitats across Italy, including forests, grasslands, and wetlands.', 'Northern Italy, Central Italy, Southern Italy, Islands of Italy', 75),
    ('Bull', 'The bull is an adult male bovine commonly bred for meat production in regions of Northern Italy and Central Italy.', 'Northern Italy, Central Italy', 95),
    ('Bear', 'The bear is a large carnivorous mammal found in forested areas of Northern Italy and Central Italy.', 'Northern Italy, Central Italy', 85),
    ('Lynx', 'The lynx is a medium-sized wild cat native to forests of Northern Italy and Central Italy.', 'Northern Italy, Central Italy', 90),
    ('Deer', 'The deer is a hoofed ruminant mammal commonly found in forests and woodlands across all regions of Italy.', 'All regions of Italy', 80),
    ('Roe Deer', 'The roe deer is a small and delicate species of deer found in forests of Northern Italy and Central Italy.', 'Northern Italy, Central Italy', 75),
    ('Wild Boar', 'The wild boar is a suid native to forests and rural areas across all regions of Italy.', 'All regions of Italy', 85),
    ('Beech Marten', 'The beech marten is a small carnivorous mammal native to forests of Northern Italy and Central Italy.', 'Northern Italy, Central Italy', 70),
    ('Pine Marten', 'The pine marten is a mammal native to forests of Northern Italy and Central Italy.', 'Northern Italy, Central Italy', 75),
    ('Squirrel', 'The squirrel is a small rodent commonly found in urban parks and wooded areas across all regions of Italy.', 'All regions of Italy', 65),
    ('Hedgehog', 'The hedgehog is a spiny mammal commonly found in gardens and rural areas across all regions of Italy.', 'All regions of Italy', 70),
    ('Hare', 'The hare is a fast-running mammal commonly found in open grasslands and meadows across all regions of Italy.', 'All regions of Italy', 75),
    ('Dormouse', 'The dormouse is a small rodent commonly found in forests and woodlands across all regions of Italy.', 'All regions of Italy', 65),
    ('Mouse', 'The mouse is a small rodent commonly found in urban and rural areas across all regions of Italy.', 'All regions of Italy', 60),
    ('Frog', 'The frog is an amphibian commonly found in wetlands and ponds across all regions of Italy.', 'All regions of Italy', 70),
    ('Newt', 'The newt is a small amphibian commonly found in ponds and slow-moving streams across all regions of Italy.', 'All regions of Italy', 65),
    ('Salamander', 'The salamander is an amphibian commonly found in forests and woodlands across all regions of Italy.', 'All regions of Italy', 70),
    ('Tortoise', 'The tortoise is a reptile commonly found in grasslands and scrublands across all regions of Italy.', 'All regions of Italy', 75),
    ('Lizard', 'The lizard is a reptile commonly found in rocky habitats and sunlit areas across all regions of Italy.', 'All regions of Italy', 65),
    ('Viper', 'The viper is a venomous snake commonly found in rocky hillsides and woodlands across all regions of Italy.', 'All regions of Italy', 80),
    ('Eagle', 'The eagle is a large bird of prey commonly found in mountainous regions and forests across all regions of Italy.', 'All regions of Italy', 90),
    ('Falcon', 'The falcon is a bird of prey commonly found in open landscapes and rocky cliffs across all regions of Italy.', 'All regions of Italy', 85),
    ('Buzzard', 'The buzzard is a bird of prey commonly found in open countryside and woodland edges across all regions of Italy.', 'All regions of Italy', 80),
    ('Owl', 'The owl is a nocturnal bird of prey commonly found in forests and wooded areas across all regions of Italy.', 'All regions of Italy', 85),
    ('Raven', 'The raven is a large, black bird commonly found in forests and mountainous regions across all regions of Italy.', 'All regions of Italy', 80),
    ('Partridge', 'The partridge is a game bird commonly found in agricultural areas and scrubland across all regions of Italy.', 'All regions of Italy', 75),
    ('Lark', 'The lark is a small songbird commonly found in open grasslands and meadows across all regions of Italy.', 'All regions of Italy', 70),
    ('Sparrow', 'The sparrow is a small passerine bird commonly found in urban and rural areas across all regions of Italy.', 'All regions of Italy', 65),
    ('Salmon', 'The salmon is a fish commonly found in rivers and streams across all regions of Italy.', 'All regions of Italy', 75),
    ('Eel', 'The eel is a fish commonly found in freshwater rivers and lakes as well as in coastal waters across all regions of Italy.', 'All regions of Italy', 70),
    ('Trout', 'The trout is a freshwater fish commonly found in rivers and streams across all regions of Italy.', 'All regions of Italy', 70),
    ('Sturgeon', 'The sturgeon is a large fish commonly found in rivers and lakes across all regions of Italy.', 'All regions of Italy', 80),
    ('Sea Bass', 'The sea bass is a marine fish commonly found in coastal waters and estuaries across all regions of Italy.', 'All regions of Italy', 75),
    ('Bream', 'The bream is a freshwater fish commonly found in lakes, rivers, and ponds across all regions of Italy.', 'All regions of Italy', 70),
    ('Gilt-head Bream', 'The gilt-head bream is a marine fish commonly found in coastal waters and estuaries across all regions of Italy.', 'All regions of Italy', 75),
    ('Butterfly', 'The butterfly is an insect commonly found in gardens, meadows, and woodland edges across all regions of Italy.', 'All regions of Italy', 65),
    ('Beetle', 'The beetle is an insect commonly found in various habitats including gardens, forests, and agricultural fields across all regions of Italy.', 'All regions of Italy', 60),
    ('Snail', 'The snail is a gastropod mollusk commonly found in gardens, forests, and fields across all regions of Italy.', 'All regions of Italy', 55),
    ('Spider', 'The spider is an arachnid commonly found in gardens, forests, and urban areas across all regions of Italy.', 'All regions of Italy', 60),
    ('Bee', 'The bee is a flying insect commonly found in gardens, meadows, and agricultural areas across all regions of Italy.', 'All regions of Italy', 65),
    ('Ant', 'The ant is a social insect commonly found in gardens, forests, and urban areas across all regions of Italy.', 'All regions of Italy', 60),
    ('Grasshopper', 'The grasshopper is an insect commonly found in grasslands, meadows, and agricultural fields across all regions of Italy.', 'All regions of Italy', 60),
    ('Marmot', 'The marmot is a large ground squirrel commonly found in mountainous regions and alpine meadows of Northern Italy and Central Italy.', 'Northern Italy, Central Italy', 70),
    ('Fox', 'The fox is a carnivorous mammal commonly found in forests, grasslands, and urban areas across all regions of Italy.', 'All regions of Italy', 85),
    ('Badger', 'The badger is a burrowing mammal commonly found in woodlands and scrublands of Northern Italy and Central Italy.', 'Northern Italy, Central Italy', 80),
    ('Porcupine', 'The porcupine is a rodent commonly found in forests, woodlands, and rocky areas across all regions of Italy.', 'All regions of Italy', 75),
    ('Weasel', 'The weasel is a small carnivorous mammal commonly found in forests, grasslands, and rural areas across all regions of Italy.', 'All regions of Italy', 70),
    ('Cow', 'The cow is a domesticated mammal commonly found in agricultural areas across all regions of Italy.', 'All regions of Italy', 80),
    ('Elk', 'The elk is a large ungulate commonly found in forests and woodlands of Northern Italy and Central Italy.', 'Northern Italy, Central Italy', 85),
    ('Seal', 'The seal is a marine mammal commonly found in coastal waters and rocky shores of the Islands of Italy.', 'Islands of Italy', 75),
    ('Dolphin', 'The dolphin is a highly intelligent marine mammal commonly found in coastal waters and offshore areas of the Islands of Italy.', 'Islands of Italy', 80),
    ('Whale', 'The whale is a massive marine mammal commonly found in deep offshore waters of the Islands of Italy.', 'Islands of Italy', 90),
    ('Turtle', 'The turtle is a reptile commonly found in coastal areas, wetlands, and freshwater bodies across all regions of Italy.', 'All regions of Italy', 75),
    ('Hippopotamus', 'The hippopotamus is a large herbivorous mammal commonly found in wetlands and freshwater rivers of the Islands of Italy.', 'Islands of Italy', 85),
    ('Mosquito', 'The mosquito is a flying insect known for its itchy bites and role in spreading diseases such as malaria and dengue fever. It is commonly found in urban and rural areas across all regions of Italy.', 'All regions of Italy', 60),
    ('Fly', 'The fly is a flying insect known for its rapid flight and ability to spread disease-causing pathogens. It is commonly found in urban and rural areas across all regions of Italy.', 'All regions of Italy', 55),
    ('Cockroach', 'The cockroach is an insect known for its resilient nature and association with unsanitary conditions. It is commonly found in urban and rural areas across all regions of Italy.', 'All regions of Italy', 50),
    ('Tarantula', 'The tarantula is a large spider known for its hairy appearance and venomous bite. It is commonly found in forests, grasslands, and rocky areas across all regions of Italy.', 'All regions of Italy', 70),
    ('Scorpion', 'The scorpion is an arachnid known for its pincers and venomous sting. It is commonly found in rocky habitats and arid regions across all regions of Italy.', 'All regions of Italy', 75),
    ('Wasp', 'The wasp is a flying insect known for its slender body and ability to deliver painful stings. It is commonly found in gardens, forests, and urban areas across all regions of Italy.', 'All regions of Italy', 65);


INSERT INTO users (user_id,email, passw, username, firstname, surname, birthday, fav_animal, forum_notify, emergency_notify, userImage, administrator)
VALUES (999,'example@example.com', 'password123', 'example_user', 'John', 'Doe', '1990-01-01', 1, true, false, NULL, false);

