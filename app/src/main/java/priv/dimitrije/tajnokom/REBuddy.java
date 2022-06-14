package priv.dimitrije.tajnokom;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;

import java.io.Serializable;

@Entity(indices = {@Index(value = "BuddyNo", unique = true)})
public class REBuddy implements Serializable {
    @PrimaryKey(autoGenerate = true)
    public int BuddyId;

    @ColumnInfo(name="BuddyName")
    public String BuddyName;

    @ColumnInfo(name="BuddyNo")
    public String BuddyNo;

    @ColumnInfo(name = "isSaved")
    public boolean isSaved;
}
