package interview.app

import akka.http.scaladsl.testkit.{ScalatestRouteTest, WSProbe, WSTestRequestBuilding}
import interview.app.actor.{AuthenticationManagerActor, TableManagerActor}
import interview.app.transport.Encoder
import interview.app.transport.protocol.InMessage._
import interview.app.transport.protocol.OutMessage._
import interview.app.transport.protocol._
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.{Matchers, WordSpec}

class WsHandlerSpec extends WordSpec
  with Matchers
  with ScalatestRouteTest
  with ScalaFutures
  with WSTestRequestBuilding
  with Encoder {

  import spray.json._

  val authManager = system.actorOf(AuthenticationManagerActor.props())
  val tableManager = system.actorOf(TableManagerActor.props())
  val wsRoute = WsHandler(authManager, tableManager).wsRoute

  "WsHandler" should {

    "fail login" in {
      val wsClient = WSProbe()
      WS("/ws", wsClient.flow) ~> wsRoute ~> check {
        wsClient.sendMessage(LoginRequest(username = "", password = "").text)
        wsClient.expectMessage(LoginFailedResponse().text)
      }
    }

    "login user with role user and respond to ping" in {
      val wsClient = WSProbe()
      WS("/ws", wsClient.flow) ~> wsRoute ~> check {
        loginUser(wsClient)

        val seq = 1
        wsClient.sendMessage(PingRequest(seq).text)
        wsClient.expectMessage(PongResponse(seq).text)
      }
    }

    "login two users and one admin and handle complex flow of subscribe/unsubscribe/add/notify/update/remove tables" in {
      val wsAdmin = WSProbe()
      val wsUser1 = WSProbe()
      val wsUser2 = WSProbe()

      WS("/ws", wsUser1.flow) ~> wsRoute ~> check {
        loginUser(wsUser1, "user1")

        WS("/ws", wsUser2.flow) ~> wsRoute ~> check {
          loginUser(wsUser2, "user2")

          WS("/ws", wsAdmin.flow) ~> wsRoute ~> check {
            loginAdmin(wsAdmin)

            val addTable1 = AddTableItem("table - James Bond", 7)
            val table1 = TableItem(1, addTable1.name, addTable1.participants)

            val addTable2 = AddTableItem("table - Mission Impossible", 4)
            val table2 = TableItem(2, addTable2.name, addTable2.participants)

            val addTable3 = AddTableItem("table - Terminator", 2)
            val table3 = TableItem(3, addTable3.name, addTable3.participants)

            wsUser1.sendMessage(SubscribeTablesRequest().text)
            wsUser1.expectMessage(TableListResponse(Vector()).text)

            wsAdmin.sendMessage(AddTableRequest(-1, addTable1).text)
            wsUser1.expectMessage(TableAddedResponse(-1, table1).text)

            wsUser2.sendMessage(SubscribeTablesRequest().text)
            wsUser2.expectMessage(TableListResponse(Vector(table1)).text)

            wsAdmin.sendMessage(AddTableRequest(1, addTable2).text)
            wsUser1.expectMessage(TableAddedResponse(1, table2).text)
            wsUser2.expectMessage(TableAddedResponse(1, table2).text)

            val updatedTable = table1.copy(name = "table - Rambo", participants = 3)
            wsAdmin.sendMessage(UpdateTableRequest(updatedTable).text)
            wsUser1.expectMessage(TableUpdatedResponse(updatedTable).text)
            wsUser2.expectMessage(TableUpdatedResponse(updatedTable).text)

            wsAdmin.sendMessage(RemoveTableRequest(updatedTable.id).text)
            wsUser1.expectMessage(TableRemovedResponse(updatedTable.id).text)
            wsUser2.expectMessage(TableRemovedResponse(updatedTable.id).text)

            wsUser1.sendMessage(UnsubscribeTablesRequest().text)
            wsAdmin.sendMessage(AddTableRequest(2, addTable3).text)
            wsUser1.expectNoMessage()
            wsUser2.expectMessage(TableAddedResponse(2, table3).text)
          }
        }
      }
    }

    "fail update table" in {
      val wsAdmin = WSProbe()
      WS("/ws", wsAdmin.flow) ~> wsRoute ~> check {
        loginAdmin(wsAdmin)

        val table = TableItem(1, "table - James Bond", 7)

        wsAdmin.sendMessage(UpdateTableRequest(table).text)
        wsAdmin.expectMessage(UpdateFailedResponse(table.id).text)
      }
    }

    "fail remove table" in {
      val wsAdmin = WSProbe()
      WS("/ws", wsAdmin.flow) ~> wsRoute ~> check {
        loginAdmin(wsAdmin)

        val tableId = 1

        wsAdmin.sendMessage(RemoveTableRequest(tableId).text)
        wsAdmin.expectMessage(RemovalFailedResponse(tableId).text)
      }
    }

    "fail unauthorized user actions" in {
      val wsUser = WSProbe()
      WS("/ws", wsUser.flow) ~> wsRoute ~> check {
        loginUser(wsUser)

        val addTable = AddTableItem("table - James Bond", 7)
        val table = TableItem(1, addTable.name, addTable.participants)

        val unauthorized = NotAuthorizedResponse().text
        wsUser.sendMessage(AddTableRequest(-1, addTable).text)
        wsUser.expectMessage(unauthorized)
        wsUser.sendMessage(UpdateTableRequest(table).text)
        wsUser.expectMessage(unauthorized)
        wsUser.sendMessage(RemoveTableRequest(table.id).text)
        wsUser.expectMessage(unauthorized)
      }
    }

    "fail malformed message" in {
      val wsClient = WSProbe()
      WS("/ws", wsClient.flow) ~> wsRoute ~> check {
        wsClient.sendMessage("hello")
        wsClient.expectMessage(MalformedMessageResponse().text)
      }
    }

  }

  def loginUser(socket: WSProbe, username: String = "Bob", userType: UserType = UserType.User) = {
    socket.sendMessage(LoginRequest(username = username, password = username).text)
    socket.expectMessage(LoginSuccessfulResponse(userType).text)
  }

  def loginAdmin(socket: WSProbe, username: String = "Bob") = loginUser(socket, "admin_" + username, userType = UserType.Admin)

  implicit class InMessageText[M <: InMessage](m: M)(implicit writer: JsonWriter[M]) {
    def text = JsObject(m.toJson.asJsObject.fields + ("$type" -> m.$type.toJson)).toString
  }

  implicit class OutMessageText[M <: OutMessage](m: M)(implicit writer: JsonWriter[M]) {
    def text = JsObject(m.toJson.asJsObject.fields + ("$type" -> m.$type.toJson)).toString
  }

  implicit class ErrorResponseText[+M <: ErrorResponse](m: M)(implicit writer: JsonWriter[ErrorResponse]) {
    def text = JsObject(writer.write(m).asJsObject.fields + ("$type" -> m.$type.toJson)).toString
  }
}

