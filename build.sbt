import sbt._

organization := "com.github.oranda"
name := "libanius-akka"
version := "0.4.2.2"

scalaVersion := "2.12.6"

homepage := Some(url("http://github.com/oranda/libanius-akka"))

licenses += ("GNU Affero General Public License", url("https://www.gnu.org/licenses/agpl-3.0.en.html"))

scmInfo := Some(ScmInfo(
  url("https://github.com/oranda/libanius-akka"),
  "scm:git:git@github.com/oranda/libanius-akka.git",
  Some("scm:git:git@github.com/oranda/libanius-akka.git")))

developers := List(
  Developer(
    id = "oranda",
    name = "James McCabe",
    email = "jjtmccabe@gmail.com",
    url = url("https://github.com/oranda")
  )
)

publishMavenStyle := true

publishTo := {
  val nexus = "https://oss.sonatype.org/"
  if (isSnapshot.value)
    Some("snapshots" at nexus + "content/repositories/snapshots")
  else
    Some("releases" at nexus + "service/local/staging/deploy/maven2")
}

scalacOptions ++= Seq("-unchecked", "-deprecation")

resolvers ++= Seq("Typesafe Repository" at "http://repo.typesafe.com/typesafe/releases/",
                  "Scalaz Bintray Repo" at "http://dl.bintray.com/scalaz/releases"
                 )

libraryDependencies ++= Seq("com.typesafe" % "config" % "1.3.4",
  "org.scalaz" %% "scalaz-core" % "7.2.25",
  "com.typesafe.akka" %% "akka-persistence" % "2.5.25",
  "com.typesafe.akka" %% "akka-remote" % "2.5.25",
  "org.apache.httpcomponents" % "httpclient" % "4.1.2",
  "com.typesafe.play" %% "play-json" % "2.6.7",
  "com.lihaoyi" %% "fastparse" % "1.0.0",
  "org.scala-lang.modules" %% "scala-parser-combinators" % "1.1.1",
  "org.fusesource.leveldbjni" % "leveldbjni-all" % "1.8",
  "org.specs2" %% "specs2-core" % "4.2.0" % "it,test",
  "org.specs2" %% "specs2-junit" % "4.2.0" % "it,test",
  "org.scalactic" %% "scalactic" % "3.0.8",
  "org.scalatest" %% "scalatest" % "3.0.8" % "it,test",
  "com.typesafe.akka" %% "akka-testkit" % "2.5.25" % "it,test"
)


configs(IntegrationTest)

Defaults.itSettings

// Use a different configuration for unit tests
javaOptions in Test += s"-Dconfig.file=${sourceDirectory.value}/test/resources/application-test.conf"
// We need to fork a JVM process when testing so the Java options above are applied
fork in Test := true
parallelExecution in Test := false  // for Akka Testkit

// Use a different configuration for simulations
javaOptions in IntegrationTest +=  s"-Dconfig.file=${sourceDirectory.value}/it/resources/application-it.conf"
fork in IntegrationTest := true
parallelExecution in IntegrationTest := false  // for Akka Testkit


artifactName := { (sv: ScalaVersion, module: ModuleID, artifact: Artifact) =>
  artifact.name + "-" + version + "." + artifact.extension
}

// an unmanaged dependency is no longer used, but these settings are retained in case it is needed
assemblyJarName in assembly := s"${name.value}-${version.value}-fat.jar"
assemblyOption in assembly := (assemblyOption in assembly).value.copy(includeScala = false)

// Exclude the config jar and akka jars from the fat jar. Ideally these jars would be
// excluded from the classpath using Provided scope, but this spoils the run task, so
// instead:
assemblyExcludedJars in assembly := {
  val cp = (fullClasspath in assembly).value
  cp filter { c =>
    c.data.getName.startsWith("config-") ||
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

addCompilerPlugin("org.psywerx.hairyfotr" %% "linter" % "0.1.17")


