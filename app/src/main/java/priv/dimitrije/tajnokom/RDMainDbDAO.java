package priv.dimitrije.tajnokom;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

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

    @Query("SELECT BuddyId FROM REBuddy WHERE BuddyId > 1")
    List<Integer> getAllBuddyIds();

    @Query("SELECT * FROM REBuddy WHERE BuddyId = :id")
    REBuddy getBuddyById(int id);

    @Query("SELECT BuddyId FROM REBuddy WHERE BuddyNo = :no")
    int getBuddyIdByNo(String no);

    @Update
    void updateBuddy(REBuddy reBuddy);

    @Insert
    void insertBuddy(REBuddy buddy);

    @Query("DELETE FROM REBuddy")
    void deleteAllBuddies();

    @Query("DELETE FROM REBuddy WHERE BuddyId = :id")
    void deleteAllBuddies(int id);

    @Delete
    void deleteBuddy(REBuddy buddy);

    @Delete
    void deleteBuddies(List<REBuddy> buddies);

    //messages
    @Query("SELECT * FROM (SELECT ROWID, MessageId, contactId, msgText, sent, read, time FROM REMessage WHERE contactId = :senderId ORDER BY ROWID DESC LIMIT :count) ORDER BY ROWID ASC")
    List<REMessage> getMsgsFrom(int senderId, int count);

    @Insert
    void insertMessage(REMessage reMessage);

    @Delete
    void deleteMessage(REMessage message);

    @Update
    void updateMessages(List<REMessage> msgs);

    @Query("SELECT * FROM REMessage WHERE read = 0 AND contactId = :cId")
    List<REMessage> getUnread(int cId);

    @Query("SELECT * FROM REMessage WHERE contactId = :cId" +
            " ORDER BY ROWID DESC LIMIT 1")
    REMessage getLastMsgFromBuddy(int cId);

    @Query("SELECT COUNT(MessageId) FROM REMessage WHERE read = 0 AND contactId = :cId")
    int getUnreadCountFromBuddyId(int cId);

    @Query("SELECT DISTINCT BuddyId FROM REBuddy INNER JOIN REMessage ON BuddyId = contactId WHERE BuddyId > 1")
    List<Integer> getAllBuddyIdsWithMessages();

    @Query("DELETE FROM REMessage WHERE contactId = :cId")
    void deleteMessagesOfBuddy(int cId);
}