package lila
package tournament

import play.api.data._
import play.api.data.Forms._
import play.api.data.validation.Constraints._

final class DataForm(isDev: Boolean) {

  import lila.core.Form._

  val clockTimes = 0 to 10 by 1
  val clockTimeDefault = 2
  val clockTimeChoices = options(clockTimes, "%d minute{s}")

  val clockIncrements = 0 to 2 by 1
  val clockIncrementDefault = 0
  val clockIncrementChoices = options(clockIncrements, "%d second{s}")

  val minutes = isDev.fold(
    (1 to 9) ++ (10 to 60 by 5),
    10 to 60 by 5
  )
  val minuteDefault = 30
  val minuteChoices = options(minutes, "%d minute{s}")

  val minPlayers = isDev.fold(
    (2 to 9) ++ (10 to 30 by 5),
    (5 to 9) ++ (10 to 30 by 5)
  )
  val minPlayerDefault = 10
  val minPlayerChoices = options(minPlayers, "%d player{s}")

  lazy val create = Form(mapping(
    "clockTime" -> numberIn(clockTimeChoices),
    "clockIncrement" -> numberIn(clockIncrementChoices),
    "minutes" -> numberIn(minuteChoices),
    "minPlayers" -> numberIn(minPlayerChoices)
  )(TournamentSetup.apply)(TournamentSetup.unapply)
    .verifying("Invalid clock", _.validClock)
  ) fill TournamentSetup(
    clockTime = clockTimeDefault,
    clockIncrement = clockIncrementDefault,
    minutes = minuteDefault,
    minPlayers = minPlayerDefault)
}

case class TournamentSetup(
    clockTime: Int,
    clockIncrement: Int,
    minutes: Int,
    minPlayers: Int) {

  def validClock = (clockTime + clockIncrement) > 0
}
