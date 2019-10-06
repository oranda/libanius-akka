package com.oranda.libanius.actor

import akka.persistence.{PersistentActor, SnapshotOffer}

import com.oranda.libanius.actor.QuizForUserActor._
import com.oranda.libanius.model.action._
import com.oranda.libanius.model.quizgroup.QuizGroupHeader
import com.oranda.libanius.model.quizitem.QuizItem
import com.oranda.libanius.model.Quiz
import com.oranda.libanius.model.action.{QuizItemSource, modelComponentsAsQuizItemSources}
import com.oranda.libanius.util.Util

import QuizItemSource._
import modelComponentsAsQuizItemSources._

import scala.concurrent.{Future, Promise}


class QuizForUserActor(quiz: Quiz) extends PersistentActor {

  import context._

  private var state = QuizState(quiz)

  override def persistenceId: String = "quiz"

  override def receiveCommand: Receive = {
    case UpdateWithUserResponse(isCorrect, quizGroupHeader, quizItem) =>
      handleEvent(QuizUpdatedWithUserResponse(isCorrect, quizGroupHeader, quizItem))
    case ScoreSoFar =>
      sender() ! Util.stopwatch(state.quiz.scoreSoFar, "scoreSoFar")
    case ProduceQuizItem =>
      sender() ! Util.stopwatch(produceQuizItem(state.quiz, NoParams()), "find quiz items")
    case IsResponseCorrect(quizGroupHeader, prompt, userResponse) =>
      sender() ! state.quiz.isCorrect(quizGroupHeader, prompt, userResponse)
  }

  private def handleEvent[E <: QuizUpdatedWithUserResponse](e: => E): Future[E] = {
    if (conf.enablePersistence) {
      val p = Promise[E]
      persist(e) { event =>
        p.success(event)
        state = state.updateWithUserResponse(event)
        system.eventStream.publish(event)
        if (lastSequenceNr != 0 && lastSequenceNr % conf.numEventsBetweenSnapshots == 0)
          saveSnapshot(state)
      }
      p.future
    } else {
      state = state.updateWithUserResponse(e)
      Future.successful(e)
    }
  }

  override def receiveRecover: Receive = {
    case event: QuizUpdatedWithUserResponse =>
      if (conf.enablePersistence) {
        println("replaying event")
        state = state.updateWithUserResponse(event)
      }
    case SnapshotOffer(_, snapshot: QuizState) =>
      if (conf.enablePersistence) {
        println("accepting snapshot")
        state = snapshot
      }
  }
}

object QuizForUserActor {

  sealed trait QuizCommand

  final case class UpdateWithUserResponse(
    isCorrect: Boolean,
    quizGroupHeader: QuizGroupHeader,
    quizItem: QuizItem
  ) extends QuizCommand

  final case object ScoreSoFar extends QuizCommand

  final case object ProduceQuizItem extends QuizCommand

  final case class IsResponseCorrect(
    quizGroupHeader: QuizGroupHeader,
    prompt: String,
    userResponse: String
  ) extends QuizCommand

  sealed trait QuizEvent

  final case class QuizUpdatedWithUserResponse(
    isCorrect: Boolean,
    quizGroupHeader: QuizGroupHeader,
    quizItem: QuizItem
  ) extends QuizEvent

  final case class QuizState(quiz: Quiz) {
    def updateWithUserResponse(event: QuizUpdatedWithUserResponse): QuizState = {
      val updatedQuiz = Util.stopwatch(quiz.updateWithUserResponse(
        event.isCorrect,
        event.quizGroupHeader,
        event.quizItem), "updateQuiz")
      QuizState(updatedQuiz)
    }
  }
}
