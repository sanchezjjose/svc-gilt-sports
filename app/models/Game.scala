package models


case class Game (
  _id: Long,
  home_team_id: Long,
  away_team_id: Option[Long],
  opponent: String,
  number: Int,
  start_time: Long,
  address: String,
  gym: String,
  location_details: Option[String],
  result: Option[String],
  players_in: Set[Long] = Set.empty[Long], // corresponds to player id's
  players_out: Set[Long] = Set.empty[Long], // corresponds to player id's
  is_playoff_game: Boolean = false
)

object GameFields {
  val Id = "_id"
  val HomeTeamId = "home_team_id"
  val AwayTeamId = "away_team_id"
  val Opponent = "opponent"
  val Number = "number"
  val StartTime = "start_time"
  val Address = "address"
  val Gym = "gym"
  val LocationDetails = "location_details"
  val Result = "result"
  val PlayersIn = "players_in"
  val PlayersOut = "players_out"
  val IsPlayoffGame = "is_playoff_game"
}
