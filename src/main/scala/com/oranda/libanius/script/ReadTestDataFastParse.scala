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

import com.oranda.libanius.io.DefaultIO
import com.oranda.libanius.util.Util

import fastparse.all._
import fastparse.core.Parsed

import TestParse._

object TestParse {

  val fileName = "data/test/testData100000.text-English word-German word.qgr"

  def getQgString: String = {
    val io = new DefaultIO()
    Util.stopwatch(io.readFile(fileName).get, "reading file " + fileName)
  }
}


object TestParseByLineFast extends App {


/*
  val notNewline = P( CharPred(_.!=(' ')).rep.! )
  //val aa = P( notNewline.! ~ " ")
  val captureRep = P( notNewline.!.rep(sep = " "))
  val as: Parsed[Seq[String]] = captureRep.parse("aa aa aa aab")
  println("number of a's", as.get.value.size)
*/

  // TODO: capture lines and filter by "a"

  //val file: Parser[Seq[String]] = P(line.rep(sep="\\n")) // rep(line) ^^ { 1 }  // a file is a repetition of lines

  //val line: Parser[String] = P( /*"a" ~*/ AnyChar.rep.! ) //^^ { 0 } // reads a line and returns 0

  val line = P( CharPred(_.!=('\n')).rep.! )
  //val line = P( AnyChar.rep.! )
  val file = P( line.!.rep(sep = "\n"))

  //val lines = Source.fromFile(fileName).getLines()
  Util.stopwatch( {
    val Parsed.Success(value, successIndex) = file.parse(getQgString)
    println("number of lines: " + value.size)
    //println("successIndex: " + successIndex)
  }, "fastparsed lines")

}


object ReadTestData extends App {

  //val qgHeader: Parser[String] = P( "#quizGroup" ~ (!"#" ~ AnyChar).rep.!)
  //val qgMemLevel: Parser[String] = P( "#quizGroupPartition" ~ (!"#" ~ AnyChar).rep.!)
  val qgHeader: Parser[String] = P( "#quizGroup" ~ CharsWhile(_ != '#').!)
  val qgMemLevel: Parser[String] = P( "#quizGroupPartition" ~ CharsWhile(_ != '#').!)
  val quizGroup: Parser[(String, Seq[String])] = P( qgHeader ~ qgMemLevel.!.rep )

  Util.stopwatch( {
    val Parsed.Success(value, successIndex) = quizGroup.parse(getQgString)
    println("qgHeader: " + value._1)
    println("number of mem levels: " + value._2.size)
    //println("successIndex: " + successIndex)
  }, "fastparsed lines")

/*
  val io = new DefaultIO()
  val dataStore = new DataStore(io)
  val fileName = "data/test/testData100000.text-English word-German word.qgr"

  val qg = for {
    qgText <- Util.stopwatch(io.readFile(fileName), "reading file " + fileName)
  } yield Util.stopwatch(parseQuizGroup(qgText), "parsing quiz group")

  def parseQuizGroup(qgText: String) = {
    val qgMemLevelSepText = "#quizGroupPartition "
    val quizGroupParts = qgText.split("(?=" + qgMemLevelSepText + ")")
    val qgHeaderLine = quizGroupParts.head  // headOption?
    val quizGroupLevelsText = quizGroupParts.tail

    println("qgHeaderLine: " + qgHeaderLine)
    println("quizGroup num levels " + quizGroupLevelsText.size)
    println("quizGroup first level " + quizGroupLevelsText.head.substring(0, 200))


    def toReader(str: String) = new CharSequenceReader(str)
    val (quizGroupHeader, userData) = parseQuizGroupHeaderAndUserData(toReader(qgHeaderLine))

    println("quizGroupHeader: " + quizGroupHeader)
    println("userData: " + userData)


    def parseQuizGroupMemoryLevel(qgmlText: String)(implicit sep: Separator) = {
     val bs = new BufferedSource(new ByteArrayInputStream(qgmlText.getBytes))
     val memLevelLines: Iterator[String] = bs.getLines
     val qgmlHeaderLine = memLevelLines.next // on Exception?
     val qgmlHeader: (Int, Int) = parseQuizGroupMemoryLevelHeader(toReader(qgHeaderLine))
     val memLevelReader = new PagedSeqReader(PagedSeq.fromLines(memLevelLines))

     // memLevelReader.
     val quizItem/*: Iterator[QuizItem]*/ = parseQuizItem(memLevelReader)
     //val qgmlLines = memLevelLines.tail


    }


    val quizGroupLevels = quizGroupLevelsText.map(parseQuizGroupMemoryLevel(_)(Separator("|")))

    //val reader = new PagedSeqReader(PagedSeq.fromLines(lines))

    //parse(quizGroupHeaderAndUserData, qgHeaderLine)

      //Parser[(QuizGroupHeader, QuizGroupUserData)]


    //QuizGroupHeader(qgText).createQuizGroupFromIterator(
    //  Source.fromFile(fileName).getLines()),
  }

  //println("Read quiz group with " + qg.get.numQuizItems + " items")
  */
}
