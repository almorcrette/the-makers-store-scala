import io.jvm.uuid.{StaticUUID, UUID}
import main.model.Item
import main.payment.{Payment, PaymentAdapterBase}
import org.scalamock.scalatest.MockFactory
import org.scalatest.{BeforeAndAfterAll, BeforeAndAfterEach}
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

import java.io.{ByteArrayOutputStream, PrintStream}
import scala.collection.mutable.ArrayBuffer

class CartSpec extends AnyWordSpec with Matchers with MockFactory with BeforeAndAfterEach with BeforeAndAfterAll {

  val mockUuidFactory = stub[FactoryBase[UUID]]
  val mockUuid = UUIDFactory.create
  (mockUuidFactory.create _).when().returns(mockUuid)

  val stubPaymentAdapter = stub[PaymentAdapterBase]

  val scoop = new Item(2, "Icecream scoop", 4.95, 1000, List("Europe"))
  val blender = new Item(3, "Blender", 44.50, 200, List("Europe", "NA"))
  val breadMaker = new Item(4, "Bread maker", 99.99, 50, List("NA"))
  val ghost = new Item(5, "Ghost", 20, 0, List("Europe"))
  val zombie = new Item(5, "Zombie", 20, 3, List("Europe"))
  val mockItemsInventory = ArrayBuffer(scoop, blender, breadMaker, ghost, zombie)
  val londonInventory = ArrayBuffer(scoop, blender, ghost, zombie)

  val mockItemsController = stub[ItemsController]

  val cart = new Cart("London", mockUuidFactory, mockItemsController, stubPaymentAdapter)

  override def beforeEach(): Unit = {
    cart.reset()
  }

  "Cart.getUUID" should {
    "return the cart's UUID" in {
      cart.getUUID() should equal (mockUuid)
    }
  }

