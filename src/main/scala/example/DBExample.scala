package example

import db.DB
import anorm.Sql

/**
 * Created with IntelliJ IDEA.
 * User: zhangh
 * Date: 12-5-3
 * Time: 下午3:16
 * To change this template use File | Settings | File Templates.
 */
import anorm._
object DBExample extends App{
   val result = DB.withConnection(){
     implicit con =>  val r = SQL("select 1").execute();println(r);r
   }

  DB.withConnection(){implicit c =>
    val result:Int = SQL("delete from City where id = 99").executeUpdate()
  }

  DB.withConnection(){implicit c =>
    val result = SQL(
    """
   select * from COUNTRY c
  join countrylanguage l on l.CountryCode = c.Code
  where c.code = 'FRA';
    """)()
    println(result.toList.foreach(println))
    val res2 = SQL(
      """
        select * from Country c
        join CountryLanguage l on l.CountryCode = c.Code
        where c.code = {countryCode};
      """
    ).on("countryCode" -> "FRA")()
    print(res2.toList)
  }


}
