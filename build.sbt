import sbt._
name := """Epoximise"""
maintainer := "Tal <tal@vatbox.com>"
packageSummary := "Epoximise is a Mongo Scala async driver helper"
packageDescription := "Library that takes a class and then maps it to it's DB representation"
version := "1.0.4"

scalaVersion := "2.12.1"
crossScalaVersions := Seq("2.11.8", "2.12.1")
organization := "com.vatbox"

credentials += Credentials(Path.userHome / ".ivy2" / ".sonatypecredentials")
publishTo := {
  val nexus = "https://oss.sonatype.org/"
  if (isSnapshot.value)
    Some("snapshots" at nexus + "content/repositories/snapshots")
  else
    Some("releases"  at nexus + "service/local/staging/deploy/maven2")
}
publishMavenStyle := true
pomIncludeRepository := { _ => false }
pomExtra := (
  <url>https://vatbox.github.io/Epoximise/</url>
    <licenses>
      <license>
        <name>Apache-style</name>
        <url>https://opensource.org/licenses/Apache-2.0</url>
        <distribution>repo</distribution>
      </license>
    </licenses>
    <scm>
      <url>https://github.com/VATBox/Epoximise.git</url>
      <connection>scm:git:git@github.com:VATBox/Epoximise.git</connection>
    </scm>
    <developers>
      <developer>
        <id>talgendler</id>
        <name>Tal Gendler</name>
        <url>https://github.com/talgendler</url>
      </developer>
    </developers>)

libraryDependencies ++= {
  val MongoScalaDriverVersion = "2.2.0"
  val Json4sVersion = "3.6.0-M2"
  Seq(
    "org.mongodb.scala" %% "mongo-scala-driver" % MongoScalaDriverVersion % Provided,
    "org.json4s" %% "json4s-core" % Json4sVersion % Provided,
    "org.scalatest" %% "scalatest" % "3.0.1" % "test" withSources(),
    "org.scalacheck" %% "scalacheck" % "1.13.4" % "test" withSources()
  )
}
