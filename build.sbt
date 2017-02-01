name := "blog-spark-recommendation"

version := "2.0-SNAPSHOT"

lazy val root = (project in file(".")).enablePlugins(PlayScala)
lazy val sparkVersion = "2.0.1"

scalaVersion := "2.11.7"


lazy val akkaVersion = "2.2.3" // override Akka to be this version to match the one in Spark

libraryDependencies ++= Seq(
  //jdbc,
  //anorm,
  cache,
  ws,
  // HTTP client
  "net.databinder.dispatch" %% "dispatch-core" % "0.11.1",
  // HTML parser
  "org.jodd" % "jodd-lagarto" % "3.5.2",
  // Spark
  "org.apache.spark" %% "spark-core" % sparkVersion,
  "org.apache.spark" %% "spark-mllib" % sparkVersion,
  // to add HiveContext
  "org.apache.spark" %% "spark-hive" % sparkVersion, 
  // Akka
  "com.typesafe.akka" %% "akka-actor" % akkaVersion,
  "com.typesafe.akka" %% "akka-slf4j" % akkaVersion,
  // MongoD
 "org.reactivemongo" %% "play2-reactivemongo" % "0.11.14"

)

dependencyOverrides += "com.fasterxml.jackson.core" % "jackson-core" % "2.6.5"

dependencyOverrides += "com.fasterxml.jackson.core" % "jackson-databind" % "2.6.5"

//play.Project.playScalaSettings

routesGenerator := InjectedRoutesGenerator




fork in run := true