package interview.app

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.stream.ActorMaterializer
import com.typesafe.config.{Config, ConfigFactory, ConfigRenderOptions}
import com.typesafe.scalalogging.StrictLogging
import interview.app.actor.{AuthenticationManagerActor, TableManagerActor}

import scala.util.{Failure, Success}


object WsServer extends StrictLogging {

  val configPath = "interview.app.server"

  def main(args: Array[String]): Unit = {
    logger.info(
      s"""
         |
         |***************************************
         |**  WebSocket server is starting...  **
         |***************************************
      """.stripMargin)

    val rootConfig = ConfigFactory.load()
    val serverConfig = loadWsServerConfig(rootConfig)

    implicit val sys = ActorSystem("WebSocketServer", rootConfig)
    implicit val mat = ActorMaterializer()
    implicit val ec = sys.dispatcher

    val host = serverConfig.getString("host")
    val port = serverConfig.getInt("port")

    val authManager = sys.actorOf(AuthenticationManagerActor.props())
    val tableManager = sys.actorOf(TableManagerActor.props())

    Http().bindAndHandle(WsHandler(authManager, tableManager).wsRoute, host, port).onComplete({
      case Success(_) =>
        logger.info(
          s"""
             |
             |**************************************
             |**  WebSocket server service is up  **
             |**************************************
            """.stripMargin)
      case Failure(t) =>
        sys.terminate().onComplete({
          case Failure(t) => logger.error("Unable to close ActorSystem", t)
          case _ =>
        })

        logger.error(s"Unable to start WebSocket server", t)
    })
  }


  def loadWsServerConfig(config: Config): Config = {
    val c = config.getConfig(configPath)
    logger.info(s"Effective config\n" + c.root().render(
      ConfigRenderOptions.defaults()
        .setFormatted(true)
        .setComments(false)
        .setOriginComments(false)
    ))
    c
  }

}