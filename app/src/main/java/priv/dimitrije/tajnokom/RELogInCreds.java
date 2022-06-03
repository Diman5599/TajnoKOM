package priv.dimitrije.tajnokom;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

//tabela Room-a koja sadrzi zapis o zapamcenom prijavljivanju
@Entity
public class RELogInCreds {
    @PrimaryKey
    public int CredId;

    @ColumnInfo(name = "domain")
    public String domainName;

    @ColumnInfo(name = "user")
    public String username;

    @ColumnInfo(name = "password")
    public String password;
}
