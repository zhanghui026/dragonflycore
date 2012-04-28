package cn.dragonfly.core

import quartz.Time

object ExampleApp extends App {
  println("Hello, DragonflyCore")
  //每隔5分钟 5mn
  val timeSec = Time.parseDuration("5mn")
  import akka.util.duration

}
