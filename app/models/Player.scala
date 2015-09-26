package models


case class PlayerViewModel (
  id: Long,
  name: String,
  number: Int,
  phoneNumber: Option[String],
  position: Option[String]
)


/**
* A player is a registered user who is a part of a team.
*/
case class Player (
  id: Long,
  user_id: Long,
  number: Int,
  position: Option[String]

) {

 /*
  * The overrides below are there to avoid the scenario where a player
  * updates an attribute that is not the id, and
  * is then able to be added to a mutable Set even if said player
  * is already in the Set.
  *
  * This may not be the most elegant, but it ensures a player is unique,
  * as long as it's id does not change.
  *
  * Maybe the better solution would be to only add the player id to the set,
  * rather than the entire player.
  */

 override def equals(o: Any) = {
   o match {
     case that: Player => this.id == that.id
     case _ => false
   }
 }

 override def hashCode = id.toInt
}

object PlayerFields {
  val Id = "id"
  val UserId = "user_id"
  val Number = "number"
  val Position = "position"
}
