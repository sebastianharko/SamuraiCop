# SamuraiCop

# SQL Script
sql```
DELETE FROM users;
DO $$
BEGIN
   FOR i IN 1..150 LOOP
     INSERT INTO users VALUES(i, floor(random()*(40-25+1))+25,   -1 * (floor(random()*(120-80+1))+80));
   END LOOP;
END; $$
```