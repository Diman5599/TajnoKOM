package priv.dimitrije.tajnokom;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class ShutdownReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        App.getInstance().logOut();
        App.getInstance().stopForeground(0);
        System.exit(0);
    }
}