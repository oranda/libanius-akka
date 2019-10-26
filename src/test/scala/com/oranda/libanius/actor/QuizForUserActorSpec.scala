package com.oranda.libanius.actor

import akka.actor.{ActorSystem, Props}
import akka.testkit.{ImplicitSender, TestKit}
import java.util.UUID

import org.scalatest.{BeforeAndAfterAll, Matchers, WordSpecLike}
import com.oranda.libanius.actor.QuizForUserActor._
import com.oranda.libanius.dependencies.AppDependencyAccess
import com.oranda.libanius.model.{Correct, Incorrect, ItemNotFound, Quiz}
import com.oranda.libanius.model.quizgroup.{QuizGroupKey, QuizGroupType}
import com.oranda.libanius.model.quizitem.{QuizItem, QuizItemViewWithChoices}

class QuizForUserActorSpec extends TestKit(ActorSystem("libanius-test"))
  with ImplicitSender
  with WordSpecLike
  with Matchers
  with BeforeAndAfterAll
  with AppDependencyAccess {

  private val quizGroupKey = QuizGroupKey("English word", "German word", QuizGroupType.WordMapping)
  private val userId = UserId(UUID.randomUUID())

  private def newDemoQuizActor = system.actorOf(Props(new QuizForUserActor(Quiz.demoQuiz())))

  // avoid all those dead letter error messages at the end
  override def afterAll: Unit = {
    TestKit.shutdownActorSystem(system)
  }

  "A QuizForUser actor" must {
    "return the score so far" in {
      newDemoQuizActor ! ScoreSoFar(userId)
      val expectedScore = BigDecimal(0)
      expectMsg(expectedScore)
      true
    }

    "produce a quiz item" in {
      newDemoQuizActor ! ProduceQuizItem(userId)
      val quizItemView: Option[QuizItemViewWithChoices] =
        expectMsgType[Option[QuizItemViewWithChoices]]
      val quizItem = quizItemView.map(_.quizItem)
      val quizItemExpected = Some(QuizItem("en route", "unterwegs"))
      assert(quizItem == quizItemExpected)
    }

    "update the quiz on a user response" in {
      val quizActor = newDemoQuizActor
      quizActor ! UpdateWithUserResponse(userId, quizGroupKey, "en route", "unterwegs", true)
      expectMsgType[QuizUpdatedWithUserResponse]

      // get the score and make sure it is not zero, showing that the quiz was updated
      quizActor ! ScoreSoFar(userId)
      val score: BigDecimal = expectMsgType[BigDecimal]
      assert(score > 0)
    }

    "confirm a response is correct" in {
      newDemoQuizActor ! IsResponseCorrect(userId, quizGroupKey, "en route", "unterwegs")
      expectMsg(Correct)
    }

    "confirm a response is incorrect" in {
      newDemoQuizActor ! IsResponseCorrect(userId, quizGroupKey, "en route", "unterschrift")
      expectMsg(Incorrect)
    }

    "return NotFound for an isCorrect call on a nonexistent item" in {
      newDemoQuizActor ! IsResponseCorrect(userId, quizGroupKey, "non-existent", "unterschrift")
      expectMsg(ItemNotFound)
    }

    "remove a quiz item" in {
      val quizActor = newDemoQuizActor
      quizActor ! RemoveQuizItem(userId, quizGroupKey, "en route", "unterwegs")
      expectMsg(QuizItemRemoved(quizGroupKey, "en route", "unterwegs"))
      quizActor ! IsResponseCorrect(userId, quizGroupKey, "en route", "unterwegs")
      expectMsg(ItemNotFound)
    }

    "activate a quiz group" in {
      val demoQuiz = Quiz.demoQuiz()
      demoQuiz.findQuizGroupHeader(quizGroupKey) match {
        case Some(quizGroupHeader) =>
          // set up the actor with a quiz whose only group is inactive
          val demoQuizInactive = demoQuiz.deactivate(quizGroupHeader)
          val quizActor = system.actorOf(Props(new QuizForUserActor(demoQuizInactive)))

          val testCorrectMessage = IsResponseCorrect(userId, quizGroupKey, "en route", "unterwegs")

          quizActor ! testCorrectMessage
          expectMsg(ItemNotFound) // fails because the group is inactive

          quizActor ! ActivateQuizGroup(userId, quizGroupKey, true)
          expectMsg(QuizGroupActivated(quizGroupKey, true))

          quizActor ! testCorrectMessage
          expectMsg(Correct) // succeeds because the group is active
        case None => throw new RuntimeException(s"$quizGroupKey not found in demo quiz")
      }
    }
  }
}
