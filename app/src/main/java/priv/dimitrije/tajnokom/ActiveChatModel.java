package priv.dimitrije.tajnokom;

import androidx.annotation.NonNull;

public class ActiveChatModel {
    private REBuddy contact;
    private REMessage lastMessage;
    private boolean isLastSent;
    private int unreadCount;

    public ActiveChatModel(int contactId, RDBMainDB db){
        contact = db.getDAO().getBuddyById(contactId);
        lastMessage = db.getDAO().getLastMsgFromBuddy(contactId);
        isLastSent = lastMessage.sent;
        unreadCount = db.getDAO().getUnreadCountFromBuddyId(contactId);
    }

    public int getUnreadCount() {
        return unreadCount;
    }

    public void setUnreadCount(int unreadCount) {
        this.unreadCount = unreadCount;
    }

    public ActiveChatModel(){
    }


    public REBuddy getContact() {
        return contact;
    }

    public void setContact(REBuddy contact) {
        this.contact = contact;
    }

    public REMessage getLastMessage() {
        return lastMessage;
    }

    public void setLastMessage(REMessage lastMessage) {
        this.lastMessage = lastMessage;
    }

    public boolean isLastSent() {
        return isLastSent;
    }

    public void setLastSent(boolean lastSent) {
        isLastSent = lastSent;
    }

    @NonNull
    @Override
    public String toString() {
        return ">>>>>>>>>>>>>>>>>>>>>>>" + (contact.BuddyName.equals("") ? "<" + contact.BuddyNo + ">" : contact.BuddyName)+ "<<<<<<<<<<<<<<<<<<<<<<<<<<<\n"
                + (isLastSent ? "->" : "") + lastMessage.msgText + " (" + unreadCount + ")\n";
    }
}
