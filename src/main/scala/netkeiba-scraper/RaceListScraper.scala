package netkeiba

import java.io.File
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

import org.apache.commons.io.FileUtils

/**
  * レースの結果が載っているURLのリストを生成するための処理群。
  * https://db.netkeiba.com/race/list/yyyymmdd/でその日付のレース結果へのリンクの一覧が記載されたページ。
  * https://db.netkeiba.com/race/[0-9]{12}で特定のレースの結果が記載されたページ。
  */
object RaceListScraper {

  def scrape(period: Int) = {
    extractRace(period).foreach { url =>
      FileUtils.writeStringToFile(new File("race_list.txt"), url + "\n", true)
    }
  }

  private def extractRace(period: Int) = {

    def range(from: LocalDate, to: LocalDate): Seq[LocalDate] = {
      Range(0, from.until(to, ChronoUnit.DAYS).toInt + 1, 1).map(from.plusDays(_))
    }

    // 5年分の日付のリストを作成する。
    val to            = LocalDate.now
    val from          = to.minusMonths(period)
    val targetDateSeq = range(from, to)

    val formatter = DateTimeFormatter.ofPattern("yyyyMMdd")

    targetDateSeq
      .map { targetDate =>
        Thread.sleep(1000)
        val targetUrl = s"https://db.netkeiba.com/race/list/${targetDate.format(formatter)}/"

        "/race/\\d+/".r
          .findAllIn(io.Source.fromURL(targetUrl, "EUC-JP").mkString)
          .toList
          .distinct
          .map("http://db.netkeiba.com" + _)
      }
      .toList
      .flatten
  }
}
