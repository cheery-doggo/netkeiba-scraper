package netkeiba

import java.io.File
import java.time.LocalDate
import java.time.temporal.ChronoUnit

import org.apache.commons.io.FileUtils

/**
  * レースの結果が載っているURLのリストを生成するための処理群。
  * https://db.netkeiba.com/race/list/yyyymmdd/でその日付のレース結果へのリンクの一覧が記載されたページ。
  * https://db.netkeiba.com/race/[0-9]{12}で特定のレースの結果が記載されたページ。
  */
object RaceListScraper {

  def scrape(period: Int) = {
    var baseUrl = "http://db.netkeiba.com/?pid=race_top"
    var i = 0

    while (i < period) {
      Thread.sleep(1000)
      val raceListPages =
        extractRaceList(baseUrl)
      val racePages =
        raceListPages.map { url =>
          Thread.sleep(1000)
          extractRace(url)
        }.flatten

      racePages.foreach { url =>
        FileUtils.writeStringToFile(new File("race_url.txt"), url + "\n", true)
      }

      baseUrl = extractPrevMonth(baseUrl)
      println(i + ": collecting URLs from " + baseUrl)
      i += 1
    }
  }

  private def extractRaceList(baseUrl: String) = {

    def range(from: LocalDate, to: LocalDate): Seq[LocalDate] =
      Range(0, from.until(to, ChronoUnit.DAYS).toInt + 1, 1).map(from.plusDays(_))

    // 5年分の日付のリストを作成する。
    val to = LocalDate.now
    val from = to.plusDays(365 * 5)
    val targetDateSeq = range(from, to)

    val a = targetDateSeq.map{ targetDate =>
      val targetUrl = s"https://db.netkeiba.com/race/list/${targetDate}/"
      "/race/\\d+/"
        .r
        .findAllIn(io.Source.fromURL(targetUrl, "EUC-JP").mkString)
        .toList
        .map("http://db.netkeiba.com" + _)
    }.distinct.toList.flatten
    a
  }

  private def extractRace(listUrl: String) = {
    "/race/\\d+/".r
      .findAllIn(io.Source.fromURL(listUrl, "EUC-JP").mkString)
      .toList
      .map("http://db.netkeiba.com" + _)
      .distinct
  }

  private def extractPrevMonth(baseList: String) = {
    "/\\?pid=[^\"]+".r
      .findFirstIn(
        io.Source
          .fromURL(baseList, "EUC-JP")
          .getLines
          .filter(_.contains("race_calendar_rev_02.gif"))
          .toList
          .head)
      .map("http://db.netkeiba.com" + _)
      .get
  }

}
