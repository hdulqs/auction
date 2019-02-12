
import models.TaggedIds.SaleIdTag
import models.db.ParametersStore
import models.tagging._
import org.scalatest.concurrent.ScalaFutures
import org.scalatestplus.play._
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import testlibs.DB

// GuiceApplicationBuilder

class ParametersStoreSpec extends PlaySpec with GuiceOneAppPerSuite with ScalaFutures {
  import models._

  def repo: ParametersStore = app.injector.instanceOf(classOf[ParametersStore])
  implicit lazy val executionContext = app.injector.instanceOf[DatabaseExecutionContext]

  "Sale model" should {

    "be saved and retrieved by saleId" in {
      DB.withTestDatabase { db =>
        whenReady(repo.allSaleIds) { ids =>
          println(ids)
        }
        val saleId = "sale1".taggedWith[SaleIdTag]
        val sale = SaleState(saleId, None)
        whenReady(repo.createSaleState(sale)) { maybeId: Option[Long] =>

          whenReady(repo.stateForSale(saleId)) { maybeSale: Option[SaleState] =>
            val testSale = maybeSale.get
            testSale must equal(sale)
          }
        }
      }
    }
  }
}
