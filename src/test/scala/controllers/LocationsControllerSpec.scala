import main.db.DbAdapterBase
import main.model.Location
import org.scalamock.scalatest.MockFactory
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

import scala.collection.mutable
import scala.collection.mutable.ArrayBuffer

class LocationsControllerSpec extends AnyWordSpec with Matchers with MockFactory {
  val aLocation = new Location(
    0,
    "Woodbridge"
  )
  val anotherLocation = new Location(
    2,
    "Reading"
  )
  val aFrenchLocation = new Location(
    3,
    "Le Vigan"
  )
  "LocationsController.retrieveAll" should {
    "fetch all locations" in {
      val mockDbAdapter = mock[DbAdapterBase]
      val mockDbLocationsLinkedHashMap = mutable.LinkedHashMap(
        "Europe" -> mutable.LinkedHashMap(
          "UK" -> Seq(aLocation, anotherLocation),
          "France" -> Seq(aFrenchLocation)
        )
      )
      val locationsController = new LocationsController(mockDbAdapter)

      (mockDbAdapter.getLocations _).expects().returns(mockDbLocationsLinkedHashMap)
      locationsController.retrieveAll() should equal(mockDbLocationsLinkedHashMap)
    }
  }
  "LocationsController.retrieveByContinent" should {
    "fetch all locations given a continent" in {
      val mockDbAdapter = mock[DbAdapterBase]
      val mockDbLocationsLinkedHashMap = mutable.LinkedHashMap(
        "Europe" -> mutable.LinkedHashMap(
          "UK" -> Seq(aLocation, anotherLocation),
          "France" -> Seq(aFrenchLocation)
        )
      )
      val locationsController = new LocationsController(mockDbAdapter)

      (mockDbAdapter.getLocations _).expects().returns(mockDbLocationsLinkedHashMap)
      locationsController.retrieveByContinent("Europe") should equal(ArrayBuffer("Woodbridge", "Reading", "Le Vigan"))
    }

  }
}