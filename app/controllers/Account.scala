package controllers

import javax.inject.Inject
import api.MongoManager
import models._
import models.JsonFormats._
import play.api.data.Forms._
import play.api.data._
import play.api.libs.json.Json
import play.api.mvc._
import play.modules.reactivemongo.{MongoController, ReactiveMongoApi, ReactiveMongoComponents}
import reactivemongo.bson.BSONDocument
import util.RequestHelper
import scala.concurrent.{ExecutionContext, Future}
import ExecutionContext.Implicits.global


class Account @Inject() (val reactiveMongoApi: ReactiveMongoApi)
  extends Controller with MongoController with ReactiveMongoComponents with RequestHelper {

  override val db = new MongoManager(reactiveMongoApi)

  private[controllers] case class AccountForm (
    email: String,
    password: Option[String],
    firstName: String,
    lastName: String,
    number: Int,
    phoneNumber: Option[String],
    position: Option[String],
    isAdmin: Boolean
  )

  val userForm: Form[AccountForm] = Form(
    mapping(
      "email" -> email,
      "password" -> optional(text),
      "first_name" -> text,
      "last_name" -> text,
      "number" -> number,
      "phone_number" -> optional(text),
      "position" -> optional(nonEmptyText),
      "is_admin" -> boolean
    ) {
      // Data Binding
      (email, password, firstName, lastName, number, phoneNumber, position, isAdmin) =>
        AccountForm(email, password, firstName, lastName, number, phoneNumber, position, isAdmin)
    } {
      // Data Unbinding
      userForm =>
        Some((userForm.email, userForm.password, userForm.firstName, userForm.lastName,
          userForm.number, userForm.phoneNumber, userForm.position, userForm.isAdmin))
    }
  )

  def account(teamId: Long) = isAuthenticatedAsync { user => implicit request =>
    withAccountContext(request, user, teamId) { (accountView: AccountView, playerViewModel: PlayerViewModel) =>
      Ok(Json.toJson(accountView))
    }
  }

  def delete = isAuthenticatedAsync { user => implicit request =>
    db.userDb.remove(Json.obj(UserFields.Id -> user._id)).map { _ =>
      NoContent
    }
  }

  def submit(teamId: Long) = isAuthenticatedAsync { user => implicit request =>
    userForm.bindFromRequest.fold(
      errors => Future successful BadRequest("An error has occurred"),

      userFormData => {

        withAccountContext(request, user, teamId) { (accountView: AccountView, playerViewModel: PlayerViewModel) =>

          db.userDb.update(
            Json.obj(UserFields.Id -> user._id, PlayerFields.Id -> playerViewModel.id),
            Json.obj("$set" -> Json.obj(
              UserFields.Email -> userFormData.email,
              UserFields.Password -> userFormData.password,
              UserFields.FirstName -> userFormData.firstName,
              UserFields.LastName -> userFormData.lastName,
              UserFields.PhoneNumber -> userFormData.phoneNumber
            ))
          )

          db.playerDb.update(
            BSONDocument(PlayerFields.Id -> playerViewModel.id),
            BSONDocument("$set" -> BSONDocument(
              PlayerFields.Position -> userFormData.position,
              PlayerFields.Number -> userFormData.number
            ))
          )

          Redirect(routes.Account.account(teamId)).flashing(
            "success" -> "Your account information has been successfully updated."
          )
        }
      }
    )
  }
}