package com.example

import akka.actor.{ActorSystem, Props}
import akka.testkit.{ImplicitSender, TestKit, TestProbe}
import org.scalatest.{BeforeAndAfterAll, Matchers, WordSpecLike}
import scala.concurrent.duration._
import com.example.EventReader._

class EventReaderActorSpec(_system: ActorSystem) extends TestKit(_system) with ImplicitSender
  with WordSpecLike with Matchers with BeforeAndAfterAll {

  val timeout = 5 seconds

  def this() = this(ActorSystem("MySpec"))

  override def afterAll {
    TestKit.shutdownActorSystem(system)
  }

  val path = "/Users/davidfindlay/repos/akkaWorkshop/src/main/resources/events-200k.txt"

  "An EventProxy actor" must {
    "get Request message" in {
      val eventProxy = TestProbe()
      val eventReaderActor = system.actorOf(Props(new EventReader(path, eventProxy.ref)))
      eventReaderActor ! StartParsing
      eventProxy.expectMsgType[Request]
    }
  }

  "An EventProxy actor" must {
    "get all Request message" in {
      val eventProxy = TestProbe()
      val eventReaderActor = system.actorOf(Props(new EventReader(path, eventProxy.ref)))
      eventReaderActor ! StartParsing

      val numLogs = 232124

      val m = eventProxy.receiveWhile[AnyRef](3 minute, 500 millis) {
        case r: Request => r
        case Tick => Tick
      }
      m.collect{case r: Request => r}.size should === (numLogs)

    }
  }
}