package controllers

import play.api.mvc._
import play.api.libs.json._
import models.{User, Game}

object Endpoints extends Controller with Config with Secured with Loggable {

  def games = Action {
    Ok(Json.toJson(Game.findAll.toList))
  }

  def game(id: Long) = Action {
    Game.findByGameId(id).map { game =>
      Ok(Json.toJson(game))
    }.getOrElse(NotFound("Game Not Found"))
  }

  def nextgame = Action {
    Game.findNextGame.map { game =>
      Ok(Json.toJson(game))
    }.getOrElse(NotFound("Game Not Found"))
  }

  def users = Action { implicit request =>
    request.getQueryString("email") match {
      case Some(email) => Ok(Json.toJson(User.findByEmail(email)))
      case _ => Ok(Json.toJson(User.findAll.toList))
    }
  }
}