import sbt._
import sbt.Keys._

object ProjectBuild extends Build {

  lazy val root = Project(
    id = "root",
    base = file("."),
    settings = Project.defaultSettings ++ Seq(
      name := "DragonflyCore",
      organization := "cn.dragonfly",
      version := "0.1-SNAPSHOT",
      scalaVersion := "2.9.1",
      // add other settings here
      libraryDependencies ++=  Seq(
        "org.quartz-scheduler" % "quartz" % "2.1.4",
        "ch.qos.logback" % "logback-classic" % "1.0.1",
        "ch.qos.logback" % "logback-classic" % "1.0.1",
        "org.slf4j" % "slf4j-api" % "1.6.4",
        "com.google.guava" %  "guava" % "10.0.1",
        "play.db" %% "anorm" % "2.0",
        "mysql" % "mysql-connector-java" % "5.1.18",
        "play.config" %% "config" % "0.3.0",
        "com.typesafe.akka" % "akka-actor" % "2.0.1",
        "com.typesafe.akka" % "akka-slf4j" % "2.0.1",
        "com.github.scala-incubator.io" %%  "scala-io-file" % "0.2.0",
        ("com.jolbox"                       %    "bonecp"                   %   "0.7.1.RELEASE" notTransitive())
          .exclude("com.google.guava", "guava")
          .exclude("org.slf4j", "slf4j-api")

      ),
      externalResolvers <<= resolvers map { rs =>
        Resolver.withDefaultResolvers(rs, mavenCentral = false, scalaTools = false)
      },
      resolvers += "adccRepo" at "http://192.168.241.162:8081/nexus/content/groups/public/",
      javacOptions ++= Seq("-source", "1.6", "-target", "1.6", "-encoding", "UTF-8")

    )
  )

}
