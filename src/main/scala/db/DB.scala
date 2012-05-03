package db


import javax.sql.DataSource
import java.sql._
import akka.event.slf4j.Logger
import com.jolbox.bonecp.hooks.AbstractConnectionHook
import com.jolbox.bonecp.{ConnectionHandle, BoneCPDataSource}
import api.Configuration
import com.typesafe.config.{ConfigFactory, Config}


/**
 * Created with IntelliJ IDEA.
 * User: zhangh
 * Date: 12-4-28
 * Time: 上午10:48
 * To change this template use File | Settings | File Templates.
 */
/**
 * Zeus Databases API 管理多个连接池
 *
 */
trait DBApi {
  val datasources:List[(DataSource,String)]

  /**
   * 关闭某一datasource
   * @param ds
   */
  def shutdownPool(ds:DataSource)

  /**
   * 获得一个jdbc连接，auto-commit 设置为true
   * 不要忘了释放connection，通过调用close
    * @param name
   * @return
   */
  def getDataSource(name:String):DataSource

  def getConnection(name:String,autocommit:Boolean = true):Connection = {
    val connection  = getDataSource(name).getConnection
    connection.setAutoCommit(autocommit)
    connection
  }

  def withConnection[A](name:String)(block:Connection => A):A = {
      val connection = new AutoCleanConnection(getConnection(name))
    try{
      block(connection)
    } finally {
      connection.close()
    }
  }

  /**
   * 执行一串code，在jdbc transaction下，
   * connection和所有的statements 自动释放
   * @param name
   * @param block
   * @tparam A
   * @return
   */
  def withTransaction[A](name:String)(block:Connection =>A):A = {
    withConnection(name) { connection =>
      try{
        connection.setAutoCommit(false)
        val r = block(connection)
        connection.commit()
        r
      } catch {
        case e => connection.rollback();throw  e
      }
    }
  }
}

/**
 * 提供高级别的常用API 获得JDBC connection
 * 例如
 * {{{
 *   val conn = DB.getConnection("customers")
 * }}}
 */
object DB {
  private val dbApi = new BoneCPApi(Configuration(ConfigFactory.load()).getConfig("db").getOrElse(Configuration.empty))
  private def error = throw new Exception("DB错误")

  def getConnection(name:String = "default",autocommit:Boolean = true):Connection = {
    dbApi.getConnection(name,autocommit)
  }

  def getDataSource(name:String = "default"):DataSource = dbApi.getDataSource(name)

  def withConnection[A](name:String = "default")(block:Connection => A):A = {
    dbApi.withConnection(name)(block)
  }

  def withTransaction[A](name:String = "default")(block:Connection => A):A = {
    dbApi.withConnection(name)(block)
  }

}

