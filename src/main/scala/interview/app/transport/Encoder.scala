package interview.app.transport

import akka.http.scaladsl.model.ws.{Message, TextMessage}
import interview.app.transport.protocol.OutMessage


trait Encoder extends JsonSupport {
  import OutMessage._
  import spray.json._

  def encode(m: OutMessage): Message = {
    //derivation of type
    val jsonValue = m match {
      case r: LoginSuccessfulResponse => r.toJson
      case r: LoginFailedResponse => r.toJson
      case r: PongResponse => r.toJson
      case r: TableListResponse => r.toJson
      case r: NotAuthorizedResponse => r.toJson
      case r: TableAddedResponse => r.toJson
      case r: TableRemovedResponse => r.toJson
      case r: TableUpdatedResponse => r.toJson
      case r: RemovalFailedResponse => r.toJson
      case r: UpdateFailedResponse => r.toJson
      case r: MalformedMessageResponse => ErrorResponseWriter.write(r)
      case r => throw new SerializationException("Unknown message: " + r)
    }

    val payload = JsObject(jsonValue.asJsObject.fields + ("$type" -> m.$type.toJson)).toString
    TextMessage(payload)
  }

}