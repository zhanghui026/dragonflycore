package api

import scala.collection.JavaConverters._
import scalax.io.Input
import com.typesafe.config.{ConfigFactory, ConfigOrigin, ConfigException, Config}

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
   * 读取值
   * @param path
   * @param v
   * @tparam T
   * @return
   */
  private def readValue[T](path:String,v: =>T):Option[T] = {
    try{
      Option(v)
    }catch {
      case e:ConfigException.Missing => None
      case e => None
    }
  }


  /**
   *
   * @param path
   * @param validValues
   * @return
   */
  def getString(path:String,validValues:Option[Set[String]] = None):Option[String] = readValue(path,underlying.getString(path)).map { value =>
     validValues match {
       case Some(values) if values.contains(value) => value
       case Some(values) if values.isEmpty => value
       case Some(values) => throw reportError(path,"不正确的值，可设置的值为 "+(values.reduceLeft(_+","+_)))
       case None => value
     }
  }

  def getInt(path:String):Option[Int] = readValue(path,underlying.getInt(path))

  def getLong(path:String):Option[Long] = readValue(path,underlying.getLong(path))

  def getBoolean(path:String):Option[Boolean] = readValue(path,underlying.getBoolean(path))

  def getMilliseconds(path:String):Option[Long] = readValue(path,underlying.getMilliseconds(path))


  /**
   * 配置err
   * @param path
   * @param message
   * @param e
   * @return
   */
  def reportError(path:String,message:String,e:Option[Throwable] = None):ZeusException = {
    Configuration.configError(if(underlying.hasPath(path)) underlying.getValue(path).origin else underlying.root.origin(), message, e)
  }

  /**
   * 全局配置error
   * @param message
   * @param e
   * @return
   */
  def globalError(message:String,e:Option[Throwable] = None):ZeusException = {
    Configuration.configError(underlying.root().origin(),message,e)
  }

  def getBytes(path:String):Option[Long] = readValue(path,underlying.getBytes(path))

  def getConfig(path:String):Option[Configuration] = readValue(path,underlying.getConfig(path)).map(Configuration(_))
}

object Configuration {

  def empty = Configuration(ConfigFactory.empty())
  private def configError(origin:ConfigOrigin,message:String,e:Option[Throwable] = None): ZeusException = {
    import scalax.io.JavaConverters._
    new ZeusException("Config配置error",message,e) with ZeusException.ExceptionSource {
      def line: Option[Int] = Option(origin.lineNumber())
      def position: Option[Int] = None
      def input: Option[Input] = Option(origin.url).map(_.asInput)
      def sourceName: Option[String] = Option(origin.filename)
      override def toString = "Config配置error" + getMessage
    }
  }
}

