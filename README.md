# Epoximise - library to serialize/deserialize MongoDB (ver 3+) **`BsonValue`**s 
[![CircleCI](https://circleci.com/gh/VATBox/Epoximise.svg?style=svg)](https://circleci.com/gh/VATBox/Epoximise)

## Example is the best way to explain what it is.
 * Let's create 2 scala classes first, they are pretty generic:
    ```scala
    case class UpperLevel(
                           i: Int,
                           b: Boolean,
                           s: String,
                           o: Option[Boolean],
                           d: Double,
                           l: Long,
                           objectId: ObjectId,
                           uuid: UUID,
                           set: Set[Embedded]
                         )
    case class Embedded(ii: Int, bb: Boolean, arr: List[Int], lclDate: LocalDateTime)
    ```
 * Then we will create some __random__ objects using [scalacheck](https://www.scalacheck.org) generators:
    ```scala
      def upperLevelGen: Gen[UpperLevel] = for{
        i <- Arbitrary.arbInt.arbitrary
        b <- Arbitrary.arbBool.arbitrary
        s <- Gen.listOfN(10,Gen.alphaNumChar).map(_.mkString)
        o <- Gen.option(Arbitrary.arbBool.arbitrary)
        d <- Arbitrary.arbDouble.arbitrary
        l <- Arbitrary.arbLong.arbitrary
        uuid <- Gen.uuid
        set <- Gen.choose(0,5).flatMap(size => Gen.listOfN(size,embeddedGen)).map(_.toSet)
      } yield UpperLevel(i, b, s, o, d, l, ObjectId.get(), uuid, set)
    
      def embeddedGen : Gen[Embedded] = for {
        ii <- Arbitrary.arbInt.arbitrary
        bb <- Arbitrary.arbBool.arbitrary
        arr <- Gen.choose(0,10).flatMap(size => Gen.listOfN(size, Arbitrary.arbInt.arbitrary))
        lDate <- Gen.choose(10000,100000).map { sec =>
          val time = LocalDateTime.now().minusSeconds(sec)
          val nanos = time.getNano
          time.minusNanos(nanos)
        }
      } yield Embedded(ii, bb, arr, lDate)
    ```
 * Now that we have our objects let's persist them to MongoDB using [MongoDB scala driver](https://docs.mongodb.com/ecosystem/drivers/scala/). 
 For that we will create a really simple DAO with the following methods:
    - Insert __ANY__(almost) object = `def insert[A <: AnyRef](entity : A): Future[Completed]`
    - Find All = `def find[A: Manifest](): Future[Seq[A]]`
 
    This is the part where Epoximise comes to the rescue and this is what you need to add:
    ```scala
    val epox: Epoximise = EpoximiseBuilder().build()
    import epox._
    ```
    That's it.
 * This is how our DAO looks like (full example [here](/src/test/scala/com/vatbox/epoximise/OneSpecToRuleThemAll.scala)):
    ```scala
    object SimpleDao {
      val collection: MongoCollection[Document] = db.getCollection("objects")
      val epox: Epoximise = EpoximiseBuilder().build()
      import epox._
    
      def insert[A <: AnyRef](entity : A): Future[Completed] = {
        val promise = Promise[Completed]()
        /**
           entity is implicitly converted to MongoDB Document
           {{{http://mongodb.github.io/mongo-scala-driver/1.2/bson/documents/#immutable-documents}}} 
        */
        val observable = collection.insertOne(entity) 
        observable.subscribe(new Observer[Completed] {
          override def onError(e: Throwable) = promise.failure(e)
    
          override def onComplete() = {} // basically do nothing
    
          override def onNext(result: Completed) = promise.trySuccess(result)
        })
        promise.future
      }
    
      def find[A: Manifest](): Future[Seq[A]] = {
        collection.find().toFuture().map( seq =>
          seq.map(_.extract[A])
        )
      }
    }
    ```
 * Now what's left for us is to use it:
    ```scala
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
    }
    ```
 * This is what is looks like in MongoDB:
    ```javascript
    db.objects.findOne()
    {
    	"_id" : ObjectId("586212cca36dbdbcee840c8d"),
    	"i" : 0,
    	"b" : false,
    	"s" : "qncavbapct",
    	"d" : 1.8429734143155635e+270,
    	"l" : -1,
    	"objectId" : ObjectId("586212cca36dbdbcee840c8c"),
    	"uuid" : BinData(4,"XqHqQgSgTHyubkEfZtGlog=="),
    	"set" : [
    		{
    			"ii" : 165273565,
    			"bb" : true,
    			"arr" : [
    				-2147483648,
    				-1,
    				-1422343589,
    				-329538388,
    				-109958632,
    				-1753241949
    			],
    			"lclDate" : ISODate("2016-12-26T17:21:33Z")
    		},
    		{
    			"ii" : -2147483648,
    			"bb" : false,
    			"arr" : [
    				-1730771104,
    				0,
    				192933199,
    				-1499973888,
    				-1290175961,
    				2147483647,
    				-2147483648,
    				-1,
    				-1688550900
    			],
    			"lclDate" : ISODate("2016-12-26T17:44:52Z")
    		},
    		{
    			"ii" : 0,
    			"bb" : true,
    			"arr" : [
    				-1,
    				-584284726,
    				-2147483648,
    				1,
    				-1547982927,
    				-2147483648,
    				1600623380
    			],
    			"lclDate" : ISODate("2016-12-26T07:15:51Z")
    		},
    		{
    			"ii" : -1,
    			"bb" : false,
    			"arr" : [
    				0,
    				-2147483648,
    				0,
    				2114507037,
    				-2147483648,
    				0,
    				-1938015543,
    				0
    			],
    			"lclDate" : ISODate("2016-12-26T10:06:59Z")
    		},
    		{
    			"ii" : 367712630,
    			"bb" : true,
    			"arr" : [
    				2147483647,
    				2147483647,
    				-2038381930
    			],
    			"lclDate" : ISODate("2016-12-26T10:28:07Z")
    		}
    	]
    }
    ```