private[db] class BoneCPApi(configuration:Configuration) extends DBApi {

 private def error(db:String,message:String = "") = throw configuration.reportError(db,message)

  private val dbNames = configuration.subKeys

  private def register(driver:String,c:Configuration) {
    try{
      DriverManager.registerDriver(Class.forName(driver).newInstance().asInstanceOf[Driver])
    } catch {
      case e => throw c.reportError("driver","没有找到Driver:["+driver+"]",Some(e))
    }
  }

  private def createDataSource(dbName:String,url:String,driver:String,conf:Configuration): DataSource = {
    val datasource = new BoneCPDataSource()
    //加载driver
    conf.getString("driver").map{driver =>
         try{
           DriverManager.registerDriver(Class.forName(driver).newInstance().asInstanceOf[Driver])
         } catch {
           case e =>  throw conf.reportError("driver","Driver没有找到:["+driver+"]",Some(e))
         }
    }
    val autocommit = conf.getBoolean("autocommit").getOrElse(true)
    val isolation = conf.getString("isolation").map{
      case "NONE" => Connection.TRANSACTION_NONE
      case "READ_COMMITED" => Connection.TRANSACTION_READ_COMMITTED
      case "READ_UNCOMMITED" => Connection.TRANSACTION_READ_UNCOMMITTED
      case "REPEATABLE_READ" => Connection.TRANSACTION_REPEATABLE_READ
      case "SERIALIZABLE" => Connection.TRANSACTION_SERIALIZABLE
      case unknown => throw conf.reportError("isolation","未知的isolation级别")
    }
    val catalog = conf.getString("defaultCatalog")
    val readOnly = conf.getBoolean("readOnly").getOrElse(false)



    val logger = Logger("com.jolbox.bonecp")

    //Re-apply per connection config @ checkout
    datasource.setConnectionHook(new AbstractConnectionHook {
      override def onCheckIn(connection:ConnectionHandle) {
        if (logger.isTraceEnabled){
          logger.trace("Check In connection [%s leased]".format(datasource.getTotalLeased))
        }
      }
      override def onCheckOut(connection:ConnectionHandle) {
        connection.setAutoCommit(autocommit)
        isolation.map(connection.setTransactionIsolation(_))
        connection.setReadOnly(readOnly)
        catalog.map(connection.setCatalog(_))
        if (logger.isTraceEnabled){
           logger.trace("Check out connection [%s leased]".format(datasource.getTotalLeased))
        }
      }
    })

    val PostgresFullUrl = "^postgres://([a-zA-Z0-9_]+):([^@]+)@([^/]+)/([^\\s]+)$".r
    val MysqlFullUrl = "^mysql://([a-zA-Z0-9_]+):([^@]+)@([^/]+)/([^\\s]+)$".r
    conf.getString("url") match {
      case Some(PostgresFullUrl(username,password,host,dbname)) =>
        datasource.setJdbcUrl("jdbc:postgresql://%s/%s".format(host,dbname))
        datasource.setUsername(username)
        datasource.setPassword(password)
      case Some(MysqlFullUrl(username,password,host,dbname)) =>
        datasource.setJdbcUrl("jdbc:mysql://%s/%s?useUnicode=yes&characterEncoding=UTF-8&connectionCollation=utf8_general_ci".format(host,dbname))
        datasource.setUsername(username)
        datasource.setPassword(password)
      case Some(s:String) => datasource.setJdbcUrl(s)
      case _ => throw conf.globalError("没有database的url配置 [%s]".format(conf))
    }

    conf.getString("user").map(datasource.setUsername(_))
    conf.getString("pass").map(datasource.setPassword(_))
    conf.getString("password").map(datasource.setPassword(_))

    //Pool 设置
    datasource.setPartitionCount(conf.getInt("partitionCount").getOrElse(1))
    datasource.setMaxConnectionsPerPartition(conf.getInt("maxConnectionsPerPatition").getOrElse(30))
    datasource.setMinConnectionsPerPartition(conf.getInt("minConnectionsPerPartition").getOrElse(5))
    datasource.setAcquireIncrement(conf.getInt("acquireIncrement").getOrElse(1))
    datasource.setAcquireRetryAttempts(conf.getInt("acquireRetryAttempts").getOrElse(10))
    datasource.setAcquireRetryDelayInMs(conf.getMilliseconds("acquireRetryDelay").getOrElse(1000))
    datasource.setConnectionTimeoutInMs(conf.getMilliseconds("connectionTimeout").getOrElse(1000))
    datasource.setIdleMaxAge(conf.getMilliseconds("idleMaxAge").getOrElse(1000*60*10),java.util.concurrent.TimeUnit.MILLISECONDS)
    datasource.setMaxConnectionAge(conf.getMilliseconds("maxConnectionAge").getOrElse(1000*60*60),java.util.concurrent.TimeUnit.MILLISECONDS)
    datasource.setDisableJMX(conf.getBoolean("disableJMX").getOrElse(true))
    datasource.setIdleConnectionTestPeriod(conf.getMilliseconds("idleConnectionTestPeriod").getOrElse(1000*60),java.util.concurrent.TimeUnit.MICROSECONDS)

    conf.getString("initSQL").map(datasource.setInitSQL(_))
    conf.getBoolean("logStatements").map(datasource.setLogStatementsEnabled(_))
    conf.getString("connectionTestStatement").map(datasource.setConnectionTestStatement(_))


    datasource

  }


  val datasources: List[(DataSource, String)] = dbNames.map { dbName =>
    val url = configuration.getString(dbName+".url").getOrElse(error(dbName,"缺失配置[db."+dbName+".url]"))
    val driver = configuration.getString(dbName+".driver").getOrElse(error(dbName,"缺失配置[db."+dbName+".driver]"))
    val extraConfig = configuration.getConfig(dbName).getOrElse(error(dbName,"缺失配置[db."+dbName+"]"))
    register(driver,extraConfig)
    createDataSource(dbName,url,driver,extraConfig) -> dbName
  }.toList

  /**
   * 关闭某一datasource
   * @param ds
   */
  def shutdownPool(ds: DataSource) = {
    ds match {
      case ds:BoneCPDataSource => ds.close()
      case _ => error("不能识别DataSource，所以不能关闭线程池")
    }
  }

  /**
   * 获得一个jdbc连接，auto-commit 设置为true
   * 不要忘了释放connection，通过调用close
   * @param name
   * @return
   */
  def getDataSource(name: String): DataSource = {
    datasources.filter(_._2 == name).headOption.map(e => e._1).getOrElse(error("不能找到对应datasource"+name))
  }
}

/**
 * 自动释放statement的connection
 * @param connection
 */
private class AutoCleanConnection(connection: Connection) extends Connection {

  private val statements = scala.collection.mutable.ListBuffer.empty[Statement]

  private def registering[T <: Statement](b: => T) = {
    val statement = b
    statements += statement
    statement
  }

  private def releaseStatements() {
    statements.foreach { statement =>
      statement.close()
    }
    statements.clear()
  }

