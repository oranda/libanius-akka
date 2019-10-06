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
import com.oranda.libanius.util.Util

import scala.util.parsing.combinator.JavaTokenParsers
import scala.util.parsing.input.CharSequenceReader


object TestParseByLineStandard extends App with JavaTokenParsers {

  val fileName = "data/test/testData100000.text-English word-German word.qgr"

  def getQgString: String = {
    val io = new DefaultIO()
    Util.stopwatch(io.readFile(fileName).get, "reading file " + fileName)
  }

  //def fnInt(ls: List[Int]) = 1
  //def fnString(str: String) = 0

  def line: Parser[String] = """(?m)^.*$""".r ^^ { case matched => (matched) } // reads a line and returns 0
  def file: Parser[List[String]] = rep(line) ^^ { case matched => (matched) }  // a file is a repetition of lines

  def toReader(str: String) = new CharSequenceReader(str)

  val io = new DefaultIO()
  val dataStore = new DataStoreDefault()

  Util.stopwatch({
    val lines: List[String] = parse(file, getQgString) match {
      case Success(matched, _) => matched
      case _ => println("FAILURE"); Nil
    }
    println("lines: " + lines.size)
  }, "TestParseByLineStandard lines")
}


/*
object TestParseByGroupBase extends App {
  def memlevels: Iterable[String] = {
    getQgString.split("#quizGroupPartition")
  }

  val levels = Util.stopwatch(memlevels, "TestParseByGroupBase") // 135ms
  println("number of levels: " + levels.size) // 8
}

object TestParseByGroupStandard extends App {

}

object TestParseByGroupFast extends App {

}
*/



import TestParse._

object TestParseByLineBase extends App {

  def linesFiltered: Iterable[String] = {
    val qgLines = getQgString.split("\\n")
    qgLines.filter(_.startsWith("a"))
  }

  val lines = Util.stopwatch(linesFiltered, "TestParseByLineBase") // 240ms
  println("number of lines: " + lines.size) // 6413
}




