package priv.dimitrije.tajnokom;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class ShutdownReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        try {
            App.getInstance().logOut();
            App.getInstance().stopForeground(0);
            Intent intent1 = new Intent(context, App.class);
            context.stopService(intent1);
        }catch (Exception e){
            e.printStackTrace();
        }
        System.exit(0);
    }
}