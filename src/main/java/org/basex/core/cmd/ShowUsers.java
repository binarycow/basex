package org.basex.core.cmd;

import static org.basex.core.Text.*;
import java.io.IOException;
import java.util.ArrayList;
import org.basex.core.CommandBuilder;
import org.basex.core.Main;
import org.basex.core.Command;
import org.basex.core.User;
import org.basex.core.Commands.Cmd;
import org.basex.core.Commands.CmdShow;
import org.basex.data.Data;

/**
 * Evaluates the 'show users' command and shows existing users.
 *
 * @author Workgroup DBIS, University of Konstanz 2005-10, ISC License
 * @author Christian Gruen
 */
public final class ShowUsers extends Command {
  /**
   * Default constructor.
   */
  public ShowUsers() {
    this(null);
  }

  /**
   * Constructor, specifying a database.
   * @param db database (for showing users)
   */
  public ShowUsers(final String db) {
    super(User.ADMIN, db);
  }

  @Override
  protected boolean run() throws IOException {
    final String db = args[0];
    if(db == null) {
      out.println(context.users.info());
    } else {
      try {
        final Data data = Open.open(context, db);
        final ArrayList<User> loc = data.meta.users;
        for(int i = 0; i < loc.size(); i++) {
          final User us = context.users.get(loc.get(i).name);
          if(us == null) loc.remove(loc.get(i--));
        }
        out.println(data.meta.users.info());
        data.flush();
        Close.close(context, data);
        return true;
      } catch(final IOException ex) {
        Main.debug(ex);
        final String msg = ex.getMessage();
        return msg.isEmpty() ? error(DBOPENERR, db) : error(msg);
      }
    }
    return true;
  }

  @Override
  public void build(final CommandBuilder cb) {
    cb.init(Cmd.SHOW + " " + CmdShow.USERS).arg(ON, 0);
  }
}
