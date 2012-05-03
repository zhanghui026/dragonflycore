package api

/**
 * Created with IntelliJ IDEA.
 * User: zhangh
 * Date: 12-5-2
 * Time: 下午3:05
 * To change this template use File | Settings | File Templates.
 */

object ZeusException {

  private val generator = new java.util.concurrent.atomic.AtomicLong(System.currentTimeMillis())
  //26进制
  def nextId = java.lang.Long.toString(generator.incrementAndGet(),26)

  trait UsefulException {
    /**
     *    Exception title
     */
    def title:String
    /*
    * Exception 描述
     */
    def description:String

    /**
     * Exception 原因
     */
    def cause: Option[Throwable]
    /*
     * Exception 独一id
     */
    def id:String

  }

  trait ExceptionSource {
    self: ZeusException =>
    /**
     * 错误行
     * @return
     */
    def line:Option[Int]

    /**
     * 列位置
     * @return
     */
    def position:Option[Int]

    /**
     * 读取流
     * @return
     */
    def input:Option[scalax.io.Input]
    /*
     * 源文件名字
     */
    def sourceName: Option[String]

    def interestingLines(border:Int = 4):Option[(Int,Seq[String],Int)] = {
      for (f<-input;l<-line;val (first,last) = f.slurpString.split('\n').splitAt(l-1);focus<-last.headOption) yield {
        val before = first.takeRight(border)
        val after = last.drop(1).take(border)
        val firstLine = 1-before.size
        val errorLine = before.size
        (firstLine,(before :+ focus)++ after ,errorLine)
      }
    }
  }

}
class ZeusException(val title:String,val description:String,val cause:Option[Throwable] = None) extends RuntimeException("%s [%s]".format(title,description),cause.orNull) with ZeusException.UsefulException {
  /*
     * Exception 独一id
     */
  def id: String = ZeusException.nextId
  override def toString = "ZeusException:" + getMessage
}
