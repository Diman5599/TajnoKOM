package priv.dimitrije.tajnokom;

import androidx.annotation.NonNull;

public class ActiveChatModel {
    private String contactName;
    private String lastMessage;
    private boolean isLastSent;
    private int unreadCount;

    public ActiveChatModel(){
    }


    @NonNull
    @Override
    public String toString() {
        return ">>>>>>>" + contactName + "\n"
                + (isLastSent ? "->" : "" + lastMessage + " (" + unreadCount + ")\n");
    }
}
