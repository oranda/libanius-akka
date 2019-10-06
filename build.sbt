import sbt._

name := "libanius-akka"

version := "0.1"

scalaVersion := "2.12.6"

scalacOptions ++= Seq("-unchecked", "-deprecation")

resolvers ++= Seq("Typesafe Repository" at "http://repo.typesafe.com/typesafe/releases/",
                  "snapshots" at "http://oss.sonatype.org/content/repositories/snapshots",
                  "releases"  at "http://oss.sonatype.org/content/repositories/releases",
                  "Scalaz Bintray Repo" at "http://dl.bintray.com/scalaz/releases"
                 )

libraryDependencies ++= Seq("com.typesafe" % "config" % "1.3.4",
  "org.specs2" %% "specs2-core" % "4.2.0" % "test",
  "org.specs2" %% "specs2-junit" % "4.2.0" % "test",
  "org.scalaz" %% "scalaz-core" % "7.2.25",
  "com.typesafe.akka" %% "akka-persistence" % "2.5.25",
  "com.typesafe.akka" %% "akka-remote" % "2.5.25",
  "org.apache.httpcomponents" % "httpclient" % "4.1.2",
  "com.typesafe.play" %% "play-json" % "2.6.7",
  "com.lihaoyi" %% "fastparse" % "1.0.0",
  "org.scala-lang.modules" %% "scala-parser-combinators" % "1.1.1",
  "org.fusesource.leveldbjni" % "leveldbjni-all" % "1.8"
)

unmanagedClasspath in Runtime += (baseDirectory map { bd => Attributed.blank(bd / "config") }).value

unmanagedClasspath in Test += (baseDirectory map { bd => Attributed.blank(bd / "config") }).value

parallelExecution in Test := true

artifactName := { (sv: ScalaVersion, module: ModuleID, artifact: Artifact) =>
  artifact.name + "-" + version + "." + artifact.extension
}

assemblyJarName in assembly := s"${name.value}-${version.value}-fat.jar"

assemblyOption in assembly := (assemblyOption in assembly).value.copy(includeScala = false)

// Exclude the config jar and akka jars from the fat jar. Ideally these jars would be
// excluded from the classpath using Provided scope, but this spoils the run task, so
// instead:
assemblyExcludedJars in assembly := {
  val cp = (fullClasspath in assembly).value
  cp filter { c =>
    c.data.getName.startsWith("config-")
    c.data.getName.startsWith("akka-")
  }
}

test in assembly := {}

scalacOptions += "-feature"

initialCommands in console := "import scalaz._, Scalaz._"

exportJars := false

fork := false

javaOptions in run += "-XX:+UseConcMarkSweepGC"

javaOptions in run += "-XX:+CMSClassUnloadingEnabled"

javaOptions in run += "-XX:PermSize=512M"

javaOptions in run += "-XX:MaxPermSize=512M"


import sbt._

addCompilerPlugin("org.psywerx.hairyfotr" %% "linter" % "0.1.17")

