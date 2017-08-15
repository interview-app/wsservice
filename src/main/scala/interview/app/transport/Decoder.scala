package interview.app.transport

import akka.http.scaladsl.model.ws.{Message, TextMessage}
import akka.stream.Materializer
import interview.app.transport.protocol.{InMessage, InMessageType}

import scala.concurrent.{ExecutionContext, Future}


trait Decoder extends JsonSupport {

  implicit def mat: Materializer
  implicit def ex: ExecutionContext

  import InMessage._
  import InMessageType._
  import spray.json._

  def decode(m: Message): Future[InMessage] = m match {
    case m: TextMessage =>
      m.textStream.runReduce(_ + _).map(v => {
        val jsonValue = v.parseJson
        //derivation of type
        jsonValue.convertTo[InMessage].$type match {
          case Login => jsonValue.convertTo[LoginRequest]
          case Ping => jsonValue.convertTo[PingRequest]
          case SubscribeTables => jsonValue.convertTo[SubscribeTablesRequest]
          case UnsubscribeTables => jsonValue.convertTo[UnsubscribeTablesRequest]
          case AddTable => jsonValue.convertTo[AddTableRequest]
          case UpdateTable => jsonValue.convertTo[UpdateTableRequest]
          case RemoveTable => jsonValue.convertTo[RemoveTableRequest]
          case t => throw new DeserializationException(s"Unknown message type[$t]")
        }
      })
    case _ => throw new DeserializationException("Binary messages are not supported")
  }

}