package models

import anorm._
import org.joda.time.DateTime
import org.joda.time.format.{DateTimeFormatter, DateTimeFormat}
import play.api.libs.json._

// Contains implicit conversions for dealing with JSON in PostgreSQL.
trait JsonConversion {
  /**
   * Converts configuration_data field to JSON.
   * @return JSON value with configuration data.
   */
  implicit def rowToJsValue: Column[JsValue] = Column.nonNull1 { (value, meta) =>
    val MetaDataItem(qualified, nullable, clazz) = meta
    value match {
      case pgo: org.postgresql.util.PGobject => Right(Json.parse(pgo.getValue))
      case _ => {
        Left(TypeDoesNotMatch("Cannot convert " + value + ":" +
          value.asInstanceOf[AnyRef].getClass + " to JsValue for column " + qualified))
      }
    }
  }

  /*
  val dateFormatGeneration: DateTimeFormatter = DateTimeFormat.forPattern("yyyyMMddHHmmssSS")

  implicit def rowToDateTime: Column[DateTime] = Column.nonNull { (value, meta) =>
    val MetaDataItem(qualified, nullable, clazz) = meta
    value match {
      case ts: java.sql.Timestamp => Right(new DateTime(ts.getTime))
      case d: java.sql.Date => Right(new DateTime(d.getTime))
      case str: java.lang.String => Right(dateFormatGeneration.parseDateTime(str))
      case _ => Left(TypeDoesNotMatch("Cannot convert " + value + ":" + value.asInstanceOf[AnyRef].getClass) )
    }
  }

  implicit val dateTimeToStatement = new ToStatement[DateTime] {
    def set(s: java.sql.PreparedStatement, index: Int, aValue: DateTime): Unit = {
      s.setTimestamp(index, new java.sql.Timestamp(aValue.withMillisOfSecond(0).getMillis()) )
    }
  }
  */
}
