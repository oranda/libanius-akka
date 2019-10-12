package com.oranda.libanius.actor

import akka.actor.{ActorRef, ActorSystem}
import akka.pattern.ask
import akka.util.Timeout

import com.oranda.libanius.actor.QuizForUserActor._
import com.oranda.libanius.model.quizgroup.QuizGroupHeader
import com.oranda.libanius.model.quizitem.{QuizItem, QuizItemViewWithChoices}

import scala.concurrent.Future
import scala.concurrent.duration._

class QuizGateway(quizActor: ActorRef, val system: ActorSystem) {

  implicit val ec = system.dispatcher

  // The default timeout for ?.
  implicit val askTimeout = Timeout(30.seconds)

  def updateWithUserResponse(
    userId: UserId,
    isCorrect: Boolean,
    quizGroupHeader: QuizGroupHeader,
    quizItem: QuizItem
  ) = {
    quizActor ! UpdateWithUserResponse(userId, isCorrect, quizGroupHeader, quizItem)
    Future.successful(true)
  }

  def isResponseCorrect(
    userId: UserId,
    quizGroupHeader: QuizGroupHeader,
    prompt: String,
    userResponse: String
  ): Future[Boolean] =
    (quizActor ? IsResponseCorrect(userId, quizGroupHeader, prompt, userResponse)).mapTo[Boolean]

  def scoreSoFar: Future[BigDecimal] =
    (quizActor ? ScoreSoFar).mapTo[BigDecimal]

  def produceQuizItem: Future[Option[QuizItemViewWithChoices]] =
    (quizActor ? ProduceQuizItem).mapTo[Option[QuizItemViewWithChoices]]

  def terminate() = {
    system.terminate()
  }
}
