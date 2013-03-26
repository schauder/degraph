package de.schauderhaft.degraph.check

import org.junit.runner.RunWith
import org.scalatest.matchers.ShouldMatchers
import org.scalatest.FunSuite
import org.scalatest.junit.JUnitRunner
import org.scalatest.OptionValues._
import de.schauderhaft.degraph.configuration.Configuration
import de.schauderhaft.degraph.graph.Graph
import de.schauderhaft.degraph.analysis.dependencyFinder.AnalyzerLike
import de.schauderhaft.degraph.model.SimpleNode
import de.schauderhaft.degraph.model.Node
import de.schauderhaft.degraph.configuration.LayeringConstraint

@RunWith(classOf[JUnitRunner])
class CheckTest extends FunSuite with ShouldMatchers {
    val mod = "mod"

    test("configuration contains the classpath") {
        Check.classpath.classpath.value should include("log4j")
    }

    //        forType("module").allow("a", "b", any("x","y","z"), none("u","v","w",)"c", )
    //        allowDirectOnly

    private def mockConfig(conns: Traversable[(Node, Node)]) = {
        val graph = new Graph()

        for ((a, b) <- conns) graph.connect(a, b)

        val analyzer = new AnalyzerLike {
            def analyze(
                sourceFolder: String,
                categorizer: Node => Node,
                filter: Node => Boolean): Graph = graph
        }

        new Configuration(
            classpath = Some("x"),
            analyzer = analyzer)
    }

    def ascending(st: String, includeEqual: Boolean = true) = for {
        x <- 'a' to 'c'
        y <- 'a' to 'c'
        if (x < y || x == y && includeEqual)
    } yield (SimpleNode(st, x.toString), SimpleNode(st, y.toString))

    def descending(st: String) = for {
        x <- 'a' to 'c'
        y <- 'a' to 'c'
        if (x > y)
    } yield (SimpleNode(st, x.toString), SimpleNode(st, y.toString))

    test("matcher accepts violation free graph for simple layering") {
        val conf = mockConfig(ascending(mod)).forType(mod).allow("a", "b", "c")
        Check.violationFree(conf).matches should be(true)
    }

    for (illegalCon <- descending(mod))
        test("matcher detects violations for simple layering %s".format(illegalCon)) {
            val conf = mockConfig(Set(illegalCon)).forType(mod).allow("a", "b", "c")
            Check.violationFree(conf).matches should be(false)
        }

    for (illegalCon <- descending(mod))
        test("matcher detects cycles %s".format(illegalCon)) {
            val conf = mockConfig(ascending(mod) :+ illegalCon).forType(mod).allow("a", "b", "c")
            Check.violationFree(conf).matches should be(false)
        }

    for {
        c <- 'a' to 'c'
        con <- Set(
            (SimpleNode(mod, "a"), SimpleNode("x", c.toString)),
            (SimpleNode("x", c.toString), SimpleNode(mod, "a")))
    } test("constraint ignores connection to and from other slices %s".format(con)) {
        val conf = mockConfig(Set(con)).forType(mod).allow("a", "b", "c")
        Check.violationFree(conf).matches should be(true)
    }

    for {
        c <- 'a' to 'c'
        con <- Set(
            (SimpleNode("x", "a"), SimpleNode("x", c.toString)),
            (SimpleNode("x", c.toString), SimpleNode("x", "a")))
    } test("constraint ignores connections in other slices %s".format(con)) {
        val conf = mockConfig(Set(con)).forType(mod).allow("a", "b", "c")
        Check.violationFree(conf).matches should be(true)
    }

    test("works without constraints") {
        val conf = mockConfig(ascending(mod))
        Check.violationFree(conf).matches should be(true)
    }

    for {
        c1 <- 'a' to 'c'
        c2 <- 'a' to 'c'
        val con = (SimpleNode(mod, c1.toString), SimpleNode(mod, c2.toString))
    } test("accepts unspecified dependencies %s ".format((c1, c2))) {
        val conf = mockConfig(Set(con)).forType(mod).allow("x", "b", "y")
        Check.violationFree(conf).matches should be(true)
    }

    for {
        con <- ascending(mod, false) ++ descending(mod)
    } test("multiple constraints both get checked %s".format(con)) {
        val conf = mockConfig(Set(con)).
            forType(mod).allow("a", "b", "c").
            forType(mod).allow("c", "b", "a")
        Check.violationFree(conf).matches should be(false)
    }

    for (i <- 0 to 4)
        test("allowDirect allows direct dependencies between layers, including unspecified %d".format(i)) {
            def n(c: Char) = SimpleNode(mod, c.toString)
            val chars = 'a' to 'f'
            val deps = Set(
                (n(chars(i)), n(chars(i + 1))),
                (n(chars(i)), n(chars(i))))
            val conf = mockConfig(
                deps).
                forType(mod).allowDirect("b", "c", "d")
            withClue(deps) {
                Check.violationFree(conf).matches should be(true)
            }
        }

    for (i <- 1 to 2)
        test("allowDirect disallows inverse dependencies %d".format(i)) {
            def n(c: Char) = SimpleNode(mod, c.toString)
            val chars = 'b' to 'd'
            val deps = Set(
                (n(chars(i)), n(chars(i - 1))))
            val conf = mockConfig(
                deps).
                forType(mod).allowDirect("b", "c", "d")
            withClue(deps) {
                Check.violationFree(conf).matches should be(false)
            }
        }

    for (i <- 0 to 2)
        test("allowDirect disallows dependencies skipping layers, including unspecified %d".format(i)) {
            def n(c: Char) = SimpleNode(mod, c.toString)
            val chars = 'a' to 'e'
            val deps = Set(
                (n(chars(i)), n(chars(i + 2))))
            val conf = mockConfig(
                deps).
                forType(mod).allowDirect("b", "c", "d")
            withClue(deps) {
                Check.violationFree(conf).matches should be(false)
            }
        }

    test("strictly does not allow dependencies from or to others") {
        pending
    }

    test("any in group") {
        pending
    }

}
