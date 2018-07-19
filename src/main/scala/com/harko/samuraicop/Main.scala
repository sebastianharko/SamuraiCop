package com.harko.samuraicop

import akka.NotUsed
import akka.actor.ActorSystem
import akka.event.Logging
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.sse.ServerSentEvent
import akka.http.scaladsl.server.Directives._
import akka.stream.alpakka.postgresqlcdc.scaladsl.ChangeDataCapture
import akka.stream.alpakka.postgresqlcdc.{PgCdcSourceSettings, PostgreSQLInstance, RowInserted}
import akka.stream.scaladsl.{BroadcastHub, Keep, Source}
import akka.stream.{ActorMaterializer, Attributes}
import org.json4s.DefaultFormats
import org.json4s.native.Serialization.write

import scala.concurrent.duration._
import scala.io.StdIn

object Main extends App {

  implicit val system: ActorSystem = ActorSystem()

  implicit val materializer: ActorMaterializer = ActorMaterializer()

  implicit val executionContext = system.dispatcher

  val postgreSQLPortNumber = 5432
  val jdbcConnectionString = s"jdbc:postgresql://localhost:$postgreSQLPortNumber/pgdb1?user=pguser&password=pguser"

  val postgreSQLInstance = PostgreSQLInstance(jdbcConnectionString, "samurai_cop")

  implicit val formats = DefaultFormats

  case class UserRegistered(lat: Double, lng: Double)

  val singleProducer: Source[UserRegistered, NotUsed] =
    ChangeDataCapture.source(postgreSQLInstance, PgCdcSourceSettings())
      .mapConcat(_.changes)
      .log("postgresqlcdc", cs ⇒ s"captured change: ${cs.toString}")
      .throttle(1, per = 500 milliseconds)
      .withAttributes(Attributes.logLevels(onElement = Logging.InfoLevel))
      .collect {
        case RowInserted(_, "users", _, _, fields)  ⇒
          val m = fields.map(f ⇒ f.columnName → f.value).toMap
          val lat = m("lat").toDouble
          val lng = m("lng").toDouble
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
