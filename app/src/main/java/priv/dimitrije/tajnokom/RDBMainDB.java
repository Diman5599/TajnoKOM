package priv.dimitrije.tajnokom;

import androidx.room.Database;
import androidx.room.RoomDatabase;

@Database(entities = {RELogInCreds.class, REBuddy.class, REMessage.class}, version = 10)
public abstract class RDBMainDB extends RoomDatabase {
    public abstract RDMainDbDAO getDAO();
}
