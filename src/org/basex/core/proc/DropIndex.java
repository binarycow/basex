package org.basex.core.proc;

import static org.basex.Text.*;
import static org.basex.core.Commands.*;
import static org.basex.data.DataText.*;
import java.io.IOException;
import org.basex.BaseX;
import org.basex.core.Process;
import org.basex.core.Commands.CmdIndex;
import org.basex.data.Data;
import org.basex.index.IndexToken;
import org.basex.io.IO;

/**
 * Evaluates the 'drop index' command.
 *
 * @author Workgroup DBIS, University of Konstanz 2005-08, ISC License
 * @author Christian Gruen
 */
public final class DropIndex extends Process {
  /** Index type. */
  protected final CmdIndex type;

  /**
   * Constructor.
   * @param t index type
   */
  public DropIndex(final CmdIndex t) {
    super(DATAREF);
    type = t;
  }
  
  @Override
  protected boolean exec() {
    final Data data = context.data();
    switch(type) {
      case TEXT:
        data.meta.txtindex = false;
        return drop(IndexToken.Type.TXT, DATATXT);
      case ATTRIBUTE:
        data.meta.atvindex = false;
        return drop(IndexToken.Type.ATV, DATAATV);
      case FULLTEXT:
        data.meta.ftxindex = false;
        return drop(IndexToken.Type.FTX, DATAFTX);
      default:
        return false;
    }
  }

  /**
   * Drops the specified index.
   * @param index index type
   * @param pat pattern
   * @return success of operation
   */
  private boolean drop(final IndexToken.Type index, final String pat) {
    try {
      final Data data = context.data();
      data.flush();
      data.closeIndex(index);
      return IO.dbdelete(data.meta.dbname, pat + "." + IO.BASEXSUFFIX) ?
          info(DBDROP, perf.getTimer()) : error(DBDROPERR);
    } catch(final IOException ex) {
      BaseX.debug(ex);
      return error(ex.getMessage());
    }
  }
  
  @Override
  public String toString() {
    return Cmd.DROP.name() + " " + CmdDrop.INDEX + " " + type;
  }
}
