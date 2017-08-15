package interview.app.transport

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import interview.app.transport.protocol._
import spray.json._

trait JsonSupport extends SprayJsonSupport with DefaultJsonProtocol {

  import InMessage._
  import OutMessage._

  implicit object InMessageTypeFormat extends JsonFormat[InMessageType] {
    override def read(json: JsValue): InMessageType = json match {
      case JsString(value) => InMessageType(value).getOrElse(throw new DeserializationException("Unknown input message type: " + json.toString))
      case _ => throw new DeserializationException("Unable to read input message type: " + json.toString)
    }

    override def write(obj: InMessageType): JsValue = JsString(obj.t)
  }

  implicit object OutMessageTypeFormat extends JsonFormat[OutMessageType] {
    override def read(json: JsValue): OutMessageType = json match {
      case JsString(value) => OutMessageType(value).getOrElse(throw new DeserializationException("Unknown output message type: " + json.toString))
      case _ => throw new DeserializationException("Unable to read output message type: " + json.toString)
    }

    override def write(obj: OutMessageType): JsValue = JsString(obj.t)
  }

  implicit object InMessageReader extends JsonReader[InMessage] {
    override def read(value: JsValue): InMessage =
      value.asJsObject
        .getFields("$type")
        .headOption
        .map(t => new InMessage(InMessageTypeFormat.read(t)){})
        .getOrElse(throw new DeserializationException(("Message $type not found")))
  }

  implicit object UserTypeFormat extends JsonFormat[UserType] {
    override def write(obj: UserType): JsValue = JsString(obj.t)

    override def read(json: JsValue): UserType = json match {
      case JsString(value) => UserType(value).getOrElse(throw new DeserializationException("Unknown user type: " + json.toString))
      case _ => throw new DeserializationException("Unable to read user type:" + json.toString)
    }
  }

  implicit object ErrorResponseWriter extends JsonWriter[ErrorResponse] {
    override def write(obj: ErrorResponse): JsValue = JsObject(
      "code" -> JsString(obj.code),
      "message" -> JsString(obj.message)
    )
  }

  implicit val addTableItemFormat = jsonFormat2(AddTableItem)
  implicit val tableItemFormat = jsonFormat3(TableItem)
  implicit val authenticationRequestFormat = jsonFormat2(LoginRequest)
  implicit val pingRequestFormat = jsonFormat1(PingRequest)
  implicit val subscribeTablesRequestFormat = jsonFormat0(SubscribeTablesRequest)
  implicit val unsubscribeTablesRequestFormat = jsonFormat0(UnsubscribeTablesRequest)
  implicit val addTableRequestFormat = jsonFormat2(AddTableRequest)
  implicit val updateTableRequestFormat = jsonFormat1(UpdateTableRequest)
  implicit val removeTableRequestFormat = jsonFormat1(RemoveTableRequest)

  implicit val authenticationSuccessfulResponse = jsonFormat1(LoginSuccessfulResponse)
  implicit val authenticationFailedResponse = jsonFormat0(LoginFailedResponse)
  implicit val pongResponse = jsonFormat1(PongResponse)
  implicit val tableListResponse = jsonFormat1(TableListResponse)
  implicit val notAuthorizedResponse = jsonFormat0(NotAuthorizedResponse)
  implicit val tableAddedResponse = jsonFormat2(TableAddedResponse)
  implicit val tableRemovedResponse = jsonFormat1(TableRemovedResponse)
  implicit val tableUpdatedResponse = jsonFormat1(TableUpdatedResponse)
  implicit val removalFailedResponse = jsonFormat1(RemovalFailedResponse)
  implicit val updateFailedResponse = jsonFormat1(UpdateFailedResponse)
}