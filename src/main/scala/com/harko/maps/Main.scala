package com.harko.maps

import akka.NotUsed
import akka.actor.ActorSystem
import akka.event.Logging
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.sse.ServerSentEvent
import akka.http.scaladsl.server.Directives._
import akka.stream.scaladsl.{BroadcastHub, Keep, Source}
import akka.stream.{ActorMaterializer, Attributes}
import com.brecht.cdc.scaladsl._
import com.brecht.cdc.{Modes, PgCdcSourceSettings, PostgreSQLInstance, RowInserted}
import com.zaxxer.hikari.{HikariConfig, HikariDataSource}
import org.json4s.DefaultFormats
import org.json4s.native.Serialization.write
import org.slf4j.LoggerFactory

import scala.concurrent.duration._
import scala.io.StdIn

case class UserRegistered(lat: Double, lng: Double)

class Main

object Main extends App {

  val logger = LoggerFactory.getLogger(classOf[Main])

  implicit val system: ActorSystem = ActorSystem()

  implicit val materializer: ActorMaterializer = ActorMaterializer()

  implicit val executionContext = system.dispatcher

  val hikariConfig: HikariConfig = {
    val cfg = new HikariConfig
    cfg.setDriverClassName(classOf[org.postgresql.Driver].getName)
    val url = s"jdbc:postgresql://localhost:5432/docker"
    logger.info("JdbcUrl is {}", url)
    cfg.setJdbcUrl(url)
    cfg.setUsername("docker")
    cfg.setPassword("docker")
    cfg.setMaximumPoolSize(2)
    cfg.setMinimumIdle(0)
    cfg.setPoolName("pg")
    cfg.setConnectionTimeout(300)
    cfg.setValidationTimeout(250)
    cfg
  }

  val hikariDataSource: HikariDataSource = new HikariDataSource(hikariConfig)

  val cdcSource = ChangeDataCapture(PostgreSQLInstance(hikariDataSource))
    .source(
      PgCdcSourceSettings(
        slotName = "cdc",
        mode = Modes.Get,
        dropSlotOnFinish = true,
        closeDataSourceOnFinish = true,
        pollInterval = 500.milliseconds
      )
    )

  implicit val formats = DefaultFormats

  val singleProducer: Source[UserRegistered, NotUsed] =
    cdcSource
      .mapConcat(_.changes)
      .log("postgresqlcdc", cs ⇒ s"Captured change: ${cs.toString}")
      .withAttributes(Attributes.logLevels(onElement = Logging.InfoLevel))
      .collect {
        case RowInserted("public", "users", _, _, data, _) ⇒
          val lat = data("lat").toDouble
          val lng = data("lng").toDouble
          UserRegistered(lat, lng)
      }
      .toMat(BroadcastHub.sink(bufferSize = 256))(Keep.right)
      .run()

  val route = {
    path("events") {
      get {
        complete {
          import akka.http.scaladsl.marshalling.sse.EventStreamMarshalling._
          singleProducer
            .map(event ⇒ write(event))
            .map(json ⇒ ServerSentEvent(json))
            .keepAlive(1.second, () => ServerSentEvent.heartbeat)
        }
      }
    } ~ path("map") {
      getFromResource("map.html")
    }
  }

  val bindingFuture = Http().bindAndHandle(route, "localhost", 8080)

  println(s"Server online at http://localhost:8080/\nPress RETURN to stop...")
  StdIn.readLine() // let it run until user presses return
  bindingFuture
    .flatMap(_.unbind()) // trigger unbinding from the port
    .onComplete(_ => system.terminate()) // and shutdown when done
}
