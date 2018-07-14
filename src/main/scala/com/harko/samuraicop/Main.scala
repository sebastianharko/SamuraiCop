package com.harko.samuraicop

import akka.NotUsed
import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.{ContentTypes, HttpEntity}
import akka.http.scaladsl.server.Directives._
import akka.stream.ActorMaterializer
import akka.stream.alpakka.postgresqlcdc.scaladsl.ChangeDataCapture
import akka.stream.alpakka.postgresqlcdc.{ChangeSet, PgCdcSourceSettings, PostgreSQLInstance}
import akka.stream.scaladsl.{BroadcastHub, Keep, RunnableGraph, Source}

import scala.io.StdIn

object Main extends App {

  implicit val system: ActorSystem = ActorSystem()

  implicit val materializer: ActorMaterializer = ActorMaterializer()

  implicit val executionContext = system.dispatcher

  val jdbcConnectionString = s"jdbc:postgresql://localhost:5432/pgdb1?user=pguser&password=pguser"
  val postgreSQLInstance = PostgreSQLInstance("", "samurai_cop")

  val producer: Source[ChangeSet, NotUsed] =
    ChangeDataCapture.source(postgreSQLInstance, PgCdcSourceSettings())

  val runnableGraph: RunnableGraph[Source[ChangeSet, NotUsed]] =
    producer.toMat(BroadcastHub.sink(bufferSize = 256))(Keep.right)


  val fromProducer: Source[ChangeSet, NotUsed] = runnableGraph.run()

  val route =
    path("hello") {
      get {
        complete(HttpEntity(ContentTypes.`text/html(UTF-8)`, "<h1>Say hello to akka-http</h1>"))
      }
    }


  val bindingFuture = Http().bindAndHandle(route, "localhost", 8080)

  println(s"Server online at http://localhost:8080/\nPress RETURN to stop...")
  StdIn.readLine() // let it run until user presses return
  bindingFuture
    .flatMap(_.unbind()) // trigger unbinding from the port
    .onComplete(_ => system.terminate()) // and shutdown when done
}
