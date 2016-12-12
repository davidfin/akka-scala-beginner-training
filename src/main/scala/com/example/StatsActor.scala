package com.example


import akka.actor.{Actor, ActorLogging}
import akka.event.Logging
import com.example.EventReader.Request
import com.example.SessionActor._
import com.example.StatsActor._

import scala.collection.immutable.ListMap
import scala.collection.mutable.Seq

object StatsActor{
  object StartComputingStats

  val topLandingNum = 2
  val topBrowserNum = 3
  val topReferrerNum = 3
  val msToMin = 60000

  def topResource[A](data: Seq[ActiveSession], top:Int, f: ActiveSession => A): Map[A, Long] ={
    // Top 2 landing pages (include number of hits). Ex: ("/home" -> 1000, "/help" -> 500)
    // landing page is defined by the first url in each session
    val resourceSeq = data.map(f)
    val countedPages = resourceSeq
      .groupBy(identity)
      .mapValues(_.size.toLong)
    ListMap(countedPages
      .toSeq
      .sortBy({case (_,counts) => counts})
      .reverse:_*)
      .take(top)
  }

  def maxSinkPage[A](data: Seq[ActiveSession], f: ActiveSession => A): Long = {
    // Top sink page (include number of hits). Ex: ("/contact" -> 1500)
    val sinkPageSeq = data.map(f)
    sinkPageSeq
      .groupBy(identity)
      .mapValues(_.size.toLong)
      .valuesIterator
      .max
  }

  // TODO: Type alias for clearer method signature
  def busiestMinute(data: Seq[ActiveSession]): (Long, Long) = {
    // Busiest minutes of the day, including number of requests. Ex: (12345 -> 20)
    val allrequests: Seq[Request] = data.flatMap(as => as.session)
    val RequestPerMinute: Map[Long, Seq[Request]] = allrequests.groupBy(m => m.timeStamp % msToMin)
    RequestPerMinute.mapValues(_.size.toLong).map(_.swap).max
  }

  def countUrlVisits(data: Seq[ActiveSession]) : Map[String, Long] = {
    // View count for all URLs
    data.flatMap(as => as.session).groupBy(_.url).mapValues(_.size.toLong)
  }

  def urlVisitTime(data: Seq[ActiveSession]): Map[String, Long] = {
    // Average visit time for all URLs. Ex: ("/home" -> 2 seconds, "/help" -> 900 millis)

    def average(visits: Seq[Visit]): Long = visits.isEmpty match {
      case true => 0L
      case _ =>
        val times = visits.map(_.duration)
        times.sum / times.length
    }

    val allVisits: Seq[Visit] = data.flatMap(_.visits)
    val visitMap: Map[String, Seq[Visit]] = allVisits.groupBy(_.url)
    visitMap.mapValues(average)
  }

}

class StatsActor extends Actor
  with ActorLogging {

  override def receive: Receive = receive(Seq())

  def receive( data: Seq[ActiveSession]): Receive = {
    case session: ActiveSession =>
      context.become(receive(data :+ session))

    case StartComputingStats =>

      log.info("top landings: " + topResource(data, topLandingNum, _.session.head.url).toString())
      log.info("top sink page: " + maxSinkPage(data, _.session.last.url).toString())
      log.info("top browser: " + topResource(data, topBrowserNum, _.session.head.browser).toString())
      log.info("top referrers: " + topResource(data, topReferrerNum, _.session.head.referrer).toString() )
      log.info("busiest minute: " + busiestMinute(data).toString())
      log.info("url hits " + countUrlVisits(data).toString() )
      log.info("url visit time: " + urlVisitTime(data).toString())

  }


}
