name := "SamuraiCop"

version := "0.1"

scalaVersion := "2.13.1"

resolvers ++= Seq(
  Resolver.bintrayRepo("lonelyplanet", "maven"),
  Resolver.bintrayRepo("brechtian", "maven")
)

libraryDependencies += "com.brecht" %% "cdc" % "0.1"

val akkaVersion = "2.6.5"
val akkaHttpVersion = "10.1.12"

libraryDependencies ++= Seq(
  "com.brecht" %% "cdc" % "0.1",
  "com.typesafe.akka" %% "akka-http" % akkaHttpVersion,
  "com.lonelyplanet" %% "prometheus-akka-http" % "0.5.0",
  "org.json4s" %% "json4s-native" % "3.6.9",
  "com.typesafe.akka" %% "akka-stream" % akkaVersion,
  "com.typesafe.akka" %% "akka-slf4j" % akkaVersion,
  "com.zaxxer" % "HikariCP" % "3.4.2",
  "ch.qos.logback" % "logback-classic" % "1.2.3"
)

