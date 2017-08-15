package interview.app.actor

import akka.actor.{Actor, ActorLogging, Props}
import interview.app.transport.protocol.InMessage.LoginRequest
import interview.app.transport.protocol.OutMessage.{LoginFailedResponse, LoginSuccessfulResponse}
import interview.app.transport.protocol.UserType


class AuthenticationManagerActor extends Actor with ActorLogging {

  override def receive: Receive = {
    case LoginRequest(username, password) =>
      if (username != password || username.isEmpty || password.isEmpty)
        sender() ! LoginFailedResponse()
      else {
        val userType =
          if (username.contains("admin")) UserType.Admin
          else UserType.User

        sender() ! LoginSuccessfulResponse(userType)
      }
  }

}

object AuthenticationManagerActor {
  def props() = Props(new AuthenticationManagerActor())
}