package models

import TaggedIds._

case class SaleState(
  saleId: SaleId,
  currentLotId: Option[LotId]
)
