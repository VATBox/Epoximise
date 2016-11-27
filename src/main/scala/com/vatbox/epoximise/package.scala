package com.vatbox

import java.time.format.{DateTimeFormatter, DateTimeFormatterBuilder}
import java.time.{LocalDateTime, ZoneOffset}
import java.util.{Date, UUID}

import org.bson.types.ObjectId
import org.json4s.JsonAST.{JField, JObject, JString}
import org.json4s._
import org.json4s.reflect.TypeInfo

/**
  * Created by talg on 24/11/2016.
  */
package object epoximise {
  private[epoximise] val DateKeyName = "$dt"
  private[epoximise] val LocalDateTimeKeyName = "$ldt"
  private[epoximise] val ObjectIdKeyName = "$oid"
  private[epoximise] val UuidKeyName = "$uuid"
  private[epoximise] val DefaultDateTimeFormatter: DateTimeFormatter = new DateTimeFormatterBuilder().appendPattern("yyyy-MM-dd'T'HH:mm:ss'Z'").toFormatter
  private[epoximise] val DefaultZoneOffSet = ZoneOffset.UTC

  private[epoximise] def uuidAsJValue(u: UUID): JValue = JObject(JField(UuidKeyName, JString(u.toString)) :: Nil)
  private[epoximise] def dateAsJValue(d: Date)(implicit formats: Formats) = JObject(JField(DateKeyName, JString(formats.dateFormat.format(d))) :: Nil)
  private[epoximise] def localDateAsJValue(d: LocalDateTime, localDateTimeFormatter: DateTimeFormatter)(implicit formats: Formats) = JObject(JField(LocalDateTimeKeyName, JString(d.format(localDateTimeFormatter))) :: Nil)
  private[epoximise] def isCustomSerializerUsed[A](clazz: Class[A])(implicit formats: Formats): Boolean = formats.customSerializers.exists(_.getClass == clazz)
  private[epoximise] def isCustomSerializerUsed[A <: Serializer[_]](instance: A)(implicit formats: Formats): Boolean = formats.customSerializers.contains(instance)
  private[epoximise] def objectIdAsJValue(oid: ObjectId) = JObject(JField(ObjectIdKeyName, JString(oid.toString)) :: Nil)

  /**
    * Provides a way to serialize/de-serialize ObjectIds.
    *
    * Queries for a ObjectId (oid) using the lift-json DSL look like:
    * ("_id" -> ("$oid" -> oid.toString))
    */
  object ObjectIdSerializer extends Serializer[ObjectId] {
    private val ObjectIdClass = classOf[ObjectId]

    def deserialize(implicit format: Formats): PartialFunction[(TypeInfo, JValue), ObjectId] = {
      case (TypeInfo(ObjectIdClass, _), json) => json match {
        case JObject(JField(ObjectIdKeyName, JString(s)) :: Nil) if ObjectId.isValid(s) =>
          new ObjectId(s)
        case x => throw new MappingException(s"Can't convert $x to ObjectId")
      }
    }

    def serialize(implicit formats: Formats): PartialFunction[Any, JValue] = {
      case x: ObjectId => objectIdAsJValue(x)
    }
  }

  /**
    * Provides a way to serialize/de-serialize Dates.
    *
    * Queries for a Date (dt) using the lift-json DSL look like:
    * ("dt" -> ("$dt" -> formats.dateFormat.format(dt)))
    */
  case class LocalDateTimeSerializer(localDateTimeFormatter: DateTimeFormatter) extends Serializer[LocalDateTime] {
    private val DateClass = classOf[LocalDateTime]

    def deserialize(implicit format: Formats): PartialFunction[(TypeInfo, JValue), LocalDateTime] = {
      case (TypeInfo(DateClass, _), json) => json match {
        case JObject(JField(LocalDateTimeKeyName, JString(s)) :: Nil) =>
          LocalDateTime.parse(s, localDateTimeFormatter)
        case x => throw new MappingException(s"Can't convert $x to LocalDateTime")
      }
    }

    def serialize(implicit format: Formats): PartialFunction[Any, JValue] = {
      case ld: LocalDateTime => localDateAsJValue(ld, localDateTimeFormatter)
    }
  }

  /**
    * Provides a way to serialize/de-serialize Dates.
    *
    * Queries for a Date (dt) using the lift-json DSL look like:
    * ("dt" -> ("$dt" -> formats.dateFormat.format(dt)))
    */
  object DateSerializer extends Serializer[Date] {
    private val DateClass = classOf[Date]

    def deserialize(implicit format: Formats): PartialFunction[(TypeInfo, JValue), Date] = {
      case (TypeInfo(DateClass, _), json) => json match {
        case JObject(JField(DateKeyName, JString(s)) :: Nil) =>
          format.dateFormat.parse(s).getOrElse(throw new MappingException(s"Can't parse $s to Date"))
        case x => throw new MappingException(s"Can't convert $x to Date")
      }
    }

    def serialize(implicit format: Formats): PartialFunction[Any, JValue] = {
      case d: Date => dateAsJValue(d)
    }
  }

  /**
    * Provides a way to serialize/de-serialize UUIDs.
    *
    * Queries for a UUID (u) using the lift-json DSL look like:
    * ("uuid" -> ("$uuid" -> u.toString))
    */
  object UUIDSerializer extends Serializer[UUID] {
    private val UUIDClass = classOf[UUID]
    def deserialize(implicit format: Formats): PartialFunction[(TypeInfo, JValue), UUID] = {
      case (TypeInfo(UUIDClass, _), json) => json match {
        case JObject(JField(UuidKeyName, JString(s)) :: Nil) => UUID.fromString(s)
        case x => throw new MappingException(s"Can't convert $x to UUID")
      }
    }

    def serialize(implicit format: Formats): PartialFunction[Any, JValue] = {
      case x: UUID => uuidAsJValue(x)
    }
  }

}
