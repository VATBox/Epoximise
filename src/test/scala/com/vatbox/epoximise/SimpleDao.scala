package com.vatbox.epoximise

import org.json4s.{DefaultFormats, Formats}
import org.mongodb.scala._
import org.mongodb.scala.result.DeleteResult

import scala.concurrent.Future

/**
  * Created by talg on 24/11/2016.
  */
object SimpleDao {
  implicit val formats: Formats = DefaultFormats + ObjectIdSerializer + LocalDateTimeSerializer() + LocalDateSerializer() + DateSerializer + UUIDSerializer
  val mongoClient = MongoClient()
  val db: MongoDatabase = mongoClient.getDatabase("test_epoximise")
  val collection: MongoCollection[Document] = db.getCollection("objects")
  val epox: Epoximise = EpoximiseBuilder().build()
  import epox._

  def insert[A <: AnyRef](entity : A): Future[Completed] = collection.insertOne(entity).head()

  def find[A: Manifest](): Future[Seq[A]] = {
    /**
        @note If the Observable is large then this will consume lots of memory!
          If the underlying Observable is infinite this Observable will never complete.
      */
    collection.find().map(_.extract[A]).toFuture()
  }

  def clean(): Future[DeleteResult] = collection.deleteMany(Document.empty).head()
}
