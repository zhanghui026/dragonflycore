package cn.dragonfly.core

import quartz.Time
import sun.security.krb5.Config
import com.typesafe.config.ConfigFactory

object ExampleApp extends App {
  println("Hello, DragonflyCore")
  //每隔5分钟 5mn
  val timeSec = Time.parseDuration("5mn")
  import akka.util.duration
  val conf = ConfigFactory.load()
  println(conf.getString("time"))
}
