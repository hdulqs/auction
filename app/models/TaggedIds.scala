package models

import models.tagging._

object TaggedIds {
  trait SaleIdTag
  type SaleId = String @@ SaleIdTag
  trait LotIdTag
  type LotId = String @@ LotIdTag  
}
