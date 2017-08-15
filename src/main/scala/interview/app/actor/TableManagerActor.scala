package interview.app.actor

import akka.actor.{Actor, ActorLogging, ActorRef, Props, Terminated}
import interview.app.transport.protocol.InMessage._
import interview.app.transport.protocol.OutMessage._
import interview.app.transport.protocol.TableItem


class TableManagerActor extends Actor with ActorLogging {

  import context._

  override def receive: Receive = flow(Vector(), List())

  def flow(tables: Vector[TableItem], subscribers: List[ActorRef]): Receive = {
    case SubscribeTablesRequest() =>
      val s = sender()
      s ! TableListResponse(tables)
      watch(s)
      become(flow(tables, s :: subscribers))

    case UnsubscribeTablesRequest() =>
      val s = sender()
      unwatch(s)
      become(flow(tables, subscribers.filter(_ != s)))

    case Terminated(actor) =>
      become(flow(tables, subscribers.filter(_ != actor)))

    case AddTableRequest(after_id, table) =>
      val newId = if (tables.isEmpty) 1 else tables.maxBy(_.id).id + 1
      val newTable = TableItem(newId, table.name, table.participants)
      val (finalAfterId, atIndex) =
        if (after_id != -1) {
          val possibleIdx = tables.indexWhere(_.id == after_id)
          if (possibleIdx != -1) (after_id, possibleIdx + 1)
          else (tables.lastOption.map(_.id).getOrElse(-1), tables.size)
        } else (after_id, 0)

      val (init, tail) = tables.splitAt(atIndex)
      val newTables = (init :+ newTable) ++ tail
      subscribers.foreach(_ ! TableAddedResponse(finalAfterId, newTable))
      become(flow(newTables, subscribers))

    case UpdateTableRequest(table) =>
      val idx = tables.indexWhere(_.id == table.id)
      if (idx != -1) {
        val updatedTables = tables.updated(idx, table)
        subscribers.foreach(_ ! TableUpdatedResponse(table))
        become(flow(updatedTables, subscribers))
      } else {
        sender() ! UpdateFailedResponse(table.id)
      }

    case RemoveTableRequest(id) =>
      val idx = tables.indexWhere(_.id == id)
      if (idx != -1) {
        val updatedTables = tables.take(idx) ++ tables.drop(idx + 1)
        subscribers.foreach(_ ! TableRemovedResponse(id))
        become(flow(updatedTables, subscribers))
      } else {
        sender() ! RemovalFailedResponse(id)
      }
  }

}

object TableManagerActor {
  def props() = Props(new TableManagerActor)
}