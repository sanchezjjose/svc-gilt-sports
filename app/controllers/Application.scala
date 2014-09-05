package controllers

import play.api.Play.current
import play.api.mvc._
import models._
import org.slf4j.LoggerFactory

object Application extends Controller with Config with Secured with Loggable {

  val logger = LoggerFactory.getLogger(getClass.getName)

  def index = Action {
    Redirect(routes.Application.home())
  }

  def home = IsAuthenticated { user => implicit request =>
    Ok(views.html.index("Next Game", Game.findNextGame))
  }

  def roster = IsAuthenticated { user => _ =>
    val players = User.findAll.toList.sortBy(u => u.firstName)

    Ok(views.html.roster(players))
  }

  def news = IsAuthenticated { user => _ =>
    Ok(views.html.news("News & Highlights"))
  }

  def updateScore(game_id: String, result: String, score: String) = Action {
    Game.updateScore(game_id, result, score)
    Redirect(routes.Schedule.schedule())
  }

  def updateRsvpStatus(game_id: String, status: String) = Action { implicit request =>
    val gameId = request.rawQueryString.split("=")(2).toInt
    val game : Option[Game] = Game.findByGameId(gameId)
    val userId = User.loggedInUser._id

    if(request.queryString.get("status").flatMap(_.headOption).get.contains("in")) {
      game.get.playersIn += userId
      game.get.playersOut -= userId
    } else {
      game.get.playersIn -= userId
      game.get.playersOut += userId
    }

    Game.update(game.get)

    Redirect(routes.Application.home())
  }
}

trait Config {
  val config = play.api.Play.configuration
}

object Config extends Config {
  lazy val msg = config.getString("msg").getOrElse("Remember to bring your game shirts. Let's get this W!")
  lazy val mongoUrl = config.getString("mongo_url").get
  lazy val environment = config.getString("environment").get
  lazy val fbAppId = config.getString("facebook_app_id").get
  lazy val fbAppSecret = config.getString("facebook_app_secret").get

  //TODO: should be entered together with new games via front-end (maybe a drop down menu)
  lazy val season = "Fall 2014"
}

object Environment {
  val DEVELOPMENT = "development"
  val PRODUCTION = "production"
}
