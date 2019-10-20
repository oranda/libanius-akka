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

package com.oranda.libanius.model.quizitem

import com.oranda.libanius.model.quizitem.TextValueOps.TextValue

import scala.language.implicitConversions
import scala.util.Random
import com.oranda.libanius.model.Quiz
import com.oranda.libanius.model.quizgroup.{QuizGroupHeader, QuizGroupKey}

/**
 * Quiz item data holder:
 * contains whatever information is necessary for the view, and for updating the backing data.
 */
case class QuizItemViewWithChoices(
  quizItem: QuizItem,
  qgCurrentPromptNumber: Int,
  quizGroupHeader: QuizGroupHeader,
  falseAnswers: List[String],
  allChoices: List[String],
  promptResponseMap: Seq[(String, String)], // ListMap did not work with upickle
  numCorrectResponsesInARow: Int,
  numCorrectResponsesRequired: Int,
  useMultipleChoice: Boolean
) {
  lazy val prompt: TextValue = quizItem.prompt
  lazy val correctResponse: TextValue = quizItem.correctResponse
  lazy val userResponses = quizItem.userResponses
  lazy val promptType = quizGroupHeader.promptType
  lazy val responseType = quizGroupHeader.responseType
  lazy val quizGroupKey = quizGroupHeader.quizGroupKey

  def isComplete = numCorrectResponsesInARow >= numCorrectResponsesRequired
}

object QuizItemViewWithChoices {
  // Allow QuizItemViewWithChoices to stand in for QuizItem whenever necessary
  implicit def qiView2qi(quizItemView: QuizItemViewWithChoices): QuizItem =
    quizItemView.quizItem

  def choicesInRandomOrder(quizItem: QuizItem, otherChoices: List[String]): List[String] = {
    val allChoices = quizItem.correctResponse.value :: otherChoices
    Random.shuffle(allChoices)
  }

  def makePromptResponseMap(
    quiz: Quiz,
    choices: Seq[String],
    quizGroupKey: QuizGroupKey
  ): Seq[(String, String)] =
    choices.map(promptToResponses(quiz, _, quizGroupKey))

  private[this] def promptToResponses(
    quiz: Quiz,
    choice: String,
    quizGroupKey: QuizGroupKey
  ): (String, String) = {
    val values = quiz.findPromptsFor(choice, quizGroupKey) match {
      case Nil => quiz.findResponsesFor(choice, quizGroupKey.reverse)
      case v => v
    }
    (choice, values.slice(0, 3).mkString(", "))
  }
}
