package priv.dimitrije.tajnokom;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.RemoteInput;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.service.notification.StatusBarNotification;

import androidx.core.app.NotificationCompat;

import org.pjsip.pjsua2.BuddyConfig;
import org.pjsip.pjsua2.SendInstantMessageParam;

public class NotificationReplyReceiver extends BroadcastReceiver{
    @Override
    public void onReceive(Context context, Intent intent) {
        String msg = RemoteInput.getResultsFromIntent(intent).getString("REMOTE_MSG");
        System.out.println("______________________________________________RECEUVED: " + msg);
        int notificationId = intent.getExtras().getInt("notification_id");
        String buddyNo = intent.getExtras().getString("buddyNo");
        String buddyName = intent.getExtras().getString("buddName");

        NotificationManager notificationManager = (NotificationManager) context.getSystemService(context.NOTIFICATION_SERVICE);
        NotificationCompat.MessagingStyle style = null;
        StatusBarNotification[] nots = notificationManager.getActiveNotifications();
        Notification n = null;
        for(StatusBarNotification s : nots){
            if(s.getId() == notificationId) {
                n = s.getNotification();
                break;
            }
        }

        style = NotificationCompat.MessagingStyle.extractMessagingStyleFromNotification(n);
        NotificationCompat.MessagingStyle.Message message =
                new NotificationCompat.MessagingStyle.Message(msg, Long.valueOf(java.time.LocalTime.now().toSecondOfDay()) , "Ја");

        Intent receivedMessageIntent = new Intent(context, MessagesActivity.class);
        receivedMessageIntent.putExtra("buddyName", buddyName);
        receivedMessageIntent.putExtra("buddyNo", buddyNo);

        PendingIntent pendingIntent = PendingIntent.getActivity(context, 102, receivedMessageIntent, 0);

        //*****Odgovarenje iz notifikacije****
        androidx.core.app.RemoteInput remoteInput = new androidx.core.app.RemoteInput.Builder("REMOTE_MSG").setLabel("Унесите поруку...").build();

        Intent replyIntent = new Intent(context, NotificationReplyReceiver.class);
        replyIntent.putExtra("notification_id", notificationId);
        replyIntent.putExtra("buddyName", buddyName);
        replyIntent.putExtra("buddyNo", buddyNo);

        replyIntent.getStringExtra("REMOTE_REPLY");
        PendingIntent replyPendingIntent = PendingIntent.getBroadcast(context, 106, replyIntent, PendingIntent.FLAG_UPDATE_CURRENT);
        NotificationCompat.Action.Builder rplyBuilder = new NotificationCompat.Action.Builder(null, "Одговори", replyPendingIntent)
                .addRemoteInput(remoteInput)
                .setAllowGeneratedReplies(true);

        NotificationCompat.Action rplyAction = rplyBuilder.build();
        //*********************************

        NotificationCompat.Builder updater = new NotificationCompat.Builder(context, "MsgChan")
                .setSmallIcon(R.mipmap.tkomico)
                .setContentIntent(pendingIntent)
                .setStyle(style.addMessage(message))
                .addAction(rplyAction)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT);


        //TODO: posalji poruku kontaktu
        try {
            App.getInstance().endpoint.libRegisterThread(Thread.currentThread().getName());
        }catch (Exception e){
            e.printStackTrace();
        }

        SendInstantMessageParam prm = new SendInstantMessageParam();
        prm.setContent(msg);
        TajniBuddy bud = new TajniBuddy();

        try {
            BuddyConfig buddyConfig = new MyBuddyCfg();
            buddyConfig.setUri("sip:"+buddyNo+"@"+App.getInstance().domain);
            System.out.println("..........................................................URI: " + "sip:"+buddyNo+"@"+App.getInstance().domain + "\n");
            bud.create(App.getInstance().usrAccount, buddyConfig);
            bud.sendInstantMessage(prm);
            buddyConfig.delete();
        } catch (Exception e) {
            e.printStackTrace();
        }
        REMessage reMessage = new REMessage();
        reMessage.msgText = prm.getContent();
        reMessage.contactId = notificationId;
        reMessage.sent = true;

        Thread writeMsgToDb = new Thread(() -> {
            App.getInstance().getDb().getDAO().insertMessage(reMessage);
            App.getInstance().closeDb();
        });
        writeMsgToDb.start();

        prm.delete();
        bud.delete();

        notificationManager.notify(notificationId, updater.build());
    }
}
