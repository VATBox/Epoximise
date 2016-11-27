package com.vatbox.epoximise

import java.time.format.DateTimeFormatter
import java.time.{Instant, LocalDateTime, ZoneOffset}
import java.util.{Calendar, Date, UUID}

import org.bson._
import org.bson.codecs.UuidCodec
import org.bson.types.ObjectId
import org.json4s.JsonAST.{JNull, JString}
import org.json4s.ParserUtil.ParseException
import org.json4s.{Formats, _}

import scala.collection.JavaConversions._
/**
  * Created by talg on 23/11/2016.
  */
case class EpoximiseSerializer(
                                localDateTimeFormatter: DateTimeFormatter,
                                zoneOffset: ZoneOffset
                              ) {
  def serialize(a: Any)(implicit formats: Formats): JValue = a.asInstanceOf[AnyRef] match {
    case null => JNull
    case b: BsonNull => JNull
    case b: BsonArray => JArray(b.toList.map(serialize))
    case b: BsonBoolean => JBool(b.getValue)
      /** LocalDateTime can be different up to the Nano seconds, since MongoDB have only Millis precision
       [[https://docs.mongodb.com/manual/reference/method/Date/#behavior]]
      * */
    case b: BsonDateTime if isCustomSerializerUsed(classOf[LocalDateTimeSerializer]) => localDateAsJValue(LocalDateTime.ofInstant(Instant.ofEpochMilli(b.getValue),zoneOffset), localDateTimeFormatter) // UTC Epoch [[https://docs.mongodb.com/manual/reference/bson-types/#date]]
    case b: BsonDateTime if isCustomSerializerUsed(DateSerializer) => dateAsJValue(new Date(b.getValue))// UTC Epoch [[https://docs.mongodb.com/manual/reference/bson-types/#date]]
    case b: BsonDateTime => JString(formats.dateFormat.format(new Date(b.getValue)))// UTC Epoch [[https://docs.mongodb.com/manual/reference/bson-types/#date]]
    case b: BsonDocument => JObject(
      b.entrySet.toList.map(entry => JField(entry.getKey, serialize(entry.getValue)))
    )
    case b: BsonBinary if b.getType == BsonBinarySubType.UUID_STANDARD.getValue => uuidAsJValue(binaryUUID(b))
    case b: BsonDouble => JDouble(b.getValue)
    case b: BsonInt32 => JInt(BigInt(b.getValue))
    case b: BsonInt64 => JLong(b.getValue)
    case b: BsonObjectId if isCustomSerializerUsed(ObjectIdSerializer) => objectIdAsJValue(b.getValue)
    case b: BsonObjectId => JString(b.getValue.toString)
    case b: BsonString => JString(b.getValue)
    case x => typeToJValue(x)
  }

  /*
    * This is used to convert DBObjects into JObjects
    */
  private def typeToJValue(a: Any)(implicit formats: Formats): JValue = a match {
    case null => JNull
    case x: String => JString(x)
    case x: Int => JInt(x)
    case x: Long => JLong(x)
    case x: Double => JDouble(x)
    case x: Float => JDouble(x)
    case x: Byte => JInt(BigInt(x))
    case x: BigInt => JInt(x)
    case x: BigDecimal => JDecimal(x)
    case x: Boolean => JBool(x)
    case x: Short => JInt(BigInt(x))
    case x: java.lang.Integer => JInt(BigInt(x.asInstanceOf[Int]))
    case x: java.lang.Long => JLong(x)
    case x: java.lang.Double => JDouble(x.asInstanceOf[Double])
    case x: java.lang.Float => JDouble(x.asInstanceOf[Float])
    case x: java.lang.Byte => JInt(BigInt(x.asInstanceOf[Byte]))
    case x: java.lang.Boolean => JBool(x.asInstanceOf[Boolean])
    case x: java.lang.Short => JInt(BigInt(x.asInstanceOf[Short]))
    case x: Date => dateAsJValue(x)
    case x: ObjectId if isCustomSerializerUsed(ObjectIdSerializer) => objectIdAsJValue(x)
    case x: ObjectId => JString(x.toString)
    case x: UUID => uuidAsJValue(x)
    /* perhaps it will be supported in the future from Mongo Inc. */
    case x: LocalDateTime if isCustomSerializerUsed(classOf[LocalDateTimeSerializer]) => localDateAsJValue(x, localDateTimeFormatter)
    case x: LocalDateTime => throw new ParseException(s"LocalDateTime(${x.toString}) can be serialized if you add a serializer - [LocalDateTimeSerializer] or your own", null)
    case x: Calendar => dateAsJValue(x.getTime)

    case _ => JNothing
  }

  private def binaryUUID(bin: BsonBinary) = {
    /**
      * This code was copied from [[UuidCodec]] since no better elegant solution was found
      */
    def readLongFromArrayBigEndian(bytes: Array[Byte], offset: Int): Long = {
      var x: Long = 0
      x |= (0xFFL & bytes(offset + 7))
      x |= (0xFFL & bytes(offset + 6)) << 8
      x |= (0xFFL & bytes(offset + 5)) << 16
      x |= (0xFFL & bytes(offset + 4)) << 24
      x |= (0xFFL & bytes(offset + 3)) << 32
      x |= (0xFFL & bytes(offset + 2)) << 40
      x |= (0xFFL & bytes(offset + 1)) << 48
      x |= (0xFFL & bytes(offset)) << 56
      x
    }
    if (bin.getData.length == 16) {
      new UUID(readLongFromArrayBigEndian(bin.getData, 0), readLongFromArrayBigEndian(bin.getData, 8))
    } else throw new BSONException("Unexpected UUID representation")

  }
}
