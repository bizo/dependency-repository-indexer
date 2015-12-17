organization := "com.bizo"

name := "dependency-repository-indexer"

version := "0.0.2.4"

scalaVersion := "2.10.6"

scalacOptions ++= Seq("-unchecked", "-deprecation", "-language:_")

libraryDependencies ++= Seq(
  "com.amazonaws" % "aws-java-sdk" % "1.9.0",
  "com.google.guava" % "guava" % "18.0",
  "org.mongodb" % "mongo-java-driver" % "2.11.3",
  "com.google.code.findbugs" % "jsr305" % "1.3.9" % "compile",  
  "junit" % "junit" % "4.10" % "test",
  "com.novocode" % "junit-interface" % "0.10-M4" % "test"
)

EclipseKeys.withSource := true

packAutoSettings
