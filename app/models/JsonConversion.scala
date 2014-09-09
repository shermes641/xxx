package models

import anorm._
import play.api.libs.json._

// Contains implicit conversions for dealing with JSON in PostgreSQL.
trait JsonConversion {
  /**
   * Converts configuration_data field to JSON.
   * @return JSON value with configuration data.
   */
  implicit def rowToJsValue: Column[JsValue] = Column.nonNull { (value, meta) =>
    val MetaDataItem(qualified, nullable, clazz) = meta
    value match {
      case pgo: org.postgresql.util.PGobject => Right(Json.parse(pgo.getValue))
      case _ => {
        Left(TypeDoesNotMatch("Cannot convert " + value + ":" +
          value.asInstanceOf[AnyRef].getClass + " to JsValue for column " + qualified))
      }
    }
  }
}
