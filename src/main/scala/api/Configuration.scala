package api

import com.typesafe.config.Config
import scala.collection.JavaConverters._

/**
 * Created with IntelliJ IDEA.
 * User: zhangh
 * Date: 12-4-23
 * Time: 上午10:50
 * To change this template use File | Settings | File Templates.
 */

case class Configuration(underlying:Config) {

  /**
   *  合并2个Configurations.若other原config同时存在key，则新key覆盖旧key
   *
   */
  def ++(other:Configuration):Configuration = {
    Configuration(other.underlying.withFallback(underlying))
  }

  /**
   * 返回所有当前config下的key值
   * @return
   */
  def keys : Set[String] = underlying.entrySet().asScala.map(_.getKey).toSet

  /**
   * 返回当前config下的子key,比如a.b.c ,a.b.d,a.c.b,a.c.d
   * 则a的子key为 b,c
   * @return
   */
  def subKeys: Set[String] = keys.map{s => s.split('.').head}

  /**
   * 返回option值。若没有对应path的key值 则返回None
   * @param path
   * @tparam T
   * @return
   */
  def getOption[T](path:String):Option[T] ={
    try{
      underlying.getValue(path).unwrapped() match {
        case t:T => Option(t)
        case _ => None
      }
    }catch {
      case e:Exception => None
    }
   }
}

object Configuration {
  implicit def conf2Configuration(conf: Config) = Configuration(conf)
}
