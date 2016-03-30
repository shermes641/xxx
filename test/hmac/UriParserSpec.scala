package hmac

import com.netaporter.uri._
import com.netaporter.uri.dsl._

/**
  * Created by shermes on 1/28/16.
  *
  * Verify we can parse out the elements of a request
  */
class UriParserSpec extends BaseSpec {

  val passingUris =
    Table(
      ("uri",
        "scheme",
        "port",
        "method",
        "hostParts",
        "path",
        "query",
        "fragment",
        "qs1Encoded"), // First tuple defines column names
      (("https://github.com/NET-A-PORTER" ? ("q1" -> "üri 666")).toString,
        "https",
        Constants.httpsPort,
        "POST",
        Vector("github", "com"),
        "/NET-A-PORTER",
        QueryString(Vector(("q1", Some("üri 666")))),
        None,
        "üri 666"),
      (("http://github.com/NET-A-PORTER" ? ("q1" -> "üri 666")).toString,
        "http",
        Constants.httpPort,
        "POST",
        Vector("github", "com"),
        "/NET-A-PORTER",
        QueryString(Vector(("q1", Some("üri 666")))),
        None,
        "üri 666"),
      (("http://example.com/path with space" ? ("q1" -> "üri 666")).toString,
        "http",
        Constants.httpPort,
        "POST",
        Vector("example", "com"),
        "/path%20with%20space",
        QueryString(Vector(("q1", Some("üri 666")))),
        None,
        "üri 666")
    )

  property("Parse good URI's should all pass") {
    forAll(passingUris) { (uri: String,
                           scheme: String,
                           port: Int,
                           method: String,
                           hostParts: Seq[String],
                           path: String,
                           query: QueryString,
                           fragment: Option[String],
                           qs1Encoded: String) =>
      val u = new ParseUri(uri).parsed.get
      u.scheme shouldEqual scheme
      u.hostParts shouldEqual hostParts
      u.path shouldEqual path
      u.query shouldEqual query
      u.fragment shouldEqual fragment
      qs1Encoded shouldEqual query.param("q1").getOrElse("no param with that name")
    }
  }

  val badSchemeUri =
    Table(
      ("uri",
        "scheme",
        "port",
        "method",
        "hostParts",
        "path",
        "query",
        "fragment",
        "qs1Encoded"),
      (("https://x.com/bad scheme" ? ("q1" -> "üri 666")).toString,
        "http",
        Constants.httpsPort,
        "POST",
        Vector("x", "com"),
        "/bad%20scheme",
        QueryString(Vector(("q1", Some("üri 666")))),
        None,
        "üri 666"))

  val badHostUri =
    Table(
      ("uri",
        "scheme",
        "port",
        "method",
        "hostParts",
        "path",
        "query",
        "fragment",
        "qs1Encoded"),

      (("http://!@#$%^&*()_+.com/NET-A-PORTER" ? ("q1" -> "üri 666")).toString,
        "http",
        Constants.httpPort,
        "POST",
        Vector("github", "com"),
        "/NET-A-PORTER",
        QueryString(Vector(("q1", Some("üri 666")))),
        None,
        "üri 666"))

  val badQueryString =
    Table(
      ("uri",
        "scheme",
        "port",
        "method",
        "hostParts",
        "path",
        "query",
        "fragment",
        "qs1Encoded"),
      (("http://example.com/path with space" ? ("q1" -> "üri 666")).toString,
        "http",
        Constants.httpPort,
        "POST",
        Vector("example", "com"),
        "/path%20with%20space",
        QueryString(Vector(("q10", Some("üri 666")))),
        None,
        "üri 666"))

  property("Parse URI with bad scheme should fail") {
    forAll(badSchemeUri) { (uri: String,
                           scheme: String,
                           port: Int,
                           method: String,
                           hostParts: Seq[String],
                           path: String,
                           query: QueryString,
                           fragment: Option[String],
                           qs1Encoded: String) =>
      val u = new ParseUri(uri).parsed.get
      u.scheme shouldNot be(scheme)
      u.hostParts shouldEqual hostParts
      u.path shouldEqual path
      u.query shouldEqual query
      u.fragment shouldEqual fragment
      qs1Encoded shouldEqual query.param("q1").getOrElse("no param with that name")
    }
  }

  property("Parse URI with bad host should fail") {
    forAll(badHostUri) { (uri: String,
                            scheme: String,
                            port: Int,
                            method: String,
                            hostParts: Seq[String],
                            path: String,
                            query: QueryString,
                            fragment: Option[String],
                            qs1Encoded: String) =>
      val u = new ParseUri(uri).parsed.get
      u.scheme shouldEqual scheme
      u.hostParts shouldNot  be (hostParts)
      u.path shouldEqual path
      u.query shouldEqual query
      u.fragment shouldEqual fragment
      qs1Encoded shouldEqual query.param("q1").getOrElse("no param with that name")
    }
  }

  property("Parse URI with missing query parameter should fail") {
    forAll(badQueryString) { (uri: String,
                          scheme: String,
                          port: Int,
                          method: String,
                          hostParts: Seq[String],
                          path: String,
                          query: QueryString,
                          fragment: Option[String],
                          qs1Encoded: String) =>
      val u = new ParseUri(uri).parsed.get
      u.scheme shouldEqual scheme
      u.hostParts shouldEqual hostParts
      u.path shouldEqual path
      u.query shouldNot be (query)
      u.fragment shouldEqual fragment
      qs1Encoded shouldNot be (query.param("q1").getOrElse("no param with that name"))
    }
  }
}
