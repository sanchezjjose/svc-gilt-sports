package controllers

import play.api.mvc._
import models._
import play.api.libs.json._
import play.api.data._
import play.api.data.Forms._
import play.api.data.Forms.text
import org.joda.time.DateTime
import utils.{RequestHelper, Loggable, Helper}


object Schedule extends Controller
  with Helper
  with RequestHelper
  with Loggable
  with Secured {

  def schedule(teamId: Long) = IsAuthenticated { implicit user => implicit request =>
    withScheduleContext(request, user, teamId) { (scheduleView: ScheduleView) =>
      render {
        case Accepts.Html() => Ok(views.html.schedule(gameForm, scheduleView.currentSeason, scheduleView.nextGame, scheduleView.games, buildTeamView(teamId)))
        case Accepts.Json() => Ok(Json.toJson(scheduleView))
      }
    }
  }

	def submit(teamId: Long) = IsAuthenticated { implicit user => implicit request =>
    val gameId = request.rawQueryString.split("=")(2).toInt
    val game: Option[Game] = Game.findById(gameId)
    val playerId = buildPlayerView(teamId).id


    if (request.queryString.get("status").flatMap(_.headOption).get.contains("in")) {
      val updatedGame = game.get.copy(
        players_in = game.get.players_in + playerId,
        players_out = game.get.players_out - playerId
      )

      Game.update(updatedGame)

      Ok(Json.toJson(
        Map(
          "status" -> Json.toJson("in"),
          "msg" -> Json.toJson("You are playing. See you there!")
        )
      ))

    } else {
      val updatedGame = game.get.copy(
        players_in = game.get.players_in - playerId,
        players_out = game.get.players_out + playerId
      )

      Game.update(updatedGame)

      Ok(Json.toJson(
        Map(
          "status" -> Json.toJson("out"),
          "msg" -> Json.toJson("Ok, you are not playing in this game. Maybe next time!")
        )
      ))
    }
	}

  // TODO: move to Game controller
  val gameForm: Form[GameForm] = Form(
    mapping(
      "number" -> optional(text),
      "start_time" -> text,
      "address" -> text,
      "gym" -> text,
      "location_details" -> optional(text),
      "opponent" -> text,
      "result" -> optional(text)
    ) { (number, startTime, address, gym, locationDetails, opponent, result) =>

      GameForm(number,
        startTime,
        address,
        gym,
        locationDetails,
        opponent,
        result)

    } { (game: GameForm) =>

      Some((game.number,
        game.startTime,
        game.address,
        game.gym,
        game.locationDetails,
        game.opponent,
        game.result))
    }
  )

  // TODO: move to Game controller
  def changeRsvpStatus(teamId: Long, game_id: Long, status: String) = IsAuthenticated { implicit user => implicit request =>
    val game = Game.findById(game_id).get
    val playerId = buildPlayerView(teamId).id

    val updatedGame = if (status == "in") {
      game.copy(
        players_in = game.players_in + playerId,
        players_out = game.players_out - playerId
      )

    } else {
      game.copy(
        players_in = game.players_in - playerId,
        players_out = game.players_out + playerId
      )
    }

    Game.update(updatedGame)

    Redirect(routes.Homepage.home(buildTeamView(teamId).current._id))
  }

  // TODO: move to Game controller
  def save(teamId: Long, seasonId: Long, isPlayoffGame: String) = IsAuthenticated { implicit user => implicit request =>
    gameForm.bindFromRequest.fold(
      errors => {
        log.error("There was a problem adding a new game", errors)

        Redirect(routes.Schedule.schedule(teamId)).flashing(
        "failure" -> "There was a problem with adding a new game."
      )},

      gameForm => {

        try {
          // Ensure date format was correct
          DateTime.parse(gameForm.startTime, Game.gameDateFormat)

          Season.findById(seasonId).map { season =>

            val newGame = gameForm.toNewGame(seasonId, isPlayoffGame.toBoolean)
            Game.create(newGame)

            // Add game to season and update
            val updatedSeason = season.copy(game_ids = season.game_ids + newGame._id)

            Season.update(updatedSeason)

            // Also add the game to opponent's season if opponent has a team
            Team.findByName(gameForm.opponent).map { opponentTeam =>
              opponentTeam.season_ids.map { opponentSeasonId =>
                Season.findById(opponentSeasonId).find(season => season.is_current_season).map { opponentSeason =>
                  val tVm = buildTeamView(teamId)
                  val oppNewGame = gameForm.toNewGame(opponentSeasonId, isPlayoffGame.toBoolean).copy(opponent = tVm.current.name)
                  val updatedSeason = opponentSeason.copy(game_ids = opponentSeason.game_ids + oppNewGame._id)

                  Game.create(oppNewGame)
                  Season.update(updatedSeason)
                }
              }
            }
          }

          Redirect(routes.Schedule.schedule(teamId))
        } catch {
          case e: Exception => {
            log.error("There was a problem with adding a new game", e)

            Redirect(routes.Schedule.schedule(teamId)).flashing(
              "failure" -> "There was a problem with adding a new game. Make sure the date format is correct."
            )
          }
        }
      }
    )
  }

  // TODO: move to Game controller
  def edit(teamId: Long, gameId: Long) = IsAuthenticated { implicit user => implicit request =>
   val game = Game.findById(gameId).get

    Ok(Json.toJson(
      Map(
        "team_id" -> Json.toJson(teamId),
        "game_id" -> Json.toJson(gameId),
        "number" -> Json.toJson(game.number),
        "start_time" -> Json.toJson(game.start_time),
        "address" -> Json.toJson(game.address),
        "gym" -> Json.toJson(game.gym),
        "location_details" -> Json.toJson(game.location_details),
        "opponent" -> Json.toJson(game.opponent),
        "result" -> Json.toJson(game.result)
      )
    ))
  }

  // TODO: move to Game controller
  def update(teamId: Long, gameId: Long, isPlayoffGame: String) = IsAuthenticated { implicit user => implicit request =>
    gameForm.bindFromRequest.fold(
      errors => {
        log.error(errors.toString)

        Redirect(routes.Schedule.schedule(teamId)).flashing(
          "failure" -> "There was a problem with adding a new game."
        )},

      gameForm => {

        try {
          // Validate date format was correct
          DateTime.parse(gameForm.startTime, Game.gameDateFormat)
          val game = gameForm.toGame(gameId, isPlayoffGame.toBoolean)

          Game.update(game)

          Redirect(routes.Schedule.schedule(teamId))
        } catch {
          case e: Exception => {
            log.error("There was a problem with adding a new game", e)

            Redirect(routes.Schedule.schedule(teamId)).flashing(
              "failure" -> "There was a problem with adding a new game. Make sure the date format is correct."
            )
          }
        }
      }
    )
  }

  // TODO: move to Game controller
  def delete(teamId: Long, seasonId: Long, gameId: Long) = IsAuthenticated { user => implicit request =>
    val season = Season.findById(seasonId).get
    val updatedSeason = season.copy(game_ids = season.game_ids - gameId)

    Season.update(updatedSeason)

    // remove game
    Game.remove(gameId)

    Redirect(routes.Schedule.schedule(teamId))
  }
}
