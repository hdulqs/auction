package models

import models.TaggedIds.{BidderId, SaleId, UserId}
import tagging._

/**
  * The credentials for client API access.
  *
  * @param role the role for the session.
  * @param userIdOpt the user ID, for bidder and operator access. Might be
  *                  omitted where other systems need to access Causality.
  * @param saleIdOpt the sale these credentials apply to. Might be omitted for
  *                  endpoints that do not refer to an existing sale.
  * @param bidderIdOpt the bidder ID, in the case that the user is a bidder.
  *                    Omitted for operator and automatic access.
  */
case class UserCredentials(
                            role: UserCredentials.AuthorizationRole,
                            userIdOpt: Option[UserId],
                            saleIdOpt: Option[SaleId],
                            bidderIdOpt: Option[BidderId]
                          ) {
  import UserCredentials._

  val isOperator: Boolean = role == AdministratorRole || role == OperatorRole
}

object UserCredentials {

  sealed abstract class AuthorizationRole(val name: String)
    extends Serializable {
    override def toString: String = name
  }

  /** The role of authorized systems, like Gravity. */
  case object AdministratorRole extends AuthorizationRole("admin")

  /** A user authorized to interface with the room in a live sale. */
  case object OperatorRole extends AuthorizationRole("operator")

  /** A user authorized to operate, but not access user data. */
  case object ExternalOperatorRole extends AuthorizationRole("externalOperator")

  /** A user registered and approved to bid. */
  case object BidderRole extends AuthorizationRole("bidder")

  /** A user not approved to bid, who may or may not be logged into Artsy. */
  case object ObserverRole extends AuthorizationRole("observer")

  object AuthorizationRole {

    def byName(name: String): Option[AuthorizationRole] = name match {
      case "admin"            => Some(AdministratorRole)
      case "operator"         => Some(OperatorRole)
      case "externalOperator" => Some(ExternalOperatorRole)
      case "bidder"           => Some(BidderRole)
      case "observer"         => Some(ObserverRole)
      case _                  => None
    }
  }
}

/**
  * Used to define privilege levels for data. Authorization service then
  * determines what a user with given credentials will receive (usually based
  * on role).
  */
sealed trait PrivilegeLevel
object PrivilegeLevel {
  case object ArtsyOperatorOnly extends PrivilegeLevel
  case object OperatorOnly      extends PrivilegeLevel
  case object Public            extends PrivilegeLevel
}