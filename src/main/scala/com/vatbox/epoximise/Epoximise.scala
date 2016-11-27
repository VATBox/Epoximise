package com.vatbox.epoximise

import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

import org.bson.BsonValue
import org.json4s.{Extraction, Formats, JValue}
import org.mongodb.scala.Document

/**
  * Created by talg on 24/11/2016.
  */
trait Epoximise { self =>
  protected val parser: EpoximiseParser
  protected val serializer: EpoximiseSerializer

  private[epoximise] implicit def convertToJvalue(any: Any)(implicit formats: Formats): JValue = Extraction.decompose(any)

  implicit def toBsonValue[A <: AnyRef](canBeJvalue: A)(implicit ev: A => JValue, formats: Formats): BsonValue = parser.parse(canBeJvalue)

  implicit def fromBsonValue[B](bson: B)(implicit e: B => Document, formats: Formats): JValue = serializer.serialize(bson.toBsonDocument)

  implicit def toBsonDocument[A <: AnyRef](any: A)(implicit ev: A => JValue, formats: Formats): Document = {
    val javaDoc = toBsonValue(any).asDocument()
    Document(javaDoc)
  }

  implicit class EpoximiseHelperFromBson(val document: Document) {
    def fromBsonValue(implicit formats: Formats): JValue = self.fromBsonValue(document)
    def extract[A: Manifest](implicit formats: Formats) : A = {
      Extraction.extract[A](fromBsonValue)
    }
    def extractOpt[A: Manifest](implicit formats: Formats) : Option[A] = {
      Extraction.extractOpt[A](fromBsonValue)
    }
  }
}

case class EpoximiseBuilder(
                             private val localDateTimeFormatter: DateTimeFormatter = DefaultDateTimeFormatter,
                             private val optimizeBigInt: Boolean = true,
                             private val zoneOffset: ZoneOffset = DefaultZoneOffSet
                           ) {
  def withLocalDateTimeFormatter(formatter: DateTimeFormatter): EpoximiseBuilder = copy(localDateTimeFormatter = formatter)

  /**
    * If it fits into Long/Int it will be created as one accordingly
    */
  def dontOptimizeBigInteger: EpoximiseBuilder = copy(optimizeBigInt = false)

  def withZoneOffSet(offset: ZoneOffset): EpoximiseBuilder = copy(zoneOffset = offset)

  def build() = new Epoximise {
    override protected val parser: EpoximiseParser = EpoximiseParser(localDateTimeFormatter, optimizeBigInt, zoneOffset)
    override protected val serializer: EpoximiseSerializer = EpoximiseSerializer(localDateTimeFormatter, zoneOffset)
  }
}