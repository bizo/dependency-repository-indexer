organization := "com.bizo"

name := "dependency-repository-indexer"

version := "0.0.2"

scalaVersion := "2.10.3"

scalacOptions ++= Seq("-unchecked", "-deprecation", "-language:_")

libraryDependencies ++= Seq(
  "com.amazonaws" % "aws-java-sdk" % "1.9.0",
  "org.mongodb" % "mongo-java-driver" % "2.11.3",
  "junit" % "junit" % "4.10" % "test",
  "com.novocode" % "junit-interface" % "0.10-M4" % "test"
)

EclipseKeys.withSource := true

packAutoSettings
