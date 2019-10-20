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

package com.oranda.libanius.script

import java.io.FileWriter

import com.oranda.libanius.dependencies.DataStoreDefault
import com.oranda.libanius.model.Quiz
import com.oranda.libanius.model.quizgroup._
import com.oranda.libanius.model.quizitem.QuizItem
import com.oranda.libanius.simulation.FullQuiz._

import scala.collection.immutable.{ListMap, Stream}
import scala.language.reflectiveCalls

object GenerateTestData extends App {
  def randomString(length: Int) = scala.util.Random.alphanumeric.take(length).mkString

  def genQiStream: Stream[QuizItem] = {
    val correctResponses = List(5, 8, 9, 10, 45)
    val incorrectResponses = List(3, 4, 44, 67)
    Stream.continually(QuizItem(randomString(10), randomString(10), correctResponses, incorrectResponses)) take 1000
  }

  def saveQuiz(quiz: Quiz, path: String = "", userToken: String = ""): Unit = {

    def saveToFile(header: QuizGroupHeader, quizGroup: QuizGroup, userToken: String) = {
      val fileName = header.makeQgFileName
      val qgwh = QuizGroupWithHeader(header, quizGroup)
      val serialized = qgwh.toCustomFormat

      l.log(s"Saving quiz group $header.promptType , quiz group has promptNumber " +
        s"quizGroup.currentPromptNumber to $fileName")
      writeToFile(path + userToken + "-" + fileName, serialized)
    }
    quiz.activeQuizGroups.foreach { case (header, qg) => saveToFile(header, qg, userToken) }
  }

  def writeToFile(fileName: String, data: String) =
    using (new FileWriter(fileName)) (_.write(data))

  private def using[A <: {def close(): Unit}, B](param: A)(f: A => B): B =
    try { f(param) } finally { param.close() }

  val qiStream0: Stream[QuizItem] = genQiStream
  val qiStream1: Stream[QuizItem] = genQiStream
  val qiStream2: Stream[QuizItem] = genQiStream
  val qiStream3: Stream[QuizItem] = genQiStream
  val qgml0 = QuizGroupMemoryLevel(0, 0, qiStream0)
  val qgml1 = QuizGroupMemoryLevel(1, 5, qiStream1)
  val qgml2 = QuizGroupMemoryLevel(2, 15, qiStream2)
  val qgml3 = QuizGroupMemoryLevel(3, 15, qiStream3)
  val memLevelMap: Map[Int, QuizGroupMemoryLevel] = Map(0 -> qgml0, 1 -> qgml1, 2 -> qgml2, 3 -> qgml3)
  val userData: QuizGroupUserData = QuizGroupUserData(isActive = true)
  val qg = QuizGroup(memLevelMap, userData)
  val qghEngGer = QuizGroupHeader("English word", "German word", WordMapping, "|", 4)
  val quizGroups = ListMap(qghEngGer -> qg)
  val quiz = Quiz(quizGroups)
  //println(quiz)

  val dataStore = new DataStoreDefault()
  saveQuiz(quiz, "data/test/testData.txt")
}
