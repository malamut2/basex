package org.basex.core.proc;

import static org.basex.Text.*;
import java.io.FileNotFoundException;
import java.io.IOException;
import org.basex.BaseX;
import org.basex.build.Builder;
import org.basex.build.DiskBuilder;
import org.basex.build.MemBuilder;
import org.basex.build.Parser;
import org.basex.core.Process;
import org.basex.core.ProgressException;
import org.basex.core.Prop;
import org.basex.data.Data;
import org.basex.data.MemData;
import org.basex.data.Data.Type;
import org.basex.index.FTTrieBuilder;
import org.basex.index.FTFuzzyBuilder;
import org.basex.index.IndexBuilder;
import org.basex.index.ValueBuilder;

/**
 * Abstract class for database creation.
 *
 * @author Workgroup DBIS, University of Konstanz 2005-09, ISC License
 * @author Christian Gruen
 */
abstract class ACreate extends Process {
  /**
   * Constructor.
   * @param p command properties
   * @param a arguments
   */
  public ACreate(final int p, final String... a) {
    super(p, a);
  }

  /**
   * Builds and creates a new database instance.
   * @param p parser instance
   * @param db name of database; if set to null,
   * a main memory database instance is created
   * @return success of operation
   */
  protected final boolean build(final Parser p, final String db) {
    String err = null;
    Builder builder = null;
    try {
      exec(new Close());

      final boolean mem = db == null || prop.is(Prop.MAINMEM);
      if(!mem && context.pinned(db)) return error(DBINUSE);

      builder = mem ? new MemBuilder(p) : new DiskBuilder(p);
      progress(builder);
      final Data data = builder.build(db == null ? "" : db);
      builder = null;
      index(data);
      context.openDB(data);
      context.addToPool(data);
      return info(DBCREATED, db, perf.getTimer());
    } catch(final FileNotFoundException ex) {
      BaseX.debug(ex);
      err = BaseX.info(FILEWHICH, p.io);
    } catch(final ProgressException ex) {
      err = Prop.server ? SERVERTIME : CANCELCREATE;
    } catch(final IOException ex) {
      BaseX.debug(ex);
      final String msg = ex.getMessage();
      err = BaseX.info(msg != null ? msg : args[0]);
    } catch(final Exception ex) {
      BaseX.errln(ex);
      err = BaseX.info(CREATEERR, args[0]);
    }
    try {
      if(builder != null) builder.close();
    } catch(final IOException ex) {
      BaseX.debug(ex);
    }
    DropDB.drop(db, prop);

    return error(err);
  }

  /**
   * Builds the indexes.
   * @param data data reference
   * @throws IOException I/O exception
   */
  protected void index(final Data data) throws IOException {
    if(data.meta.txtindex) buildIndex(Type.TXT, data);
    if(data.meta.atvindex) buildIndex(Type.ATV, data);
    if(data.meta.ftxindex) buildIndex(Type.FTX, data);
  }

  /**
   * Builds the specified index.
   * @param i index to be built.
   * @param d data reference
   * @throws IOException I/O exception
   */
  protected void buildIndex(final Type i, final Data d) throws IOException {
    if(d instanceof MemData) return;
    final Prop pr = d.meta.prop;
    IndexBuilder builder = null;
    switch(i) {
      case TXT: builder = new ValueBuilder(d, true); break;
      case ATV: builder = new ValueBuilder(d, false); break;
      case FTX: builder = d.meta.ftfz ?
          new FTFuzzyBuilder(d, pr) : new FTTrieBuilder(d, pr); break;
      default: break;
    }
    progress(builder);
    d.setIndex(i, builder.build());
  }
}
