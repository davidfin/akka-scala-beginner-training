package com.example

import akka.actor.{ActorRef, ActorSystem, Props}
import com.example.EventProxy.{GetActiveSessions, GetCompletedSessions, GetEventsProcessed}
import com.example.EventReader.StartParsing
import akka.pattern.ask
import akka.util.Timeout

import scala.concurrent.duration._
import scala.concurrent.Future
import scala.util.{Failure, Success, Try}

object ApplicationMain extends App {
  val system = ActorSystem("MyActorSystem")
  val path = "/Users/davidfindlay/repos/akkaWorkshop/src/main/resources/events-200k.txt"

  implicit val timeout = Timeout(5 seconds)
  implicit val executionContext = system.dispatcher


  val statsActor = system.actorOf(Props(new StatsActor()), "StatsActor")
  val proxyActor = system.actorOf(Props(new EventProxy(statsActor)), "ProxyActor")
  val eventReaderActor = system.actorOf(Props(new EventReader(path, proxyActor)), "EventReaderActor")

  eventReaderActor ! StartParsing

  println("1: open session actors")
  println("2: closed session actors")
  println("3: processed events")

  while (true) {

    // Read a line from the console window.
    val line = scala.io.StdIn.readLine()
    val input = line.trim()
    val command:Try[Int] = Try(input.toInt)

    command match{
      case Success(validInput) =>
        validInput match {
          case 1 =>
            val numActive:Future[Int] = (proxyActor ? GetActiveSessions).mapTo[Int]
            numActive.onComplete{
              case Success(n) => println(n)
              case Failure(f) => println("something went wrong")
            }
          case 2 =>
            val numCompleted:Future[Int] = (proxyActor ? GetCompletedSessions).mapTo[Int]
            numCompleted.onComplete{
              case Success(n) => println(n)
              case Failure(f) => println("something went wrong")
            }
          case 3 =>
            val numEvents:Future[Int] = (proxyActor ? GetEventsProcessed).mapTo[Int]
            numEvents.onComplete{
              case Success(n) => println(n)
              case Failure(f) => println("something went wrong")
            }
          case _ =>
            println("please enter valid integer.")
        }
      case Failure(f) => println("failed : please enter valid integer")
    }
  }
  system.awaitTermination()
}