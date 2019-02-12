package models

import models.TaggedIds.{LotId, SaleId}

case class SaleState(
  saleId: SaleId,
  currentLotId: Option[LotId]
)
