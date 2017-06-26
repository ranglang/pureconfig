package pureconfig

import com.typesafe.config.{ ConfigFactory, ConfigObject, ConfigValueFactory }
import org.scalacheck.{ Arbitrary, Gen }
import pureconfig.error._

class CoproductConvertersSuite extends BaseSuite {

  behavior of "ConfigConvert"

  val genBirdConfig: Gen[BirdConfig] = Arbitrary.arbBool.arbitrary.map(BirdConfig.apply)
  val genCatConfig: Gen[CatConfig] = Arbitrary.arbInt.arbitrary.map(CatConfig.apply)
  val genDogConfig: Gen[DogConfig] = Arbitrary.arbInt.arbitrary.map(DogConfig.apply)
  val genAnimalConfig: Gen[AnimalConfig] = Gen.oneOf(genBirdConfig, genCatConfig, genDogConfig)
  implicit val arbAnimalConfig = Arbitrary(genAnimalConfig)

  checkArbitrary[AnimalConfig]

  it should "read disambiguation information on sealed families by default" in {
    val conf = ConfigFactory.parseString("{ type = dogconfig, age = 2 }")
    ConfigConvert[AnimalConfig].from(conf.root()) shouldEqual Right(DogConfig(2))
  }

  it should "read disambiguation information on sealed families with the cases nested in the companion" in {
    import CarMaker._
    val conf = ConfigFactory.parseString("{ type = bmw }")
    ConfigConvert[CarMaker].from(conf.root()) shouldEqual Right(BMW)
  }

  it should "write disambiguation information on sealed families by default" in {
    val conf = ConfigConvert[AnimalConfig].to(DogConfig(2))
    conf shouldBe a[ConfigObject]
    conf.asInstanceOf[ConfigObject].get("type") shouldEqual ConfigValueFactory.fromAnyRef("dogconfig")
  }

  it should "return a proper ConfigReaderFailure if the hint field in a coproduct is missing" in {
    val conf = ConfigFactory.parseString("{ can-fly = true }")
    val failures = ConfigConvert[AnimalConfig].from(conf.root()).left.value.toList
    failures should have size 1
    failures.head shouldBe a[KeyNotFound]
  }

  it should "return a proper ConfigReaderFailure when a coproduct config is missing" in {
    case class AnimalCage(animal: AnimalConfig)
    val failures = ConfigConvert[AnimalCage].from(ConfigFactory.empty().root()).left.value.toList
    failures should have size 1
    failures.head shouldBe a[KeyNotFound]
  }
}

sealed trait CarMaker

object CarMaker {
  case object Mercedes extends CarMaker
  case object BMW extends CarMaker
  case object Tesla extends CarMaker
}
