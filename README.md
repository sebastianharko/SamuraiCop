# Create table

```
 ----------------------------
-- Table structure for users
-- ----------------------------
DROP TABLE IF EXISTS "public"."users";
CREATE TABLE "public"."users" (
  "id" int4 NOT NULL,
  "lat" float8 NOT NULL,
  "lng" varchar(255) COLLATE "pg_catalog"."default" NOT NULL
)
;

-- ----------------------------
-- Primary Key structure for table users
-- ----------------------------
ALTER TABLE "public"."users" ADD CONSTRAINT "users_pkey" PRIMARY KEY ("id");
```


# Insert Data

```
DELETE FROM users;
DO $$
BEGIN
   FOR i IN 1..150 LOOP
     INSERT INTO users VALUES(i, floor(random()*(40-25+1))+25,   -1 * (floor(random()*(120-80+1))+80));
   END LOOP;
END; $$
```
