package interview.app.actor

import akka.actor.Status.Failure
import akka.actor.{Actor, ActorLogging, ActorRef, Props}
import interview.app.transport.protocol.InMessage._
import interview.app.transport.protocol.OutMessage._
import interview.app.transport.protocol.{AdminMessage, UserType}


class UserSocketActor(authManager: ActorRef, tableManager: ActorRef) extends Actor with ActorLogging {
  import context._

  override def receive: Receive = initFlow

  def onCloseFlow: Receive = {
    case UserSocketActor.Closed =>
      stop(self)
  }

  def initFlow: Receive = onCloseFlow orElse {
    case UserSocketActor.Connected(out) =>
      become(loginFlow(out))
  }

  def loginFlow(out: ActorRef): Receive = onCloseFlow orElse {
    case m: LoginRequest =>
      authManager ! m
    case m: LoginSuccessfulResponse =>
      if (m.user_type == UserType.Admin)
        become(adminFlow(out))
      else
        become(userFlow(out))
      out ! m
    case m: LoginFailedResponse =>
      out ! m
    case Failure(t) =>
      log.error("Actor about to stop because of upstream error: ", t)
      stop(self)
  }

  def userFlow(out: ActorRef): Receive = onCloseFlow orElse {
    case PingRequest(seq) =>
      out ! PongResponse(seq)
    case m @
      ( _: SubscribeTablesRequest
      | _: UnsubscribeTablesRequest) =>
      tableManager ! m
    case m: TableListResponse =>
      out ! m
    case m @
      ( _: TableAddedResponse
      | _: TableRemovedResponse
      | _: TableUpdatedResponse) =>
      out ! m
    case _: AdminMessage =>
      out ! NotAuthorizedResponse()
  }

  def adminFlow(out: ActorRef): Receive = ({
    case m @
      (_: AddTableRequest
      |_: UpdateTableRequest
      |_: RemoveTableRequest) =>
      tableManager ! m
    case m: UpdateFailedResponse =>
      out ! m
    case m: RemovalFailedResponse =>
      out ! m
  }: Receive) orElse userFlow(out)

}

object UserSocketActor {

  def props(authManager: ActorRef, tableManager: ActorRef) = Props(new UserSocketActor(authManager, tableManager))

  case class Connected(out: ActorRef)
  case object Closed
}