  def createStatement() = registering(connection.createStatement())
  def createStatement(resultSetType: Int, resultSetConcurrency: Int) = registering(createStatement(resultSetType, resultSetConcurrency))
  def createStatement(resultSetType: Int, resultSetConcurrency: Int, resultSetHoldability: Int) = registering(createStatement(resultSetType, resultSetConcurrency, resultSetHoldability))
  def prepareStatement(sql: String) = registering(connection.prepareStatement(sql))
  def prepareStatement(sql: String, autoGeneratedKeys: Int) = registering(connection.prepareStatement(sql, autoGeneratedKeys))
  def prepareStatement(sql: String, columnIndexes: scala.Array[Int]) = registering(connection.prepareStatement(sql, columnIndexes))
  def prepareStatement(sql: String, resultSetType: Int, resultSetConcurrency: Int) = registering(connection.prepareStatement(sql, resultSetType, resultSetConcurrency))
  def prepareStatement(sql: String, resultSetType: Int, resultSetConcurrency: Int, resultSetHoldability: Int) = registering(connection.prepareStatement(sql, resultSetType, resultSetConcurrency, resultSetHoldability))
  def prepareStatement(sql: String, columnNames: scala.Array[String]) = registering(connection.prepareStatement(sql, columnNames))
  def prepareCall(sql: String) = registering(connection.prepareCall(sql))
  def prepareCall(sql: String, resultSetType: Int, resultSetConcurrency: Int) = registering(connection.prepareCall(sql, resultSetType, resultSetConcurrency))
  def prepareCall(sql: String, resultSetType: Int, resultSetConcurrency: Int, resultSetHoldability: Int) = registering(connection.prepareCall(sql, resultSetType, resultSetConcurrency, resultSetHoldability))

  def close() {
    releaseStatements()
    connection.close()
  }

  def clearWarnings() { connection.clearWarnings() }
  def commit() { connection.commit() }
  def createArrayOf(typeName: String, elements: scala.Array[AnyRef]) = connection.createArrayOf(typeName, elements)
  def createBlob() = connection.createBlob()
  def createClob() = connection.createClob()
  def createNClob() = connection.createNClob()
  def createSQLXML() = connection.createSQLXML()
  def createStruct(typeName: String, attributes: scala.Array[AnyRef]) = connection.createStruct(typeName, attributes)
  def getAutoCommit() = connection.getAutoCommit()
  def getCatalog() = connection.getCatalog()
  def getClientInfo() = connection.getClientInfo()
  def getClientInfo(name: String) = connection.getClientInfo(name)
  def getHoldability() = connection.getHoldability()
  def getMetaData() = connection.getMetaData()
  def getTransactionIsolation() = connection.getTransactionIsolation()
  def getTypeMap() = connection.getTypeMap()
  def getWarnings() = connection.getWarnings()
  def isClosed() = connection.isClosed()
  def isReadOnly() = connection.isReadOnly()
  def isValid(timeout: Int) = connection.isValid(timeout)
  def nativeSQL(sql: String) = connection.nativeSQL(sql)
  def releaseSavepoint(savepoint: Savepoint) { connection.releaseSavepoint(savepoint) }
  def rollback() { connection.rollback() }
  def rollback(savepoint: Savepoint) { connection.rollback(savepoint) }
  def setAutoCommit(autoCommit: Boolean) { connection.setAutoCommit(autoCommit) }
  def setCatalog(catalog: String) { connection.setCatalog(catalog) }
  def setClientInfo(properties: java.util.Properties) { connection.setClientInfo(properties) }
  def setClientInfo(name: String, value: String) { connection.setClientInfo(name, value) }
  def setHoldability(holdability: Int) { connection.setHoldability(holdability) }
  def setReadOnly(readOnly: Boolean) { connection.setReadOnly(readOnly) }
  def setSavepoint() = connection.setSavepoint()
  def setSavepoint(name: String) = connection.setSavepoint(name)
  def setTransactionIsolation(level: Int) { connection.setTransactionIsolation(level) }
  def setTypeMap(map: java.util.Map[String, Class[_]]) { connection.setTypeMap(map) }
  def isWrapperFor(iface: Class[_]) = connection.isWrapperFor(iface)
  def unwrap[T](iface: Class[T]) = connection.unwrap(iface)

  // JDBC 4.1
  def getSchema() = {
    connection.asInstanceOf[{ def getSchema(): String }].getSchema()
  }

  def setSchema(schema: String) {
    connection.asInstanceOf[{ def setSchema(schema: String): Unit }].setSchema(schema)
  }

  def getNetworkTimeout() = {
    connection.asInstanceOf[{ def getNetworkTimeout(): Int }].getNetworkTimeout()
  }

  def setNetworkTimeout(executor: java.util.concurrent.Executor, milliseconds: Int) {
    connection.asInstanceOf[{ def setNetworkTimeout(executor: java.util.concurrent.Executor, milliseconds: Int): Unit }].setNetworkTimeout(executor, milliseconds)
  }

  def abort(executor: java.util.concurrent.Executor) {
    connection.asInstanceOf[{ def abort(executor: java.util.concurrent.Executor): Unit }].abort(executor)
  }

}