  "Cart.addItem" should {
    "add an item to the cart if it is available in the cart location and the item is in stock" in {
      (mockItemsController.retrieveByLocation _).when("London").returns(londonInventory)
      cart.addItem("icecream scoop")
      cart.viewItems()("icecream scoop") should equal(1)
    }
    "add an item to the cart even if typed with different case (if available)" in {
      (mockItemsController.retrieveByLocation _).when("London").returns(londonInventory)
      cart.addItem("iceCREam scoop")
      cart.viewItems()("icecream scoop") should equal(1)
    }
    "add more than one of an item to the cart if it is available in the cart location and the item is in stock" in {
      (mockItemsController.retrieveByLocation _).when("London").returns(londonInventory)
      cart.addItem("Icecream scoop", 5)
      cart.viewItems()("icecream scoop") should equal(5)
    }
    "add more of an item already in the basket (if available)" in {
      (mockItemsController.retrieveByLocation _).when("London").returns(londonInventory)
      cart.addItem("Icecream scoop", 5)
      cart.addItem("Icecream scoop", 5)
      cart.viewItems()("icecream scoop") should equal(10)
    }
    "raises an error if the name of the item is not found in the inventory" in {
      (mockItemsController.retrieveByLocation _).when("London").returns(londonInventory)
      val stream = new ByteArrayOutputStream()
      Console.withOut(stream) {
        cart.addItem("Typo")
      }
      stream.toString() should include ("Item not found")
    }
    "raises an error if item is not available in the customer's location" in {
      (mockItemsController.retrieveByLocation _).when("London").returns(londonInventory)
      val stream = new ByteArrayOutputStream()
      Console.withOut(stream) {
        cart.addItem("Bread maker")
      }
      stream.toString() should include ("Item not found")
    }
    "print message 'not enough in stock' if item is out of stock in the customer's location" in {
      (mockItemsController.retrieveByLocation _).when("London").returns(londonInventory)
      val stream = new ByteArrayOutputStream()
      Console.withOut(stream) {
        cart.addItem("Ghost")
      }
      stream.toString() should include ("Not enough in stock")
    }
    "print message 'not enough in stock' if trying to add more of an item than is in stock" in {
      (mockItemsController.retrieveByLocation _).when("London").returns(londonInventory)
      val stream = new ByteArrayOutputStream()
      Console.withOut(stream) {
        cart.addItem("Zombie", 5)
      }
      stream.toString() should include ("Not enough in stock")
    }
    "print message 'not enough in stock if trying to add more of an item than is in stock and already in basket" in {
      (mockItemsController.retrieveByLocation _).when("London").returns(londonInventory)
      cart.addItem("Zombie", 3)

      val stream = new ByteArrayOutputStream()
      Console.withOut(stream) {
        cart.addItem("Zombie", 1)
      }
      stream.toString() should include ("Not enough in stock")
    }
  }
  "Cart.reset" should {
    "empty the cart" in {
      (mockItemsController.retrieveByLocation _).when("London").returns(londonInventory)
      cart.addItem("icecream scoop")
      cart.reset()
      cart.viewItems() should equal(Map())

    }
  }
  "Cart.changeAmount" should {
    "increase the amount of an item in the cart by 1 (regardless of how the item is typed)" in {
      (mockItemsController.retrieveByLocation _).when("London").returns(londonInventory)
      cart.addItem("icecream scoop")
      cart.changeAmount("iceCreaM ScOOP", 1)
      cart.viewItems()("icecream scoop") should equal(2)
    }
    "decrease the amount of an item in the cart by 1 (regardless of how the item is typed)" in {
      (mockItemsController.retrieveByLocation _).when("London").returns(londonInventory)
      cart.addItem("icecream scoop")
      cart.changeAmount("iceCreaM ScOOP", 1, "-")
      cart.viewItems()("icecream scoop") should equal(0)
    }
    "increase the amount of an item in the cart by more than 1 (regardless of how the item is typed)" in {
      (mockItemsController.retrieveByLocation _).when("London").returns(londonInventory)
      cart.addItem("icecream scoop")
      cart.changeAmount("iceCreaM ScOOP", 3)
      cart.viewItems()("icecream scoop") should equal(4)
    }
    "decrease the amount of an item in the cart by more than 1 (regardless of how the item is typed)" in {
      (mockItemsController.retrieveByLocation _).when("London").returns(londonInventory)
      cart.addItem("icecream scoop", 3)
      cart.changeAmount("iceCreaM ScOOP", 3, "-")
      cart.viewItems()("icecream scoop") should equal(0)
    }
    "raise an error if the item is not found in the cart" in {
      (mockItemsController.retrieveByLocation _).when("London").returns(londonInventory)
      val thrown = the [Exception] thrownBy {
        cart.changeAmount("rubbish", 1)
      }
      thrown.getMessage should equal ("Item not in cart")
    }
    "raise an error if attempt to increase amount of item beyond what's available in inventory" in {
      (mockItemsController.retrieveByLocation _).when("London").returns(londonInventory)
      cart.addItem("zombie", 3)

      val stream = new ByteArrayOutputStream()
      Console.withOut(stream) {
        cart.changeAmount("zombie", 1)
      }
      stream.toString() should include ("Not enough in stock")
    }
    "raise an error if attempt to decrease amount of item beyond what's in the cart" in {
      (mockItemsController.retrieveByLocation _).when("London").returns(londonInventory)
      cart.addItem("icecream scoop", 3)
      val stream = new ByteArrayOutputStream()
      Console.withOut(stream) {
        cart.changeAmount("icecream scoop", 4, "-")
      }
      stream.toString() should include ("Not enough in cart")
    }
  }
  "cart.onPaymentSuccess" should {
    "updates the inventory to subtract the recently sold stock when the cart has one item (an icecream scoop)" in {
      val mockAnotherItemsController = mock[ItemsController]

      val anotherCart = new Cart("London", mockUuidFactory, mockAnotherItemsController)


      (mockAnotherItemsController.retrieveByLocation _).expects("London").returns(londonInventory)
      anotherCart.addItem("icecream scoop")

      (mockAnotherItemsController.retrieveByName _).expects("icecream scoop").returns(scoop)
      (mockAnotherItemsController.update _).expects(
        scoop.id,
        None,
        None,
        Some(scoop.quantity - 1),
        None
      )

      anotherCart.onPaymentSuccess()
    }
    "updates the inventory to subtract the recently sold stock when the cart has one item (a blender)" in {
      val mockAnotherItemsController = mock[ItemsController]

      val anotherCart = new Cart("London", mockUuidFactory, mockAnotherItemsController)

      (mockAnotherItemsController.retrieveByLocation _).expects("London").returns(londonInventory)
      anotherCart.addItem("Blender")

      (mockAnotherItemsController.retrieveByName _).expects("blender").returns(blender)
      (mockAnotherItemsController.update _).expects(
        blender.id,
        None,
        None,
        Some(blender.quantity - 1),
        None
      )

      anotherCart.onPaymentSuccess()
    }
    "updates the inventory to subtract the recently sold stock when the cart has multiple different items" in {
      val mockAnotherItemsController = mock[ItemsController]

      val anotherCart = new Cart("London", mockUuidFactory, mockAnotherItemsController)


      (mockAnotherItemsController.retrieveByLocation _).expects("London").anyNumberOfTimes.returns(londonInventory)
      anotherCart.addItem("Blender")
      anotherCart.addItem("icecream scoop", 2)


      (mockAnotherItemsController.retrieveByName _).expects("blender").returns(blender)
      (mockAnotherItemsController.retrieveByName _).expects("icecream scoop").returns(scoop)

      (mockAnotherItemsController.update _).expects(
        blender.id,
        None,
        None,
        Some(blender.quantity - 1),
        None
      )

      (mockAnotherItemsController.update _).expects(
        scoop.id,
        None,
        None,
        Some(scoop.quantity - 2),
        None
      )

      anotherCart.onPaymentSuccess()
    }
    "clear the cart" in {
      val mockAnotherItemsController = mock[ItemsController]

      val anotherCart = new Cart("London", mockUuidFactory, mockAnotherItemsController)


      (mockAnotherItemsController.retrieveByLocation _).expects("London").returns(londonInventory)
      anotherCart.addItem("icecream scoop")

      (mockAnotherItemsController.retrieveByName _).expects("icecream scoop").returns(scoop)
      (mockAnotherItemsController.update _).expects(
        scoop.id,
        None,
        None,
        Some(scoop.quantity - 1),
        None
      )

      anotherCart.onPaymentSuccess()

      anotherCart.viewItems() should equal(Map())
    }
  }
  "cart.onPaymentFailed" should {
    "clear the cart" in {
      (mockItemsController.retrieveByLocation _).when("London").returns(londonInventory)

      cart.addItem("icecream scoop")
      cart.onPaymentFailed()
      cart.viewItems() should equal(Map())

    }
  }
  "cart.checkout" should {
    "call the payment adapter" which {
      "passes onPaymentSucess and onPaymentFailed methods as function arguments" in {
        val mockPaymentAdapter = mock[PaymentAdapterBase]

        val cartWithMockPayment = new Cart("London", mockUuidFactory, mockItemsController, mockPaymentAdapter)

        (mockItemsController.retrieveByLocation _).when("London").returns(londonInventory)

        cartWithMockPayment.addItem("icecream scoop")

        val onSuccess = cartWithMockPayment.onPaymentSuccess _
        val onFailure = cartWithMockPayment.onPaymentFailed _

        (mockItemsController.retrieveByName _).when("icecream scoop").returns(
          new Item(2, "Icecream scoop", 4.95, 999, List("Europe"))
        )

        (mockPaymentAdapter.makePayment _).expects(
          4.95,
          *, // problem: not testing that onSuccess is passed as an argument
          * // problem: not testing that onFailure is passed as an argument
        )

        cartWithMockPayment.checkout()
      }
      "passes the total price of the items in the cart" in {
        val mockPaymentAdapter = mock[PaymentAdapterBase]

        val cartWithMockPayment = new Cart("London", mockUuidFactory, mockItemsController, mockPaymentAdapter)

        (mockItemsController.retrieveByLocation _).when("London").returns(londonInventory)

        cartWithMockPayment.addItem("icecream scoop")
        cartWithMockPayment.addItem("icecream scoop")
        cartWithMockPayment.addItem("blender")

        (mockItemsController.retrieveByName _).when("icecream scoop").returns(
          new Item(2, "Icecream scoop", 4.95, 998, List("Europe"))
        )

        (mockItemsController.retrieveByName _).when("blender").returns(
          new Item(3, "Blender", 44.50, 199, List("Europe", "NA"))
        )

        (mockPaymentAdapter.makePayment _).expects(
          54.4,
          *,  // problem: not testing that onSuccess is passed as an argument
          *  // problem: not testing that onSuccess is passed as an argument
        )
        cartWithMockPayment.checkout()
      }
    }
  }
}