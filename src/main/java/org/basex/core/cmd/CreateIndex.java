package org.basex.core.cmd;

import static org.basex.core.Text.*;
import java.io.IOException;
import org.basex.core.CommandBuilder;
import org.basex.core.Main;
import org.basex.core.Prop;
import org.basex.core.User;
import org.basex.core.Commands.Cmd;
import org.basex.core.Commands.CmdCreate;
import org.basex.core.Commands.CmdIndex;
import org.basex.data.Data;
import org.basex.data.MemData;
import org.basex.data.Data.IndexType;

/**
 * Evaluates the 'create db' command and creates a new index.
 *
 * @author Workgroup DBIS, University of Konstanz 2005-10, ISC License
 * @author Christian Gruen
 */
public final class CreateIndex extends ACreate {
  /**
   * Default constructor.
   * @param type index type, defined in {@link CmdIndex}
   */
  public CreateIndex(final Object type) {
    super(DATAREF | User.WRITE, type != null ? type.toString() : null);
  }

  @Override
  protected boolean run() {
    final Data data = context.data;
    if(data instanceof MemData) return error(PROCMM);

    try {
      IndexType index = null;
      switch(getOption(CmdIndex.class)) {
        case TEXT:
          data.meta.txtindex = true;
          index = IndexType.TXT;
          break;
        case ATTRIBUTE:
          data.meta.atvindex = true;
          index = IndexType.ATV;
          break;
        case FULLTEXT:
          data.meta.ftxindex = true;
          data.meta.wildcards = prop.is(Prop.WILDCARDS);
          data.meta.stemming = prop.is(Prop.STEMMING);
          data.meta.casesens = prop.is(Prop.CASESENS);
          data.meta.diacritics = prop.is(Prop.DIACRITICS);
          data.meta.scoring = prop.num(Prop.SCORING);
          index = IndexType.FTX;
          break;
        case PATH:
          index = IndexType.PTH;
          break;
        default:
          return false;
      }

      index(index, data);
      data.flush();

      return info(DBINDEXED, perf);
    } catch(final IOException ex) {
      Main.debug(ex);
      return error(ex.getMessage());
    }
  }

  @Override
  public void build(final CommandBuilder cb) {
    cb.init(Cmd.CREATE + " " + CmdCreate.INDEX).args();
  }
}
