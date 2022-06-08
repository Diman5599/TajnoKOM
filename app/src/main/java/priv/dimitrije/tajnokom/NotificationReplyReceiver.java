package priv.dimitrije.tajnokom;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.RemoteInput;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.service.notification.StatusBarNotification;

import androidx.annotation.RequiresApi;
import androidx.core.app.NotificationCompat;

import org.pjsip.pjsua2.BuddyConfig;
import org.pjsip.pjsua2.SendInstantMessageParam;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class NotificationReplyReceiver extends BroadcastReceiver{
    @RequiresApi(api = Build.VERSION_CODES.P)
    @Override
    public void onReceive(Context context, Intent intent) {
        String msg = RemoteInput.getResultsFromIntent(intent).getString("REMOTE_MSG");
        System.out.println("______________________________________________RECEUVED: " + msg);
        int notificationId = intent.getExtras().getInt("notification_id");
        String buddyNo = intent.getExtras().getString("buddyNo");
        String buddyName = intent.getExtras().getString("buddName");

        List<REMessage> unread =  App.getInstance().unreadMessages.get(App.getInstance().activeContactId);
        if(unread != null) {
            for(REMessage m : unread) m.read = true;
            unread.clear();
        }

        NotificationManager notificationManager = (NotificationManager) context.getSystemService(context.NOTIFICATION_SERVICE);

        Notification.MessagingStyle style = null;
        StatusBarNotification[] nots = notificationManager.getActiveNotifications();
        Notification n = null;


        for(StatusBarNotification s : nots){
            if(s.getId() == notificationId) {
                n = s.getNotification();
                break;
            }
        }

        Notification.Builder updater = Notification.Builder.recoverBuilder(context, n);

        style = (Notification.MessagingStyle) updater.getStyle();

        Notification.MessagingStyle.Message message =
                new Notification.MessagingStyle.Message(msg, Long.valueOf(java.time.LocalTime.now().toSecondOfDay()) , "Ја");

        style.addMessage(message);

        Intent receivedMessageIntent = new Intent(context, MessagesActivity.class);
        receivedMessageIntent.putExtra("buddyName", buddyName);
        receivedMessageIntent.putExtra("buddyNo", buddyNo);

        PendingIntent pendingIntent = PendingIntent.getActivity(context, 102, receivedMessageIntent, 0);

        updater.setSmallIcon(R.mipmap.tkomico)
                .setContentIntent(pendingIntent)
                .setStyle(style);


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
        reMessage.time = LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm"));

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
