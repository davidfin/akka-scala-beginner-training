package com.example

import akka.actor.{ActorSystem, Props}
import akka.testkit.{ImplicitSender, TestActorRef, TestKit, TestProbe}
import com.example.EventReader.{Request, EOS}
import com.example.SessionActor.ActiveSession
import org.scalatest.{BeforeAndAfterAll, Matchers, WordSpecLike}

import scala.collection.immutable.Seq
import scala.concurrent.duration._

class SessionActorSpec(_system: ActorSystem) extends TestKit(_system) with ImplicitSender
  with WordSpecLike with Matchers with BeforeAndAfterAll {

  val timeout = 5 seconds

  val requestSeq: Seq[Request] =
    (  for {time <- 1 to 10} yield {
      Request(795253082L, time,"/","google","firefox")
    })


  def this() = this(ActorSystem("MySpec"))

  override def afterAll {
    TestKit.shutdownActorSystem(system)
  }

  "An EventProxy actor" must {
    "must create a Map with 2 elements" in {
      val statsActor = TestProbe()
      val sessionActor = TestActorRef[SessionActor](Props(new SessionActor(statsActor.ref)))
      requestSeq.foreach(m=> sessionActor ! m)
      sessionActor ! EOS
      statsActor expectMsg(ActiveSession(requestSeq))
    }
  }
}
