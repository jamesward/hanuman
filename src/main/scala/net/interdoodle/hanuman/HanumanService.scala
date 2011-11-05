package net.interdoodle.hanuman

import akka.stm.Ref
import blueeyes._
import blueeyes.concurrent.Future
import blueeyes.core.data.{BijectionsChunkJson, BijectionsChunkString, ByteChunk}
import blueeyes.core.http.{HttpRequest, HttpResponse, HttpStatus}
import blueeyes.core.http.combinators.HttpRequestCombinators
import blueeyes.core.http.HttpStatusCodes
import blueeyes.json.JsonAST._
import core.service._
import java.util.UUID
import net.interdoodle.hanuman.message.SimulationStatus
import net.interdoodle.hanuman.domain.Hanuman
import net.interdoodle.hanuman.domain.Hanuman.Simulations
import net.lag.logging.Logger
import akka.actor.{ActorRef, Actor}


/**
 * @author Mike Slinn */

trait HanumanService extends BlueEyesServiceBuilder
  with HttpRequestCombinators
  with BijectionsChunkString
  with BijectionsChunkJson {

  var hanuman:Option[Hanuman] = None
  var hanumanRefOption:Option[ActorRef] = None

  /** TODO make into configurable parameter */
  val maxTicks:Int = 10

  /** TODO make into configurable parameter */
  val monkeysPerVisor:Int = 10

  val simulations:Simulations = new Simulations()
  /** Contains simulationID->Option[MonkeyVisorRef] map */
  val simulationStatus = new SimulationStatus(false, None, simulations)
  val simulationStatusRef = new Ref(simulationStatus)

  val versionMajor = 0
  val versionMinor = 1


  val helloJson: HttpService[ByteChunk] = service("helloJson", "0.1") {
    requestLogging {
      logging {
        log =>
          context: HttpServiceContext =>
            request {
              path("/json/") {
                jvalue {
                  get { requestParam:HttpRequest[JValue] => reqHello(log) } ~
                  path('operation) {
                    get { request => reqOperation(log, request) }
                  } ~
                  path('operation/'id) {
                    get { request => reqDoCommand(log, request) }
                  } ~
                  path('operation/'id/'param) {
                    get { request => reqDoCommandParam(log, request) }
                  } ~
                  orFail(HttpStatusCodes.NotFound, "No handler found that could handle this request.") // return HTTP status 404
                }
              }
            }
      }
    }
  }

  private def reqHello[T, S](log:Logger) = {
    val json = JString("Hanuman v" + versionMajor.toString + "." + versionMinor.toString)
    val response = HttpResponse[JValue](content = Some(json))
    log.info(response.toString())
    Future.sync(response)
  }

  private def reqOperation[T, S](log:Logger, request:HttpRequest[T]):Future[HttpResponse[JValue]] = {
    val operation = request.parameters('operation)
    if (operation=="newSimulation") {
      val document = "ABCDEFGHIJKLMNOPQRSTUVWXYZ"*5 + // faked for now because BlueEyes cannot parse POST parameters
        "abcdefghijklmnopqrstuvwxyz"*25 +
        "0123456789"*2 +
        "`~!@#$%^&*()_-+={[}]|\\\"':;<,>.?/"
      val simulationID = UUID.randomUUID().toString

      simulationStatus.putSimulation(simulationID, None)
      hanumanRefOption = Some(Actor.actorOf(new Hanuman(simulationID, monkeysPerVisor, maxTicks, document, simulationStatusRef)))
      simulationStatus.putSimulation(simulationID, hanumanRefOption)
      simulationStatusRef.set(simulationStatus)

      Future.sync(HttpResponse(
        /*headers = HttpHeaders.Empty + sessionCookie(simulationID),*/
        content = Some(simulationID)))
    } else {
      val msg = "The only operation that can be without a simulationID is newSimulation. You specified '" + operation + "'"
      Future.sync(HttpResponse(status=HttpStatus(400, msg), content = Some(msg)))
    }
  }

  private def reqDoCommand[T, S](log:Logger, request:HttpRequest[T]):Future[HttpResponse[JValue]] = {
    val operation = request.parameters('operation).toString
    val simulationID = request.parameters('id).toString
    val simulation = simulationStatus.getSimulation(simulationID)
    Future.sync(HttpResponse(
      /*headers = HttpHeaders.Empty + sessionCookie(simulationID),*/
      content = Some(if (simulation==None) {
        "Simulation with ID " + simulationID + " does not exist"
      } else
        doCommand(log, operation, simulationID)
    )))
  }

  private def reqDoCommandParam[T, S](log:Logger, request:HttpRequest[T]):Future[HttpResponse[JValue]] = {
    val operation = request.parameters('operation)
    val simulationID = request.parameters('id)
    val param = request.parameters('param)
    val simulation = simulationStatus.getSimulation(simulationID)
    Future.sync(HttpResponse(
      /*headers = HttpHeaders.Empty + sessionCookie(simulationID),*/
      content = if (simulation==None)
        Some("Simulation with ID " + simulationID + " does not exist")
      else
        Some("Simulation ID=" + simulationID + "; operation: '" + operation + "'"))
    )
  }

  private def doCommand(log:Logger, command:String, simulationID:String):String = {
    command match {
      case "run" =>
        val hanumanRef = hanumanRefOption.get
        hanumanRef.start
        "Updated simulationStatus with new Hanuman instance " + hanumanRef.id + " and started hanuman"

      case "status" =>
        val simulationStatus = simulationStatusRef.get
        // TODO return simulationStatus object in JSON format to client
        //monkeyResult.msg
        "TODO return simulation status in JSON format to client"

      case _ =>
        command + "is an unknown command"
    }
  }

   /** BlueEyes cookie support is not fully baked */
  /*private def sessionCookie(simulationID:String) = {
    val cookie = new HttpCookie {
      def name = "SessionID"
      def cookieValue = simulationID
      def expires = Some(HttpDateTime.parseHttpDateTimes("MON, 01-JAN-2001 00:00:00 UTC"))
      def domain = Option("")
      def path = Option("")
      // TODO add "; HttpOnly"
    }
    `Set-Cookie`(cookie :: Nil)
  }*/
}
