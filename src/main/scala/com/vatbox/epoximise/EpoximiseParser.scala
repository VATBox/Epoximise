package com.vatbox.epoximise

import java.time.format.DateTimeFormatter
import java.time.{LocalDateTime, ZoneOffset}
import java.util.UUID

import org.bson._
import org.bson.codecs.UuidCodec
import org.bson.types.ObjectId
import org.json4s.JsonAST.{JBool, JDecimal, JDouble, JField, JInt, JLong, JNothing, JNull, JString, _}
import org.json4s.ParserUtil.ParseException
import org.json4s.{Formats, JValue}

import scala.collection.immutable.Iterable
import scala.math.ScalaNumber

/**
  * Created by talg on 23/11/2016.
  */
case class EpoximiseParser(
                            localDateTimeFormatter: DateTimeFormatter,
                            optimizeBigInt : Boolean,
                            zoneOffset: ZoneOffset
                          ) {
  def parse(jv: JValue)(implicit formats: Formats): BsonValue = jv match {
    case JObject(obj) => parseObject(obj)
    case JArray(arr) => parseIterable(arr)
    case JSet(set) => parseIterable(set)
    case primitive => throw new ParseException(s"Unable to parse $primitive to BsonValue", null)
  }

  private def parseObject(obj: List[JField])(implicit formats: Formats): BsonDocument = {
    val out = new BsonDocument()
    obj.foreach {
      case (name, jv) if jv == JNothing =>
        throw new ParseException(s"Can't do anything with nothing: [$name]", null)
      case (name, jv) =>
        out.put(name, jValueToBson(jv))
    }
    out
  }

  private def parseIterable(iterable: Iterable[JValue])(implicit formats: Formats): BsonArray = {
    val out = new BsonArray()
    iterable.filterNot(_ == JNothing).foreach {
      case jv if jv == JNothing =>
        throw new ParseException(s"Can't do anything with nothing in this array $iterable", null)
      case jv =>
        out.add(jValueToBson(jv))
    }
    out
  }

  private def jValueToBson(jValue: JValue)(implicit formats: Formats): BsonValue = jValue match {
    case JObject(JField(ObjectIdKeyName, JString(s)) :: Nil) if ObjectId.isValid(s) =>
      new BsonObjectId(new ObjectId(s))
    case JObject(JField(DateKeyName, JString(s)) :: Nil) =>
      formats.dateFormat.parse(s).map(date => new BsonDateTime(date.getTime)) match {
        case Some(bsonDT) => bsonDT
        case None => throw new ParseException(s"Failed to parse into Date from $s", null)
      }
    case JObject(JField(LocalDateTimeKeyName, JString(s)) :: Nil) =>
      val localDateTime = LocalDateTime.parse(s, localDateTimeFormatter)
      new BsonDateTime(localDateTime.toEpochSecond(zoneOffset) * 1000)
    case JObject(JField(UuidKeyName, JString(s)) :: Nil) =>
      UUIDtoBson(UUID.fromString(s))
    case JObject(jvObj) => parseObject(jvObj)
    case JArray(arr) => parseIterable(arr)
    case JSet(set) => parseIterable(set)
    case primitiveJValue => parseJValue(primitiveJValue)
  }

  private def parseJValue(jValue: JValue)(implicit formats: Formats): BsonValue = jValue match {
    case JNull => BsonNull.VALUE
    case JString(s) if s == null => new BsonString("null")
    case JString(s) => new BsonString(s)
    case JDouble(num) => new BsonDouble(num)
    case JLong(num) => new BsonInt64(num)
    case JBool(value) => new BsonBoolean(value)
    case JDecimal(num) => bigNumberToBson(num)
    case JInt(num) => bigNumberToBson(num)
    case unsupported => throw new ParseException(s"Following Jvalue $unsupported is unsupported", null)
  }

  private def bigNumberToBson(number: ScalaNumber): BsonValue = number match {
    case bi: BigInt if optimizeBigInt =>
      if (bi <= Int.MaxValue && bi >= Int.MinValue)
        new BsonInt32(bi.intValue())
      else if (bi <= Long.MaxValue && bi >= Long.MinValue) new BsonInt64(bi.longValue())
      else new BsonString(bi.toString)
    case bigNum => new BsonString(bigNum.toString)
  }

  private def UUIDtoBson(uuid: UUID): BsonValue = {
    val wrapper = new BsonDocumentWrapper[UUID](uuid, new UuidCodec(UuidRepresentation.STANDARD))
    wrapper
  }
}
