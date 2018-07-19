name := "SamuraiCop"

version := "0.1"

scalaVersion := "2.12.6"

libraryDependencies += "com.lightbend.akka" %% "akka-stream-alpakka-postgresql-cdc" % "0.18+199-ac298282"

libraryDependencies += "com.typesafe.akka" %% "akka-http" % "10.1.3"

libraryDependencies += "org.postgresql" % "postgresql" % "42.2.1"

libraryDependencies += "org.json4s" %% "json4s-native" % "3.5.4"