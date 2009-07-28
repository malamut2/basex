package org.basex.test.query;

import org.basex.core.Context;
import org.basex.core.Process;
import org.basex.core.AProp;
import org.basex.core.Prop;
import org.basex.core.proc.CreateDB;
import org.basex.core.proc.DropDB;
import org.basex.core.proc.XQuery;
import org.basex.data.Nodes;
import org.basex.data.Result;
import org.basex.util.Performance;

/**
 * XPath Test class.
 *
 * @author Workgroup DBIS, University of Konstanz 2005-09, ISC License
 * @author Christian Gruen
 */
public final class QueryTest {
  /** Database Context. */
  private static final Context CONTEXT = new Context();
  /** Test instances. */
  private static final AbstractTest[] TESTS = {
    new SimpleTest(), new XPathMarkFTTest(), new FTTest()
  };
  /** Verbose flag. */
  private static final boolean VERBOSE = false;
  /** Test all flag. */
  private static final boolean ALL = true;
  /** Wrong results counter. */
  private int wrong;
  /** Query counter. */
  private int counter;

  /**
   * Main method of the test class.
   * @param args command line arguments (ignored)
   */
  public static void main(final String[] args) {
    new QueryTest();
  }

  /**
   * Constructor.
   */
  private QueryTest() {
    final Performance p = new Performance();
    final Prop prop = CONTEXT.prop;
    prop.set(Prop.TEXTINDEX, true);
    prop.set(Prop.ATTRINDEX, true);
    prop.set(Prop.MAINMEM, true);
    prop.set(Prop.CHOP, true);
    boolean ok = true;

    if(ALL) {
      // testing all kinds of combinations
      for(int a = 0; a < 2; a++) { prop.set(Prop.FTINDEX, a == 0);
        for(int b = 0; b < 2; b++) { prop.set(Prop.FTFUZZY, b == 0);
          for(int c = 0; c < 2; c++) { prop.set(Prop.FTST, c == 0);
            for(int d = 0; d < 2; d++) { prop.set(Prop.FTDC, d == 0);
              for(int e = 0; e < 2; e++) { prop.set(Prop.FTCS, e == 0);
                ok &= test(prop);
              }
            }
          }
        }
      }
    } else {
      // single test
      prop.set(Prop.FTINDEX, true);
      prop.set(Prop.FTFUZZY, false);
      prop.set(Prop.FTST, false);
      prop.set(Prop.FTDC, false);
      prop.set(Prop.FTCS, false);
      ok &= test(prop);
    }

    System.out.println(ok ? "All tests correct." : wrong + " Wrong results...");
    System.out.println(counter + " queries, " + p);
  }

  /**
   * Tests the specified query implementation.
   * @param prop database properties
   * @return true if everything went alright
   */
  private boolean test(final AProp prop) {
    boolean ok = true;
    for(final AbstractTest test : TESTS) ok &= test(test, test.details(prop));
    return ok;
  }

  /**
   * Tests the specified instance.
   * @param test instance
   * @param ext extended error info
   * @return true if everything went alright
   */
  private boolean test(final AbstractTest test, final String ext) {
    final String file = test.doc.replaceAll("\\\"", "\\\\\"");
    final String name = test.getClass().getSimpleName();
    Process proc = new CreateDB(file, name);
    boolean ok = proc.execute(CONTEXT);

    if(ok) {
      for(final Object[] qu : test.queries) {
        final boolean correct = qu.length == 3;
        final String query = qu[correct ? 2 : 1].toString();
        final String cmd = qu[0] + ": " + query;

        if(VERBOSE) err(cmd, ext);

        proc = new XQuery(query);
        counter++;

        if(proc.execute(CONTEXT)) {
          final Result val = proc.result();
          final Result cmp = correct ? (Result) qu[1] : null;
          if(val instanceof Nodes && cmp instanceof Nodes) {
            ((Nodes) cmp).data = ((Nodes) val).data;
          }
          if(!correct || !val.same(cmp)) {
            err(cmd, "  Right: " + (correct ? qu[1] : "error") + "\n  Found: " +
                val + (ext != null ? "\n  Flags: " + ext : ""));
            ok = false;
            wrong++;
            continue;
          }
        } else if(correct) {
          err(qu[0].toString(), proc.info() +
              (ext != null ? "\n  Flags: " + ext : ""));
          wrong++;
          ok = false;
        }
      }
    } else {
      err(proc.info(), null);
      wrong++;
    }

    new DropDB(name).execute(CONTEXT);
    return ok;
  }

  /**
   * Prints the specified string to standard output.
   * @param info short info
   * @param detail detailed info
   */
  private void err(final String info, final String detail) {
    System.out.println("- " + info);
    if(detail != null) System.out.println(detail);
  };
}
