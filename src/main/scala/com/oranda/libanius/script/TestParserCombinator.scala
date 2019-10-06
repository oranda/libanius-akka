/*
 * Libanius
 * Copyright (C) 2012-2019 James McCabe <james@oranda.com>
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

import com.oranda.libanius.dependencies.{DataStore, DataStoreDefault}
import com.oranda.libanius.io.DefaultIO
import com.oranda.libanius.model.quizgroup.QuizGroupHeader
import com.oranda.libanius.util.Util

import scala.collection.immutable.PagedSeq
import scala.io.Source
import scala.language.postfixOps
import fastparse.all._

import scala.util.parsing.combinator.JavaTokenParsers
import scala.util.parsing.input.CharSequenceReader

object TestParserCombinator extends App/* with JavaTokenParsers*/ {


  //def file: Parser[Unit] = P(line) // rep(line) ^^ { 1 }  // a file is a repetition of lines

  //def line: Parser[Unit] = P("") //^^ { 0 } // reads a line and returns 0


  val io = new DefaultIO()
  val dataStore = new DataStoreDefault()
  val fileName = "data/test/testData100000.text-English word-German word.qgr"


  //def toReader(str: String) = new CharSequenceReader(str)

  //val lines = Source.fromFile(fileName).getLines()


  val qg = for {
    qgText <- Util.stopwatch(io.readFile(fileName), "reading file " + fileName)
  } yield Util.stopwatch( {
      val qg = QuizGroupHeader(qgText).createQuizGroup(qgText)
      qg
    }, "parsing quiz group")


  Util.stopwatch(println("Read quiz group with " + qg.get.numQuizItems +
    " items, per level: " + qg.get.levels.map(_.quizItems.size)), "println")

}

  //println("Read quiz group with " + qg.get.numQuizItems + " items")

  //Util.stopwatch(lines.foreach(line.parse(_)), "fastparsed lines")

  //val Parsed.Success(value, successIndex) = line.parse("a")
  //assert(value == (), successIndex == 1)

//

/*

  val io = new DefaultIO()
  val dataStore = new DataStore(io)
  val fileName = "data/test/testData10000.text-English word-German word.qgr"

  val lines = Source.fromFile(fileName).getLines()

  var reader = new PagedSeqReader(PagedSeq.fromLines(lines))

  Util.stopwatch(parse(line, reader), {
    while (!reader.atEnd) {
      parse(line, reader)
      reader = reader.rest
    }
      //match {
      //  case Success(matched, _) => //println("matched: " + matched);

     //   case Failure(msg, _) => println("FAILURE: " + msg)
     //   case _ => println("ERROR")
      //}
      //println("finished parsing")
    }, "parsing file with trivial parser")
*/

  //Util.stopwatch(parse(line, reader),  "parsing file with trivial parser ")


