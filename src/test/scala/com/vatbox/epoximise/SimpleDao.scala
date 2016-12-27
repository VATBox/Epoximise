package com.vatbox.epoximise

import org.json4s.{DefaultFormats, Formats}
import org.mongodb.scala.result.DeleteResult
import org.mongodb.scala.{Completed, Document, MongoClient, MongoCollection, MongoDatabase, Observer}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{Future, Promise}

/**
  * Created by talg on 24/11/2016.
  */
object SimpleDao {
  implicit val formats: Formats = DefaultFormats + ObjectIdSerializer + LocalDateTimeSerializer(DefaultDateTimeFormatter) + DateSerializer + UUIDSerializer
  val mongoClient = MongoClient()
  val db: MongoDatabase = mongoClient.getDatabase("test_epoximise")
  val collection: MongoCollection[Document] = db.getCollection("objects")
  val epox: Epoximise = EpoximiseBuilder().build()
  import epox._

  def insert[A <: AnyRef](entity : A): Future[Completed] = {
    val promise = Promise[Completed]()
    /** {{{http://mongodb.github.io/mongo-scala-driver/1.2/bson/documents/#immutable-documents}}} */
    val observable = collection.insertOne(entity)
    observable.subscribe(new Observer[Completed] {
      override def onError(e: Throwable) = promise.failure(e)

      override def onComplete() = {} // basically do nothing

      override def onNext(result: Completed) = promise.trySuccess(result)
    })
    promise.future
  }

  def find[A: Manifest](): Future[Seq[A]] = {
    /**
        @note If the Observable is large then this will consume lots of memory!
          If the underlying Observable is infinite this Observable will never complete.
      */
    collection.find().toFuture().map( seq =>
      seq.map(_.extract[A])
    )
  }

  def clean(): Future[DeleteResult] = {
    collection.deleteMany(Document.empty).toFuture().map(_.head)
  }
}
