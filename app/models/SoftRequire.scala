package models

import com.typesafe.config.{Config, ConfigFactory}
import com.typesafe.scalalogging.LazyLogging

// scalastyle:off magic.number

/**
  * Provides a replacement for scala.Predef.require that won't throw
  * exceptions if it fails. This is helpful for surfacing broken invariants in
  * logs and alerting, while not failing outright and causing the service to be
  * visibly disrupted. So, it's most useful when some sensible default can still
  * be returned.
  *
  * It also has the concept of an error context that can be used to add
  * circumstantial info from a surrounding scope, without polluting individual
  * calls.
  *
  * Lastly, in testing, it's designed to fail fatally.
  */
trait SoftRequire extends LazyLogging {

  def softRequire(requirement: Boolean, message: => Any)(
    implicit errorContext: Option[SoftRequire.ErrorContext] = None
  ): Unit = {
    if (!requirement) {
      logError(s"requirement failed: $message")
      if (SoftRequire.isFatal) throw new Exception("softRequire failure.")
    }
  }

  def logError(
                message: String
              )(implicit errorContext: Option[SoftRequire.ErrorContext] = None): Unit = {
    val context     = errorContext.map("context: " + _.context)
    val stackDump   = Thread.currentThread().getStackTrace.take(30).mkString("\n")
    val messageList = Seq(message) ++ context ++ Seq(stackDump)

    logger.error(messageList.mkString("\n"))
  }
}

object SoftRequire extends SoftRequire {

  trait ErrorContext {
    def context: String
  }

  protected val fatalFailureKey = "soft-require.fatal-failures"
  protected val config: Config = ConfigFactory.load()

  val isFatal: Boolean = config.hasPath(fatalFailureKey) && config.getBoolean(
    fatalFailureKey
  )
}
