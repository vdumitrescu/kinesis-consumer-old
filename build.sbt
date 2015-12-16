name := "kinesis-consumer"

version := "1.0"

scalaVersion := "2.11.7"

resolvers += Resolver.sonatypeRepo("public")

libraryDependencies ++= Seq(
  "com.github.scopt" %% "scopt" % "3.3.0",
  "com.typesafe.play" %% "play-json" % "2.4.6",
  "com.gilt" %% "gfc-logging" % "0.0.3",
  "com.gilt" %% "gfc-util" % "0.1.1",
  "com.amazonaws" % "amazon-kinesis-client" % "1.6.1",
  "com.amazonaws" % "aws-java-sdk-sts" % "1.10.40"
)
