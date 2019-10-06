package com.oranda.libanius.actor

import akka.actor.ActorSystem
import akka.util.Timeout
import com.oranda.libanius.model.Quiz

import scala.concurrent.duration._

class QuizActorSystem(val system: ActorSystem, val quiz: Quiz) {

  // The default timeout for ?.
  implicit val askTimeout = Timeout(30.seconds)
}
