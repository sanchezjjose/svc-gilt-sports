package controllers

import com.sportify.config.Config
import models._
import play.api.data.Forms._
import play.api.data._
import play.api.mvc._
import utils.{Loggable, Helper}
import views._

object Login extends Controller with Loggable with Helper with Config {

  val loginForm = Form(
    tuple(
      "email" -> text,
      "password" -> text
    ) verifying ("Invalid email or password.", result => result  match {
      case (email: String, password: String) => User.authenticate(email, password).isDefined
    })
  )

  /**
   * Login page.
   */
  def login = Action { implicit request =>
    Ok(html.login(loginForm))
  }

  /**
   * Handle login form submission.
   */
  def authenticate = Action { implicit request =>
    loginForm.bindFromRequest.fold(
      formWithErrors => {
        Unauthorized(html.login(formWithErrors))
      },
      credentials => {
        implicit val user = User.findByEmail(credentials._1).get
        val defaultTeamId = buildTeamView.current._id
        Redirect(routes.Homepage.home(defaultTeamId)).flashing("team_id" -> s"$defaultTeamId").withSession("user_info" -> user.email)
      }
    )
  }

  /**
   * Logout and clean the session.
   */
  def logout = Action {
    Redirect(routes.Login.login).withNewSession.flashing(
      "success" -> "You've been successfully logged out."
    )
  }

}

/**
 * Provide security features
 */
trait Secured extends Loggable {
  
  /**
   * Retrieve the connected user session variable.
   */
  private def sessionKey(request: RequestHeader) = request.session.get("user_info")

  /**
   * Redirect to login if the user in not authorized.
   */
  private def onUnauthorized(request: RequestHeader) = Results.Redirect(routes.Login.login)
  
  /** 
   * Action for authenticated users.
   */
  def IsAuthenticated(f: => User => Request[AnyContent] => Result) = Security.Authenticated(sessionKey, onUnauthorized) { email =>

    // TODO: add to some sort of session to avoid hitting DB with each request
    User.findByEmail(email).map { user =>
      Action(request => f(user)(request))
    }.getOrElse {
      Action(request => onUnauthorized(request))
    }
  }
}