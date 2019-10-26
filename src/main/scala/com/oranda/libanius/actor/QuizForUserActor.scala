package com.oranda.libanius.actor

import akka.persistence.{PersistentActor, SnapshotOffer}
import akka.pattern.pipe

import com.oranda.libanius.actor.QuizForUserActor._
import com.oranda.libanius.model.action._
import com.oranda.libanius.model.quizgroup.QuizGroupKey
import com.oranda.libanius.model.quizitem.QuizItem
import com.oranda.libanius.model.Quiz
import com.oranda.libanius.model.action.{QuizItemSource, modelComponentsAsQuizItemSources}
import com.oranda.libanius.util.Util
import QuizItemSource._
import modelComponentsAsQuizItemSources._

import scala.concurrent.{Future, Promise}

// There is a one-to-one relationship between a Quiz and a user
class QuizForUserActor(quiz: Quiz) extends PersistentActor {

  import context._

  private var state = QuizState(quiz)

  override val persistenceId: String = self.path.name

  override def receiveCommand: Receive = {
    case ScoreSoFar(_) =>
      sender() !  Util.stopwatch(state.quiz.scoreSoFar, "scoreSoFar")
    case ProduceQuizItem(_) =>
      sender() ! Util.stopwatch(produceQuizItem(state.quiz, NoParams()), "find quiz items")
    case IsResponseCorrect(_, quizGroupKey, prompt, userResponse) =>
      sender() ! state.quiz.isCorrect(quizGroupKey, prompt, userResponse)
    case UpdateWithUserResponse(_, quizGroupKey, prompt, correctResponse, isCorrect) =>
      handleStateChangingEvent(
        QuizUpdatedWithUserResponse(quizGroupKey, prompt, correctResponse, isCorrect)
      ) pipeTo sender()
    case ActivateQuizGroup(_, quizGroupKey, singleGroupActiveMode) =>
      handleStateChangingEvent(
        QuizGroupActivated(quizGroupKey, singleGroupActiveMode)
      ) pipeTo sender()
    case RemoveQuizItem(_, quizGroupKey, prompt, correctResponse) =>
      handleStateChangingEvent(
        QuizItemRemoved(quizGroupKey, prompt, correctResponse)
      ) pipeTo sender()
  }

  private def handleStateChangingEvent[E <: QuizEvent](e: => E): Future[E] = {
    if (conf.enablePersistence) {
      val p = Promise[E]
      persist(e) { event =>
        p.success(event)
        runStateChangingEvent(event)
        system.eventStream.publish(event)
        if (lastSequenceNr != 0 && lastSequenceNr % conf.numEventsBetweenSnapshots == 0)
          saveSnapshot(state)
      }
      p.future
    } else {
      runStateChangingEvent(e)
      Future.successful(e)
    }
  }

  private def runStateChangingEvent(e: QuizEvent) = {
    e match {
      case e: QuizUpdatedWithUserResponse => state = state.updateWithUserResponse(e)
      case e: QuizGroupActivated => state = state.activateQuizGroup(e)
      case e: QuizItemRemoved => state = state.removeQuizItem(e)
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
    l.logError(s"QuizForUserActor unhandled: $message")
  }
}

object QuizForUserActor {

  sealed trait QuizCommand {
    val userId: UserId
  }

  final case class ScoreSoFar(userId: UserId) extends QuizCommand

  final case class ProduceQuizItem(userId: UserId) extends QuizCommand

  final case class UpdateWithUserResponse(
    userId: UserId,
    quizGroupKey: QuizGroupKey,
    prompt: String,
    correctResponse: String,
    isCorrect: Boolean
  ) extends QuizCommand

  final case class ActivateQuizGroup(
    userId: UserId,
    quizGroupKey: QuizGroupKey,
    singleGroupActiveMode: Boolean
  ) extends QuizCommand

  final case class RemoveQuizItem(
    userId: UserId,
    quizGroupKey: QuizGroupKey,
    prompt: String,
    correctResponse: String
  ) extends QuizCommand

  final case class IsResponseCorrect(
    userId: UserId,
    quizGroupKey: QuizGroupKey,
    prompt: String,
    userResponse: String
  ) extends QuizCommand

  sealed trait QuizEvent

  final case class QuizUpdatedWithUserResponse(
    quizGroupKey: QuizGroupKey,
    prompt: String,
    correctResponse: String,
    isCorrect: Boolean
  ) extends QuizEvent

  final case class QuizGroupActivated(
    quizGroupKey: QuizGroupKey,
    singleGroupActiveMode: Boolean
  ) extends QuizEvent

  final case class QuizItemRemoved(
    quizGroupKey: QuizGroupKey,
    prompt: String,
    correctResponse: String
  ) extends QuizEvent

  final case class QuizState(quiz: Quiz) {
    def updateWithUserResponse(event: QuizUpdatedWithUserResponse): QuizState = {
      val updatedQuiz = for {
        qgh <- quiz.findQuizGroupHeader(event.quizGroupKey)
        quizItem <- quiz.findQuizItem(qgh, event.prompt, event.correctResponse)
      } yield Util.stopwatch(
        quiz.updateWithUserResponse(event.isCorrect, qgh, quizItem),
        "updateWithUserResponse"
      )
      QuizState(updatedQuiz.getOrElse(quiz))
    }

    def activateQuizGroup(event: QuizGroupActivated): QuizState = {
      val (quizUpdated1, qgh) = quiz.loadQuizGroup(event.quizGroupKey)
      qgh match {
        case Some(qgh) =>
          val quizUpdated2 =
            if (event.singleGroupActiveMode) quizUpdated1.deactivateAll else quizUpdated1
          l.log(s"QuizForUserActor: activating quizgroup $qgh")
          QuizState(quizUpdated2.activate(qgh))
        case None =>
          l.logError(s"Could not activate quiz group for ${event.quizGroupKey}")
          QuizState(quiz)
      }
    }

    def removeQuizItem(event: QuizItemRemoved): QuizState = {
      val quizItem = QuizItem(event.prompt, event.correctResponse)
      val updatedQuiz = quiz.findQuizGroupHeader(event.quizGroupKey) match {
        case Some(qgh) =>
          val (quizAfter, wasRemoved) = quiz.removeQuizItem(quizItem, qgh)
          if (wasRemoved)
            l.log(s"Removed $quizItem")
          else
            l.logError(s"Could not remove quiz item $quizItem")
          quizAfter
        case _ =>
          l.logError(s"Quiz header not found for ${event.quizGroupKey}")
          quiz
      }
      QuizState(updatedQuiz)
    }
  }
}
