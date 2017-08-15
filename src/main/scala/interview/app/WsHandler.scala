package interview.app

import akka.NotUsed
import akka.actor.{ActorRef, ActorSystem}
import akka.http.scaladsl.model.ws.Message
import akka.http.scaladsl.server.Directives
import akka.stream.Supervision.Resume
import akka.stream.scaladsl.{Flow, Sink, Source}
import akka.stream.{ActorAttributes, ActorMaterializer, OverflowStrategy}
import com.typesafe.scalalogging.StrictLogging
import interview.app.actor.UserSocketActor
import interview.app.transport.protocol.InMessage
import interview.app.transport.protocol.OutMessage.MalformedMessageResponse
import interview.app.transport.{Decoder, Encoder}
import spray.json.DeserializationException
import spray.json.JsonParser.ParsingException

import scala.concurrent.ExecutionContext

case class WsHandler(authManager: ActorRef, tableManager: ActorRef)(implicit sys: ActorSystem, override val mat: ActorMaterializer, override val ex: ExecutionContext)
  extends Decoder with Encoder with Directives with StrictLogging {

  def flow(): Flow[Message, Message, NotUsed] = {
    val userSocketActor = sys.actorOf(UserSocketActor.props(authManager, tableManager))

    var out = Option.empty[ActorRef]
    val producer = Source.actorRef(10, OverflowStrategy.fail).mapMaterializedValue(outActor => {
      out = Some(outActor)
      userSocketActor ! UserSocketActor.Connected(outActor)
    }).map(encode)

    val consumer = Flow[Message]
      .mapAsync(1)(decode)
      .withAttributes(ActorAttributes.supervisionStrategy({
        case ex @
          (_: DeserializationException
          |_: ParsingException) =>
          logger.error("Cannot process incoming message: ", ex)
          out.map(_ ! MalformedMessageResponse())
          Resume
      }))
      .to(Sink.actorRef[InMessage](userSocketActor, UserSocketActor.Closed))

    Flow.fromSinkAndSource(consumer, producer)
  }

  val wsRoute = path("ws"){
    get {
      handleWebSocketMessages(flow())
    }
  }

}