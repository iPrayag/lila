package lila
package round

import scalaz.effects._
import com.mongodb.casbah.MongoCollection
import akka.actor.Props
import play.api.libs.concurrent._
import play.api.Application

import game.{ GameRepo, PgnRepo, DbGame, Rewind }
import user.{ UserRepo, User }
import elo.EloUpdater
import ai.Ai
import core.Settings
import i18n.I18nKeys
import security.Flood

final class RoundEnv(
    app: Application,
    settings: Settings,
    mongodb: String ⇒ MongoCollection,
    gameRepo: GameRepo,
    pgnRepo: PgnRepo,
    rewind: Rewind,
    userRepo: UserRepo,
    eloUpdater: EloUpdater,
    i18nKeys: I18nKeys,
    ai: () ⇒ Ai,
    countMove: () ⇒ Unit,
    flood: Flood,
    indexGame: DbGame ⇒ IO[Unit]) {

  implicit val ctx = app
  import settings._

  lazy val history = () ⇒ new History(timeout = RoundMessageLifetime)

  lazy val hubMaster = Akka.system.actorOf(Props(new HubMaster(
    makeHistory = history,
    uidTimeout = RoundUidTimeout,
    hubTimeout = RoundHubTimeout,
    playerTimeout = RoundPlayerTimeout
  )), name = ActorRoundHubMaster)

  private lazy val moveNotifier = new MoveNotifier(
    siteHubName = ActorSiteHub,
    lobbyHubName = ActorLobbyHub,
    tournamentHubMasterName = ActorTournamentHubMaster,
    countMove = countMove)

  lazy val socket = new Socket(
    getWatcherPov = gameRepo.pov,
    getPlayerPov = gameRepo.pov,
    hand = hand,
    hubMaster = hubMaster,
    messenger = messenger,
    moveNotifier = moveNotifier,
    flood = flood)

  lazy val hand = new Hand(
    gameRepo = gameRepo,
    pgnRepo = pgnRepo,
    messenger = messenger,
    ai = ai,
    finisher = finisher,
    takeback = takeback,
    hubMaster = hubMaster,
    moretimeSeconds = RoundMoretime)

  lazy val finisher = new Finisher(
    userRepo = userRepo,
    gameRepo = gameRepo,
    messenger = messenger,
    eloUpdater = eloUpdater,
    eloCalculator = eloCalculator,
    finisherLock = finisherLock,
    indexGame = indexGame,
    tournamentOrganizerActorName = ActorTournamentOrganizer)

  lazy val eloCalculator = new chess.EloCalculator(false)

  lazy val finisherLock = new FinisherLock(timeout = FinisherLockTimeout)

  lazy val takeback = new Takeback(
    gameRepo = gameRepo,
    pgnRepo = pgnRepo,
    rewind = rewind,
    messenger = messenger)

  lazy val messenger = new Messenger(
    roomRepo = roomRepo,
    watcherRoomRepo = watcherRoomRepo,
    i18nKeys = i18nKeys)

  lazy val roomRepo = new RoomRepo(
    mongodb(RoundCollectionRoom))

  lazy val watcherRoomRepo = new WatcherRoomRepo(
    mongodb(RoundCollectionWatcherRoom))

  lazy val meddler = new Meddler(
    gameRepo = gameRepo,
    finisher = finisher,
    socket = socket)
}
