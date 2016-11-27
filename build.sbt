import sbt._
name := """Epoximise"""
maintainer := "Tal <tal@vatbox.com>"
packageSummary := "Epoximise is a Mongo Scala async driver helper"
packageDescription := "Library that takes a class and then maps it to it's DB representation"
version := "1.0.0"

scalaVersion := "2.11.8"
organization := "com.vatbox"
// Change this to another test framework if you prefer
libraryDependencies ++= {
  val MongoScalaDriverVersion = "1.1.1"
  val Json4sVersion = "3.5.0"
  Seq(
    "org.mongodb.scala" %% "mongo-scala-driver" % MongoScalaDriverVersion % Provided,
    "org.json4s" %% "json4s-core" % Json4sVersion % Provided,
    "org.scalatest" %% "scalatest" % "2.2.6" % "test",
    "org.scalacheck" %% "scalacheck" % "1.12.6" % "test"
  )
}
//
//
//
//
//credentials += Credentials(Path.userHome / ".ivy2" / ".sonatypecredentials")
//publishTo := {
//  val nexus = "https://oss.sonatype.org/"
//  if (isSnapshot.value)
//    Some("snapshots" at nexus + "content/repositories/snapshots")
//  else
//    Some("releases"  at nexus + "service/local/staging/deploy/maven2")
//}
//publishMavenStyle := true
//pomIncludeRepository := { _ => false }
//pomExtra := (
//  <url>https://vatbox.github.io/polyjuicelib/</url>
//    <licenses>
//      <license>
//        <name>Apache-style</name>
//        <url>https://opensource.org/licenses/Apache-2.0</url>
//        <distribution>repo</distribution>
//      </license>
//    </licenses>
//    <scm>
//      <url>https://github.com/VATBox/polyjuicelib.git</url>
//      <connection>scm:git:git@github.com:VATBox/polyjuicelib.git</connection>
//    </scm>
//    <developers>
//      <developer>
//        <id>talgendler</id>
//        <name>Tal Gendler</name>
//        <url>https://github.com/talgendler</url>
//      </developer>
//    </developers>)
//
