package com.example

import akka.actor.{Actor, ActorLogging, ActorRef}
import com.example.EventReader.{Request, EOS, Tick}
import com.example.SessionActor._
import scala.collection.immutable.Seq
import scala.collection.mutable.ListBuffer

case class Visit(url: String, duration: Long)

object SessionActor {
  case class ActiveSession(session: Seq[Request]){
    lazy val visits: Seq[Visit]={
      val requestPairs = session.zip(session.tail)
      val visitSeq = requestPairs.map {
        case (req1, req2) =>
          val visitTime = req2.timeStamp - req1.timeStamp
          Visit(req1.url, visitTime)
      }
      // merge consecutive url hits.
      visitSeq.groupBy(_.url).map {
        case (url,durationSeq) =>
          Visit(url, durationSeq.map(_.duration).sum)}.to[Seq]
    }
    // find long sessions to trigger the chatactor.
    def longHelpSession(time: Long): Boolean = {
      // check if duration is > time for all elements in seq
      visits.filter(_.url == "/help").exists(_.duration > time)
    }
  }
}

class SessionActor (statsRef: ActorRef)
  extends Actor
  with ActorLogging {

  /* ListBuffer append is O(C)
    * Seq append is O(n)
    * List prepend is O(1) and reverse is O(n)
    */

  val sessionEnd = 50
  val restartTick = 0
  val longSessionTime = 120000

  override def receive = myReceive(restartTick, ListBuffer.empty[Request])

  def myReceive(tickCounter: Int, userSession: ListBuffer[Request]): Receive = {
    case req: Request =>
      context.become(myReceive(restartTick, userSession += req ))

    case Tick =>
      tickCounter match {
        case x if x >= sessionEnd =>
          statsRef ! ActiveSession(userSession.to[Seq])
          context.become(myReceive(restartTick, userSession))
          context.stop(self)
        case _ =>
          context.become(myReceive(tickCounter+1, userSession))
      }
    case EOS =>
      statsRef ! ActiveSession(userSession.to[Seq])
      context.stop(self)
  }
}
