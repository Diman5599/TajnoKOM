package priv.dimitrije.tajnokom;

import androidx.annotation.NonNull;

import org.pjsip.pjsua2.Buddy;

public class MyBuddy extends Buddy {



    @Override
    public void onBuddyState() {
        super.onBuddyState();
        try {
            int status = this.getInfo().getPresStatus().getStatus();
            if(MessagesActivity.tvChatContactStatus != null) MessagesActivity.tvChatContactStatus.setText(MessagesActivity.getStatus(status));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @NonNull
    @Override
    public String toString() {
        try {
            return this.getInfo().getUri();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "buddy exception";
    }

    @Override
    protected void finalize() {
        this.delete();
        super.finalize();
        System.out.println("FFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFF: MyBuddy finalized!\n");
    }
}
