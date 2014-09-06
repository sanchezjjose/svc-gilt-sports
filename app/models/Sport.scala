package models


/**
 * Model for the currently supported sports.
 */
case class Sport(_id: Long,
                 name: String)

object Sport {

  import play.api.libs.functional.syntax._
  import play.api.libs.json._

  implicit val sportWrites: Writes[Sport] = (
    (JsPath \ "_id").write[Long] and
    (JsPath \ "name").write[String]
  )(unlift(Sport.unapply))


//  object Name extends Enumeration {
//    type Name = Value
//
//    val Basketball, Baseball, Softball = Value
//  }
}
