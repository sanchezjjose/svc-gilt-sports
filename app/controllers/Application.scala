package controllers

import play.api.mvc._

import models._

object Application extends Controller with Secured {

  def index = Action {
    Redirect(routes.Login.login)
  }

  def home = IsAuthenticated { user => _ =>
    Ok(views.html.index("Next Game"))
  }

  def schedule = IsAuthenticated { user => implicit request =>
    Ok(views.html.schedule("Winter 2013 Season", Game.findAll.toList)(user))
  }

  def roster = IsAuthenticated { user => _ =>
    Ok(views.html.roster("Gilt Unit"))
  }

  def news = IsAuthenticated { user => _ =>
    Ok(views.html.news("News & Highlights"))
  }
}
