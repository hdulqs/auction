package models

import tagging._

// tags used to prevent mix-ups of string-based IDs
object TaggedIds {
  trait SaleIdTag
  type SaleId = String @@ SaleIdTag
  trait LotIdTag
  type LotId = String @@ LotIdTag  

  // TODO rename GroupTag and SubgroupTag to better names
  trait IncrementPolicyIdTag
  type IncrementPolicyId = String @@ IncrementPolicyIdTag
  // awkward, but keeping the pattern consistent
  trait GroupTagTag
  type GroupTag = String @@ GroupTagTag
  trait SubgroupTagTag
  type SubgroupTag = String @@ SubgroupTagTag

  trait BidderIdTag
  type BidderId = String @@ BidderIdTag
  trait EventIdTag
  type EventId = String @@ EventIdTag
  trait PaddleNumberTag
  type PaddleNumber = String @@ PaddleNumberTag

  trait UserIdTag
  type UserId = String @@ UserIdTag

  trait CommandKeyTag
  type CommandKey = String @@ CommandKeyTag
}
