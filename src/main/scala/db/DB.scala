package db


import javax.sql.DataSource
import java.sql.{Statement, Savepoint, Connection}

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

  }
}

class DB {

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