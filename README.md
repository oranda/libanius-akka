Libanius Akka
=============

This is a version of the Libanius library implemented using Akka and Akka Persistence.

The purpose of Libanius is to aid learning. Basically it presents "quiz items" to the user, and for each one the user must select the correct answer option. Quiz items are presented at random according to a certain algorithm based on [spaced repetition](http://en.wikipedia.org/wiki/Spaced_repetition). An item has to be answered correctly several times before it is considered learnt.

The core use is as a vocabulary builder in a new language, but it is designed to be flexible enough to present questions and answers of all types.

The implementation is in Scala. The main target platforms are the Web and Android.

Suggestions for new features and code improvements will be happily received by:

James McCabe <jjtmccabe@gmail.com>


Usage
=====

To use libanius-akka as a library in your project, add this to your `build.sbt`:

    libraryDependencies += "com.github.oranda" %% "libanius-akka" % "0.4.2.2"
                                                                               `
A console UI is provided in this project. To run it, get a copy of 
`github.com/oranda/libanius-akka` using `git clone`. Unzip it, navigate to the root
directory of the project, and type:

    sbt run

Pick the option `com.oranda.libanius.consoleui.RunQuiz` and try out a sample quiz.

This has been tested with Scala 2.12.6, Java 8, and sbt 1.1.2.

For a graphical interface to Libanius, see https://github.com/oranda/libanius-scalajs-react-akka.


License
=======

Most Libanius source files are made available under the terms of the GNU Affero General Public License (AGPL).
See individual files for details.

Attribution info is in [SOURCES](SOURCES.md).
