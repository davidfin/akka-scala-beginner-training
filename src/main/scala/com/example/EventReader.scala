package com.example

import akka.actor.{Actor, ActorLogging, ActorRef}
import com.example.EventReader._
import scala.io.Source

object EventReader {

  object StartParsing

  object EOS

  object Tick

  case class Request(sessionId: Long,
                     timeStamp: Long,
                     url: String,
                     referrer: String,
                     browser: String)

  val pattern = "Request\\((\\d.+),(\\d.+),(.+),(.+),(.+)\\)".r

  def parseMsg(s: String) = s match {
    case p@pattern(sessionId, timeStamp, url, referrer, browser) =>
      Request(sessionId.toLong, timeStamp.toLong, url, referrer, browser)
  }

  val sessionMilli = 100

}
class EventReader(sourceFile: String, proxy: ActorRef)
  extends Actor
    with ActorLogging {

  override def receive: Receive = {
    case StartParsing =>

      Source.fromFile(sourceFile).getLines().foldLeft(0L)((referenceTime, line) => {
        val msg = parseMsg(line)
        proxy ! msg
        val currentTime = msg.timeStamp
        if ((currentTime - referenceTime) > sessionMilli) {
          proxy ! Tick
          currentTime
        }
        else referenceTime
      })

      proxy ! EOS
      context.stop(self)
  }
}


