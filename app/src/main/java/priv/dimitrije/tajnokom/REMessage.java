package priv.dimitrije.tajnokom;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

@Entity
public class REMessage {
    @PrimaryKey(autoGenerate = true)
    public int MessageId;

    @ColumnInfo(name = "contactId")
    public int contactId;

    @ColumnInfo(name = "msgText")
    public String msgText;

    @ColumnInfo(name = "sent")
    public boolean sent;

    @ColumnInfo(name = "read")
    public boolean read;

    @ColumnInfo(name = "time")
    public String time;

    public REMessage(){
        sent = true;
        read = true;
    }
}
