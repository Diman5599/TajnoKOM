package priv.dimitrije.tajnokom;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;

import java.util.LinkedList;
import java.util.List;

@Dao
public interface RDMainDbDAO {
    //login creds
    @Query("SELECT * FROM RELogInCreds")
    List<RELogInCreds> getAllLogins();

    @Insert
    void insertLogin(RELogInCreds creds);

    @Delete
    void deleteLogin(RELogInCreds creds);

    //buddies
    @Query("SELECT * FROM REBuddy")
    List<REBuddy> getAllBuddies();

    @Query("SELECT * FROM REBuddy WHERE BuddyId > 1")
    List<REBuddy> getAllBuddiesS();

    @Query("SELECT * FROM REBuddy WHERE BUddyID = :id")
    REBuddy getBuddyById(int id);

    @Query("SELECT BuddyId FROM REBuddy WHERE BuddyNo = :no")
    int getBuddyIdByNo(String no);

    @Insert
    void insertBuddy(REBuddy buddy);

    @Query("DELETE FROM REBuddy")
    void deleteAllBuddies();

    @Query("DELETE FROM REBuddy WHERE BuddyId = :id")
    void deleteAllBuddies(int id);

    @Delete
    void deleteBuddy(REBuddy buddy);

    //messages
    @Query("SELECT * FROM (SELECT ROWID, MessageId, contactId, msgText, sent, read FROM REMessage WHERE contactId = :senderId ORDER BY ROWID DESC LIMIT :count) ORDER BY ROWID ASC")
    List<REMessage> getMsgsFrom(int senderId, int count);

    @Insert
    void insertMessage(REMessage reMessage);

    @Delete
    void deleteMessage(REMessage message);
}
