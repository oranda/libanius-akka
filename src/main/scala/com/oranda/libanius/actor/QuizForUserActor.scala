package com.oranda.libanius.actor

import akka.persistence.{PersistentActor, SnapshotOffer}

import com.oranda.libanius.actor.QuizForUserActor._
import com.oranda.libanius.model.action._
import com.oranda.libanius.model.quizgroup.{QuizGroupHeader, WordMapping}
import com.oranda.libanius.model.quizitem.QuizItem
import com.oranda.libanius.model.Quiz
import com.oranda.libanius.model.action.{QuizItemSource, modelComponentsAsQuizItemSources}
import com.oranda.libanius.util.Util

import QuizItemSource._
import modelComponentsAsQuizItemSources._

import scala.concurrent.{Future, Promise}

// There is a one-to-one relationship between a Quiz and a user
class QuizForUserActor(quiz: Quiz) extends PersistentActor {

  import context.system

  private var state = QuizState(quiz)

  override val persistenceId: String = self.path.name

  override def receiveCommand: Receive = {
    case UpdateWithUserResponse(_, isCorrect, quizGroupHeader, quizItem) =>
      handleEvent(QuizUpdatedWithUserResponse(isCorrect, quizGroupHeader, quizItem))
    case ScoreSoFar(_) =>
      sender() ! Util.stopwatch(state.quiz.scoreSoFar, "scoreSoFar")
    case ProduceQuizItem(_) =>
      sender() ! Util.stopwatch(produceQuizItem(state.quiz, NoParams()), "find quiz items")
    case IsResponseCorrect(_, quizGroupHeader, prompt, userResponse) =>
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
        state = state.updateWithUserResponse(event)
      }
    case SnapshotOffer(_, snapshot: QuizState) =>
      if (conf.enablePersistence) {
        state = snapshot
      }
  }

  override def unhandled(message: Any): Unit = {
    println(s"QuizForUserActor unhandled: $message")
  }
}

object QuizForUserActor {

  sealed trait QuizCommand {
    val userId: UserId
  }

  final case class UpdateWithUserResponse(
    userId: UserId,
    isCorrect: Boolean,
    quizGroupHeader: QuizGroupHeader,
    quizItem: QuizItem
  ) extends QuizCommand

  final case class ScoreSoFar(userId: UserId) extends QuizCommand

  final case class ProduceQuizItem(userId: UserId) extends QuizCommand

  final case class IsResponseCorrect(
    userId: UserId,
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
