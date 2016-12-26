package com.vatbox.epoximise

import org.scalatest._
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.prop.GeneratorDrivenPropertyChecks
import org.scalatest.time.{Millis, Seconds, Span}

import scala.collection.mutable

/**
  * Created by talg on 24/11/2016.
  */
class OneSpecToRuleThemAll extends AsyncFreeSpecLike with ScalaFutures with GeneratorDrivenPropertyChecks with Matchers with BeforeAndAfterAll{
  val dao = SimpleDao
  override implicit val patienceConfig: PatienceConfig = PatienceConfig(Span(10,Seconds),Span(100, Millis))

  "Generate random objects and insert them to DB" in {
    var buffer = mutable.Buffer[UpperLevel]()
    val res = forAll(Generators.upperLevelGen,MinSuccessful(100)) { obj =>
      val future = dao.insert(obj)
      future.map { completed =>
        buffer += obj
      }
    }

    dao.find[UpperLevel]().map { seq =>
      seq should contain theSameElementsAs buffer
    }
  }

  override protected def beforeAll(): Unit = {
    dao.clean().futureValue
  }
}
