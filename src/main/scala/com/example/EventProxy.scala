package com.example

import akka.actor.{Actor, ActorContext, ActorLogging, ActorRef, Props, Terminated}
import com.example.EventReader._
import com.example.EventProxy._
import com.example.StatsActor.StartComputingStats

object EventProxy{
  object GetActiveSessions
  object GetCompletedSessions
  object GetEventsProcessed

  def createSessionActor (statsRef: ActorRef, id:String)(implicit context:ActorContext) : ActorRef = {
    context.actorOf(Props(new SessionActor(statsRef)), id)
  }
}

class EventProxy(statsActorRef:ActorRef)
  extends Actor
    with ActorLogging {

  override def receive: Receive = currentlyParsing(0, 0)

  def commonReceive(numCompletedSessions: Int, numEventsProcessed:Int): Receive = {
    case GetActiveSessions =>
      val source = sender()
      source ! context.children.size

    case GetCompletedSessions =>
      val source = sender()
      source ! numCompletedSessions

    case GetEventsProcessed =>
      val source = sender()
      source ! numEventsProcessed
  }

  def currentlyParsing(numCompletedSessions:Int, numEventsProcessed: Int) : Receive  =
    commonReceive(numCompletedSessions, numEventsProcessed) orElse {

    case req: Request =>

      val sessionRef = context
        .child(req.sessionId.toString())
        .getOrElse(context.watch(createSessionActor(statsActorRef, req.sessionId.toString)))
      sessionRef ! req

      context.become(currentlyParsing(numCompletedSessions, numEventsProcessed+1))

    case Tick =>
      context.children foreach (child => child ! Tick)

    case EOS =>
      context.children foreach (child => child ! EOS)
      context.become(finishedParsing(numCompletedSessions, numEventsProcessed, false))
    case Terminated(_) =>
      context.become(currentlyParsing(numCompletedSessions+1, numEventsProcessed))

  }

  def finishedParsing(numCompletedSessions:Int, numEventsProcessed: Int, sentStats: Boolean) : Receive  =
    commonReceive(numCompletedSessions, numEventsProcessed) orElse {
    case Terminated(ref) =>
      if (context.children.isEmpty && ! sentStats) {
        statsActorRef ! StartComputingStats
        context.become(finishedParsing(numCompletedSessions,numEventsProcessed,true))
      }
  }

}