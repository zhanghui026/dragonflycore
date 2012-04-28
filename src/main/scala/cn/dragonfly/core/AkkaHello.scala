package cn.dragonfly.core

import akka.util.duration._
import akka.actor.{ActorSystem, Actor, Props}
import org.slf4j.LoggerFactory
import quartz.Time
import akka.util.Duration
import java.util.concurrent.TimeUnit

/**
 * Created with IntelliJ IDEA.
 * User: zhangh
 * Date: 12-4-27
 * Time: 下午1:56
 * To change this template use File | Settings | File Templates.
 */
object AkkaHello extends App{
  val log = LoggerFactory.getLogger("cn.dragonfly.core.AkkaHello")
   val system = ActorSystem("job")
  system.scheduler.scheduleOnce(50 milliseconds, tickActor, "foo")
  val Tick = "tick"
  val tickActor = system.actorOf(Props(new Actor {
    def receive = {
      case Tick ⇒
      log.debug("sss")
    }
  }))
  //每天的0点
  val timeCronStr = """0 0 0 * * ?"""
  println(Time.cronInterval(timeCronStr))
  println(Time.parseCRONExpression(timeCronStr))
  val timeSec = Duration(86400000,TimeUnit.MILLISECONDS)
  println(timeSec.toDays)
  //This will schedule to send the Tick-message
  //to the tickActor after 0ms repeating every 50ms
  val cancellable =
    system.scheduler.schedule(10 seconds,
      timeSec,
      tickActor,
      Tick)
  Thread.sleep(1000022)
  cancellable.cancel()
  Thread.sleep(1000022)
  cancellable
  //This cancels further Ticks to be sent
//  cancellable.cancel()
  //cronExpression:
}
