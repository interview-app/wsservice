package interview.app.transport

object protocol {

  sealed abstract class MessageType(val t: String)
  sealed abstract class InMessageType(t: String) extends MessageType(t)
  sealed abstract class OutMessageType(t: String) extends MessageType(t)

  sealed abstract class Message[T <: MessageType](val $type: T)
  abstract class InMessage(override val $type: InMessageType) extends Message($type)
  abstract class OutMessage(override val $type: OutMessageType) extends Message($type)

  object InMessageType {
    case object Login extends InMessageType("login")
    case object Ping extends InMessageType("ping")
    case object SubscribeTables extends InMessageType("subscribe_tables")
    case object UnsubscribeTables extends InMessageType("unsubscribe_tables")
    case object AddTable extends InMessageType("add_table")
    case object UpdateTable extends InMessageType("update_table")
    case object RemoveTable extends InMessageType("remove_table")

    val values = Login :: Ping :: SubscribeTables :: UnsubscribeTables :: AddTable :: UpdateTable :: RemoveTable :: Nil

    def apply(msgType: String): Option[InMessageType] = values.find(_.t == msgType)
  }

  object OutMessageType {
    case object LoginSuccessful extends OutMessageType("login_successful")
    case object LoginFailed extends OutMessageType("login_failed")
    case object Pong extends OutMessageType("pong")
    case object TableList extends OutMessageType("table_list")
    case object NotAuthorized extends OutMessageType("not_authorized")
    case object TableAdded extends OutMessageType("table_added")
    case object TableRemoved extends OutMessageType("table_removed")
    case object TableUpdated extends OutMessageType("table_updated")
    case object RemovalFailed extends OutMessageType("removal_failed")
    case object UpdateFailed extends OutMessageType("update_failed")
    case object Error extends OutMessageType("error")

    val values = LoginSuccessful :: LoginFailed :: Pong :: TableList :: NotAuthorized :: TableAdded :: TableRemoved ::
      TableUpdated :: TableUpdated :: RemovalFailed :: UpdateFailed :: Nil

    def apply(msgType: String): Option[OutMessageType] = values.find(_.t == msgType)
  }

  sealed abstract class UserType(val t: String)

  object UserType {
    case object User extends UserType("user")
    case object Admin extends UserType("admin")

    val values = User :: Admin :: Nil

    def apply(userType: String): Option[UserType] = values.find(_.t == userType)
  }

  import InMessageType._
  import OutMessageType._

  case class TableItem(id: Int, name: String, participants: Int)
  case class AddTableItem(name: String, participants: Int)

  trait AdminMessage

  sealed abstract class ErrorResponse(val code: String, val message: String) extends OutMessage(Error)

  object InMessage {
    case class LoginRequest(username: String, password: String) extends InMessage(Login)
    case class PingRequest(seq: Int) extends InMessage(Ping)
    case class SubscribeTablesRequest() extends InMessage(SubscribeTables)
    case class UnsubscribeTablesRequest() extends InMessage(UnsubscribeTables)
    case class AddTableRequest(after_id: Int, table: AddTableItem) extends InMessage(AddTable) with AdminMessage
    case class UpdateTableRequest(table: TableItem) extends InMessage(UpdateTable) with AdminMessage
    case class RemoveTableRequest(id: Int) extends InMessage(RemoveTable) with AdminMessage
  }

  object OutMessage {
    case class LoginSuccessfulResponse(user_type: UserType) extends OutMessage(LoginSuccessful)
    case class LoginFailedResponse() extends OutMessage(LoginFailed)
    case class PongResponse(seq: Int) extends OutMessage(Pong)
    case class TableListResponse(tables: Vector[TableItem]) extends OutMessage(TableList)
    case class NotAuthorizedResponse() extends OutMessage(NotAuthorized)
    case class TableAddedResponse(after_id: Int, table: TableItem) extends OutMessage(TableAdded)
    case class TableUpdatedResponse(table: TableItem) extends OutMessage(TableUpdated)
    case class TableRemovedResponse(id: Int) extends OutMessage(TableRemoved)
    case class RemovalFailedResponse(id: Int) extends OutMessage(RemovalFailed)
    case class UpdateFailedResponse(id: Int) extends OutMessage(UpdateFailed)
    case class MalformedMessageResponse() extends ErrorResponse("ERR1", "Malformed message")
  }

}
