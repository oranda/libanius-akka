package com.oranda.libanius.actor

import akka.actor.{ActorSystem, Props}
import akka.pattern.ask
import com.oranda.libanius.actor.QuizForUserActor._
import com.oranda.libanius.model.Quiz
import com.oranda.libanius.model.quizgroup.QuizGroupHeader
import com.oranda.libanius.model.quizitem.{QuizItem, QuizItemViewWithChoices}

import scala.concurrent.Future

class QuizGateway(system: ActorSystem, quiz: Quiz) extends QuizActorSystem(system, quiz) {

  implicit val executionContext = system.dispatcher

  private val quizActor = system.actorOf(
    Props(classOf[QuizForUserActor], quiz),
    "quizActor"
  )

  def updateWithUserResponse(
    isCorrect: Boolean,
    quizGroupHeader: QuizGroupHeader,
    quizItem: QuizItem
  ) = {
    quizActor ! UpdateWithUserResponse(isCorrect, quizGroupHeader, quizItem)
    Future.successful(true)
  }

  def isResponseCorrect(quizGroupHeader: QuizGroupHeader, prompt: String, userResponse: String):
    Future[Boolean] =
    (quizActor ? IsResponseCorrect(quizGroupHeader, prompt, userResponse)).mapTo[Boolean]

  def scoreSoFar: Future[BigDecimal] =
    (quizActor ? ScoreSoFar).mapTo[BigDecimal]

  def produceQuizItem: Future[Option[QuizItemViewWithChoices]] =
    (quizActor ? ProduceQuizItem).mapTo[Option[QuizItemViewWithChoices]]

  def terminate() = {
    system.terminate()
  }
}
