package com.example

import akka.actor.{ActorSystem, Props}
import akka.testkit.{ImplicitSender, TestActorRef, TestKit, TestProbe}
import com.example.EventReader.Request
import org.scalatest.{BeforeAndAfterAll, Matchers, WordSpecLike}

import scala.concurrent.duration._

class EventProxyActorSpec(_system: ActorSystem) extends TestKit(_system) with ImplicitSender
  with WordSpecLike with Matchers with BeforeAndAfterAll {

  val timeout = 5 seconds

  val testRequest1 = Request(795253081L,1480534410848L,"/","google","firefox")
  val testRequest2 = Request(795253082L,1480534410848L,"/","google","firefox")

  def this() = this(ActorSystem("MySpec"))

  override def afterAll {
    TestKit.shutdownActorSystem(system)
  }

  "An EventProxy actor" must {
    "must create a Map with 2 elements" in {
      val statsActor = TestProbe()
      val eventProxyActor = TestActorRef[EventProxy](Props(new EventProxy(statsActor.ref)))
      eventProxyActor ! testRequest1
      eventProxyActor ! testRequest2
      eventProxyActor.underlyingActor.context.children.size shouldBe(2)
    }
  }
}