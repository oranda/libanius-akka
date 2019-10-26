/*
 * Libanius
 * Copyright (C) 2012-2019 James McCabe <jjtmccabe@gmail.com>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.oranda.libanius.consoleui

import akka.actor.{ActorSystem, Props}
import java.util.UUID

import scala.util.{Failure, Success, Try}
import com.oranda.libanius.util.StringUtil
import Output._
import ConsoleUtil._
import com.oranda.libanius.dependencies._
import com.oranda.libanius.model.quizitem.QuizItemViewWithChoices
import com.oranda.libanius.model.quizgroup.{QuizGroup, QuizGroupHeader}
import com.oranda.libanius.actor.{QuizForUserActor, QuizGateway, UserId}
import com.oranda.libanius.model.{Correct, Quiz}
import com.oranda.libanius.model.quizgroup.QuizGroupType.WordMapping

import scala.concurrent.{Await, Future}
import scala.concurrent.duration._

class InteractiveQuiz(quizGateway: QuizGateway) extends AppDependencyAccess {

  implicit val executionContext = quizGateway.system.dispatcher

  def testUserWithQuizItem(): Unit = {
    showScore()
    quizGateway.produceQuizItem onComplete {
      case Success(optQuizItem) =>
        optQuizItem match {
          case Some(quizItem) => keepShowingQuizItems(quizItem)
          case _ => output("No more questions found! Done!")
        }
      case Failure(t) => output("Error: actor did not return a quiz item: " + t.getMessage())
    }
  }

  def keepShowingQuizItems(quizItem: QuizItemViewWithChoices): Unit = {
    val response = showQuizItemAndProcessResponse(quizItem)
    response match {
      case Invalid =>
        output("Invalid input\n")
        keepShowingQuizItems(quizItem)
      case Quit =>
        output("Exiting... .")
        quizGateway.terminate()
      case _ =>
        testUserWithQuizItem()
    }
  }

  def showScore(): Unit = {
    quizGateway.scoreSoFar onComplete {
      case Success(score) =>
        val formattedScore = StringUtil.formatScore(score)
        output(s"Score: $formattedScore")
      case Failure(t) => output("Error: actor did not return the score " + t.getMessage())
    }
  }

  def showQuizItemAndProcessResponse(quizItem: QuizItemViewWithChoices): UserConsoleResponse = {
    val wordText = s": what is the ${quizItem.responseType} for this ${quizItem.promptType}?"
    val wordTextToShow =
      if (quizItem.quizGroupHeader.quizGroupType == WordMapping) wordText else ""
    val answeredText = s" (correctly answered ${quizItem.numCorrectResponsesInARow} times)"
    val answeredTextToShow = if (quizItem.numCorrectResponsesInARow > 0) answeredText else ""
    val questionText = quizItem.qgCurrentPromptNumber + ": " + quizItem.prompt
    val fullQuestionText = questionText + wordTextToShow + answeredTextToShow
    output(s"$fullQuestionText\n")

    if (quizItem.useMultipleChoice) showChoicesAndProcessResponse(quizItem)
    else getTextResponseAndProcess(quizItem)
  }

  def showChoicesAndProcessResponse(quizItem: QuizItemViewWithChoices): UserConsoleResponse = {
    val choices = ChoiceGroupStrings(quizItem.allChoices)
    choices.show()
    val userResponse = choices.getSelectionFromInput match {
      case Right(chosenOptions) => chosenOptions
      case Left(noProcessResponse) => noProcessResponse
    }
    processAnswer(userResponse, quizItem)
  }

  def getTextResponseAndProcess(quizItem: QuizItemViewWithChoices): UserConsoleResponse = {
    output("(Not multiple choice. Type it in.)")
    Try(getAnswerFromInput).recover {
      case e: Exception => Invalid
    }.map(userResponse => processAnswer(userResponse, quizItem)).get
  }

  def processAnswer(userResponse: UserConsoleResponse,
      quizItem: QuizItemViewWithChoices): UserConsoleResponse = {
    userResponse match {
      case answer: Answer => processUserAnswer(answer.text, quizItem)
      case _ =>
    }
    userResponse
  }

  def processUserAnswer(
    userResponse: String,
    quizItem: QuizItemViewWithChoices
  ) = {
    val (quizGroupKey, prompt) = (quizItem.quizGroupKey, quizItem.prompt.value)
    val responseCorrectness = Await.result(
      quizGateway.isResponseCorrect(quizGroupKey, prompt, userResponse),
      10.seconds
    )
    val isCorrect = responseCorrectness == Correct
    if (isCorrect)
      output("\nCorrect!\n")
    else
      output(s"\nWrong! It's ${quizItem.correctResponse}\n")
    Await.result(
      quizGateway.updateWithUserResponse(quizGroupKey, isCorrect, quizItem.quizItem),
      10.seconds
    )
  }

  def getAnswerFromInput: UserConsoleResponse =
    readLineUntilNoBackspaces match {
      case "q" | "quit" => Quit
      case input: String => TextAnswer(input)
    }
}

object InteractiveQuiz extends AppDependencyAccess {
  def runQuiz(quiz: Quiz): Unit = {
    val system = ActorSystem("libanius")
    val quizActor = system.actorOf(
      Props(classOf[QuizForUserActor], quiz),
      "quizActor"
    )
    val quizGateway = new QuizGateway(quizActor, system)
    val interactiveQuiz = new InteractiveQuiz(quizGateway)
    output("OK, the quiz begins! To quit, type q at any time.\n")
    interactiveQuiz.testUserWithQuizItem()
  }

  def userQuizGroupSelection(
    quizGroupHeaders: List[QuizGroupHeader]
  ): Map[QuizGroupHeader, QuizGroup] = {
    output("Choose quiz group(s). For more than one, separate with commas, e.g. 1,2,3")
    val choices = ChoiceGroupQgHeaders(quizGroupHeaders)
    choices.show()

    val selectedQuizGroupHeaders: List[QuizGroupHeader] = choices.getSelectionFromInput match {
      case Right(ChosenOptions(selectedChoices)) => selectedChoices
      case _ => Nil
    }

    if (selectedQuizGroupHeaders.isEmpty) {
      output("Unrecognized option")
      userQuizGroupSelection(quizGroupHeaders)
    }
    else
      selectedQuizGroupHeaders.map(header => (header, dataStore.loadQuizGroupCore(header))).toMap
  }
}


