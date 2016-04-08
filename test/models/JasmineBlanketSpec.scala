package models

import controllers.routes
import play.Logger
import play.api.test.Helpers._
import resources.SpecificationWithFixtures

class JasmineBlanketSpec extends SpecificationWithFixtures {
  val distributorUser = running(testApplication) {
    distributorUserService.create(email, password, "Company Name")
    distributorUserService.findByEmail(email).get
  }

  val distributorID = distributorUser.distributorID.get
  val minCoveragePercent = "70.0"

  "JavaScript tests and coverage" should {
    "Pass Jasmine tests with BlanketJS coverage" in new WithAppBrowser(distributorUser.distributorID.get) {
      browser.goTo(routes.Assets.at("""/javascripts/test/SpecRunner.html""").url)
      browser.await().atMost(30, java.util.concurrent.TimeUnit.SECONDS).until(".bar.passed").isPresent
      val page = browser.pageSource()
      val blanketPartOfPage = page
        .drop(page.indexOfSlice("""<script type="text/javascript">function blanket_toggleSource("""))
        .replaceAll("http://localhost:19001/assets//", "")
      val idx = blanketPartOfPage.indexOfSlice("%</div>")
      // percent format XX.XX
      val coveragePercent = blanketPartOfPage.substring(idx - 6, idx).toDouble
      val minPercent = sys.env.getOrElse("COVERAGE_PERCENT_JS", minCoveragePercent).toDouble
      val goodOrBad =  if (coveragePercent >= minPercent) "met the " else "did not meet the "
      Logger.info(s"""coverage percent: $coveragePercent  $goodOrBad min required coverage: $minPercent""")
      coveragePercent > minPercent mustEqual true

      scala.tools.nsc.io.File("./target/test-reports/blanketjs.html").writeAll(
        """
            <!DOCTYPE html><html xmlns="http://www.w3.org/1999/xhtml"
        class="mac chrome chrome4 webkit webkit5 msie msie1 trident trident1 msie9 trident9 opera opera9 presto prestof chrome3 firefox firefox4 gecko gecko2"><head>
                <script type="text/javascript" src="https://www.google.com/uds/?file=visualization&amp;v=1.1&amp;packages=corechart%2Ctable&amp;async=2"></script>
                <link type="text/css" href="https://www.google.com/uds/api/visualization/1.1/cdc66802afe0f81ba897232917523eee/ui+en,table+en.css" rel="stylesheet" />
                <script type="text/javascript" src="https://www.google.com/uds/api/visualization/1.1/cdc66802afe0f81ba897232917523eee/webfontloader,format+en,default+en,ui+en,table+en,corechart+en.I.js"></script>
                <head><style>#blanket-main {margin:2px;background:#EEE;color:#333;clear:both;font-family:'Helvetica Neue Light', 'HelveticaNeue-Light', 'Helvetica Neue', Calibri, Helvetica, Arial, sans-serif; font-size:17px;}
        #blanket-main a {color:#333;text-decoration:none;}  #blanket-main a:hover {text-decoration:underline;} .blanket {margin:0;padding:5px;clear:both;border-bottom: 1px solid #FFFFFF;}
        .bl-error {color:red;}.bl-success {color:#5E7D00;} .bl-file{width:auto;} .bl-cl{float:left;} .blanket div.rs {margin-left:50px; width:150px; float:right} .bl-nb {padding-right:10px;}
        #blanket-main a.bl-logo {color: #EB1764;cursor: pointer;font-weight: bold;text-decoration: none} .bl-source{ overflow-x:scroll; background-color: #FFFFFF;
                border: 1px solid #CBCBCB; color: #363636; margin: 25px 20px; width: 80%;} .bl-source div{white-space: pre;font-family: monospace;} .bl-source &gt; div &gt;
            span:first-child{background-color: #EAEAEA;color: #949494;display: inline-block;padding: 0 10px;text-align: center;width: 30px;} .bl-source .hit{background-color:#c3e6c7}
        .bl-source .miss{background-color:#e6c3c7} .bl-source span.branchWarning{color:#000;background-color:yellow;} .bl-source span.branchOkay{color:#000;background-color:transparent;}</style></head><body>
        """ + blanketPartOfPage)
    }
  }
}
