package priv.dimitrije.tajnokom;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.core.app.NotificationCompat;
import android.app.RemoteInput;
import androidx.room.Room;
import androidx.room.migration.Migration;
import androidx.sqlite.db.SupportSQLiteDatabase;

import org.pjsip.pjsua2.AccountInfo;
import org.pjsip.pjsua2.Call;
import org.pjsip.pjsua2.Endpoint;
import org.pjsip.pjsua2.EpConfig;
import org.pjsip.pjsua2.OnInstantMessageParam;
import org.pjsip.pjsua2.TransportConfig;
import org.pjsip.pjsua2.UaConfig;
import org.pjsip.pjsua2.Version;
import org.pjsip.pjsua2.pjsip_transport_type_e;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.function.Predicate;

public class App extends Service {
    private AESEncryptor encryptor;

    public static Call activeCall;

    public static int FAILED_STATUS;
    public Endpoint endpoint;
    public MyAccount usrAccount;

    public String domain;
    public String username;
    public String password;

    //Lista kontakata
    public List<REBuddy> contacts;
    //Lista postijecih razgovora
    public List<ActiveChatModel> activeChats;

    public RDBMainDB mainDB = null;
    public AccountInfo accInfo;
    public MyBuddy activeBuddy;

    private static App instance = null;

    //sadrzaj ceta u kome je korisnik trenutno, kako bi se azurirao pri stizanju poruke
    public LinkedList<REMessage> activeChatList;
    //koji je kontakt u pitanju
    public int activeContactId;

    private int notificationId = 2;
    private boolean logedin;

    public HashMap<Integer, List<REMessage>> unreadMessages;


    public App(){
        logedin = false;
    }

    boolean logIn(RELogInCreds reLogInCreds){
        logedin = (new LogInManager()).logIn(reLogInCreds);

        try {
            endpoint.libRegisterThread(Thread.currentThread().getName());
        } catch (Exception e) {
            e.printStackTrace();
        }

        if(logedin){
            unreadMessages = new HashMap<>();

            Intent notificationIntent = new Intent(this, MainActivity.class);

            PendingIntent pendingIntent = PendingIntent.getActivity(this, 0,
                    notificationIntent, 0);


            Intent ztopIntent = new Intent(this, ShutdownReceiver.class);
            ztopIntent.setAction("ZTOP");
            PendingIntent stopFromNotificationIntent = PendingIntent.getBroadcast(getApplicationContext(), 105, ztopIntent, 0);

            NotificationCompat.Action action = new NotificationCompat.Action(null, "Заустави", stopFromNotificationIntent);

            NotificationCompat.Builder builder = new NotificationCompat.Builder(this, "foreground_chan")
                    .setSmallIcon(R.mipmap.tkomico)
                    .setColor(this.getColor(R.color.bordeaux))
                    .setContentTitle("ТајноКОМ")
                    .setContentText("Пријављени сте на сервер " + domain)
                    .setContentIntent(pendingIntent)
                    .addAction(action)
                    .setPriority(NotificationCompat.PRIORITY_HIGH);

            NotificationManager notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
            notificationManager.notify(1, builder.build());
        }
        return logedin;
    }

    boolean isLogedin(){
        return logedin;
    }

    private REMessage receivedMessage;
    public void makeNotification(OnInstantMessageParam msg){
        receivedMessage = new REMessage();
        receivedMessage.msgText = msg.getMsgBody();
        receivedMessage.sent = false;
        receivedMessage.read = false;
        receivedMessage.time = LocalDateTime.now().format(DateTimeFormatter.ofPattern("uuuu-MM-dd HH:mm:ss:SSS"));

        //start task for writing
        WriteNewMessageTask writeTask = new WriteNewMessageTask();
        writeTask.execute(msg);
    }

    private String buddyName;
    @RequiresApi(api = Build.VERSION_CODES.P)
    private void finnishNotification(OnInstantMessageParam msg){
        if(MessagesRVAdapter.getInstance() != null) MessagesRVAdapter.notifyMe();
        else {
            NotificationManager notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);

            notificationManager.cancel(receivedMessage.contactId);

            String buddyNo = msg.getFromUri().substring(5, msg.getFromUri().indexOf('@'));
            if (buddyName == null || buddyName.equals("")) buddyName = buddyNo;
            Intent receivedMessageIntent = new Intent(this, MessagesActivity.class);
            receivedMessageIntent.putExtra("buddyName", buddyName);
            receivedMessageIntent.putExtra("buddyNo", buddyNo);
            receivedMessageIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
            receivedMessageIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

            PendingIntent pendingIntent = PendingIntent.getActivity(this, 102, receivedMessageIntent, 0);

            //Odgovarenje iz notifikacije
            RemoteInput remoteInput = new RemoteInput.Builder("REMOTE_MSG").setLabel("Унесите поруку...").build();

            Intent replyIntent = new Intent(this, NotificationReplyReceiver.class);
            replyIntent.putExtra("buddyName", buddyName);
            replyIntent.putExtra("buddyNo", buddyNo);
            replyIntent.putExtra("notification_id", receivedMessage.contactId);
            replyIntent.getStringExtra("REMOTE_REPLY");
            PendingIntent replyPendingIntent = PendingIntent.getBroadcast(this, 106, replyIntent, PendingIntent.FLAG_UPDATE_CURRENT);
            Notification.Action.Builder rplyBuilder = new Notification.Action.Builder(null, "Одговори", replyPendingIntent)
                    .addRemoteInput(remoteInput)
                    .setAllowGeneratedReplies(true);

            Notification.Action rplyAction = rplyBuilder.build();
            //*********************************

            Notification.MessagingStyle style = new Notification.MessagingStyle(buddyName);
            for(REMessage m : unreadMessages.get(receivedMessage.contactId)){
                Notification.MessagingStyle.Message styleMessage = new Notification.MessagingStyle.Message(m.msgText, LocalTime.now().getHour(), buddyName);
                style.addMessage(styleMessage);
            }

            Notification.Builder builder = new Notification.Builder(this, "MsgChan").setSmallIcon(R.mipmap.tkomico)
                    .setColor(this.getColor(R.color.bordeaux))
                    .setContentIntent(pendingIntent)
                    .addAction(rplyAction)
                    .setStyle(style);


            Notification notification = builder.build();

            notificationManager.notify(receivedMessage.contactId, notification);
        }
        msg.delete();
    }

    public static Bundle logInLock;
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        activeCall = null;

        //pjTrash = new LinkedList<>();
        Bundle args = intent.getExtras();
        domain = args.getString("domain");
        password = args.getString("pass");
        username = args.getString("user");

        initSipEndpoint(this);

        activeChatList = new LinkedList<>();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = "messages";
            String description = "Message notif channel";
            int importance = NotificationManager.IMPORTANCE_HIGH;
            NotificationChannel channel = new NotificationChannel("MsgChan", name, importance);
            channel.enableLights(true);
            channel.setLightColor(Color.RED);
            channel.setDescription(description);

            // Register the channel with the system; you can't change the importance
            // or other notification behaviors after this
            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = "foreground";
            String description = "Foreground service notif channel";
            int importance = NotificationManager.IMPORTANCE_HIGH;
            NotificationChannel channel = new NotificationChannel("foreground_chan", name, importance);
            channel.setDescription(description);
            // Register the channel with the system; you can't change the importance
            // or other notification behaviors after this
            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }

        /*if(db == null) {
            RDBMainDB db = Room.databaseBuilder(this, RDBMainDB.class, "tajnokomDb").fallbackToDestructiveMigration().build();
            this.db = db;
        }*/

        DbTask dbTask = new DbTask();
        dbTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);

        instance = this;

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, "foreground_chan")
                .setSmallIcon(R.mipmap.tkomico)
                .setColor(this.getColor(R.color.bordeaux))
                .setContentTitle("ТајноКОМ")
                .setContentText("Пријава у току...")
                .setPriority(NotificationCompat.PRIORITY_HIGH);

        startForeground(1, builder.build());

        return Service.START_REDELIVER_INTENT;
    }

    public void logOut(){
        try {
            activeBuddy.delete();
        }catch (NullPointerException e){
            System.out.println("NO ACTIVE BUDDY");
        }

        try {
            endpoint.libDestroy();
        } catch (Exception e) {
            e.printStackTrace();
        }
        endpoint.delete();
        App.instance = null;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        logOut();
        System.out.println("APP DESTROYED!");
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    App(Context context){
        if(mainDB == null) {
            RDBMainDB db = Room.databaseBuilder(this, RDBMainDB.class, "tajnokomDb").build();
            this.mainDB = db;
        }
    }

    public RDBMainDB getDb(){
        RDBMainDB db = Room.databaseBuilder(this, RDBMainDB.class, "tajnokomDb").build();
        this.mainDB = db;
        return mainDB;
    }

    public void destroyInstance(){
        instance = null;
    }

    public static App getInstance(){
        return instance;
    }

    public App initSipEndpoint(Context context){
           // if(instance == null) {
               // instance = new App();
                endpoint = new Endpoint();

                try {
                    endpoint.libCreate();
                } catch (Exception e) {
                    e.printStackTrace();
                }

                EpConfig epConfig = new EpConfig();
                TransportConfig sipTpConfig = new TransportConfig();

                sipTpConfig.setPort(50603);

                Version version = endpoint.libVersion();
                UaConfig uaConfig = new UaConfig();
                uaConfig.setUserAgent("TajnoKOM via Pjsua2 v" + version.getFull());
                //pjTrash.add(version);
                version.delete();

                epConfig.setUaConfig(uaConfig);

                try {
                    endpoint.libInit(epConfig);
                    endpoint.libStart();
                } catch (Exception e) {
                    e.printStackTrace();
                }

                //pjTrash.add(uaConfig);
                //pjTrash.add(epConfig);
                uaConfig.delete();
                epConfig.delete();

                try {
                    endpoint.transportCreate(pjsip_transport_type_e.PJSIP_TRANSPORT_TCP, sipTpConfig);
                    endpoint.transportCreate(pjsip_transport_type_e.PJSIP_TRANSPORT_UDP, sipTpConfig);
                    sipTpConfig.setPort(5061);
                    endpoint.transportCreate(pjsip_transport_type_e.PJSIP_TRANSPORT_TLS, sipTpConfig);
                } catch (Exception e) {
                    System.out.println(e.getMessage());
                }
          //  }
                //pjTrash.add(sipTpConfig);
                sipTpConfig.delete();
                sipTpConfig = null;

        return instance;
    }

    public AESEncryptor getEncryptor(){
        return encryptor;
    }

    public class DbTask extends AsyncTask<Void, Void, Void>{

        @Override
        protected Void doInBackground(Void... voids) {
            try {
                //registrovanje trenutne niti u okviru pjlib-a
                instance.endpoint.libRegisterThread(Thread.currentThread().getName());
            } catch (Exception e) {
                e.printStackTrace();
            }
            if(!logedin){
                /*RDBMainDB db = App.getDb();
                List<RELogInCreds> creds = db.getDAO().getAllLogins();
                if(!creds.isEmpty()){
                    logIn(creds.get(0));
                    App.domain = creds.get(0).domainName;
                }*/
                RDBMainDB db = getDb();
                List<REBuddy> buddies = db.getDAO().getAllBuddies();
                if(buddies.isEmpty()){
                    REBuddy b = new REBuddy();
                    b.BuddyName = "";
                    b.BuddyNo = "/";
                    getDb().getDAO().insertBuddy(b);
                }
                db.close();
                RELogInCreds reLogInCreds = new RELogInCreds();
                reLogInCreds.domainName = domain;
                reLogInCreds.password = password;
                reLogInCreds.username = username;

                encryptor = new AESEncryptor();

                logIn(reLogInCreds);
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void unused) {
            if(logInLock != null) {
                synchronized (logInLock) {
                    logInLock.notifyAll();
                }
            }
            super.onPostExecute(unused);
        }
    }

    public void closeDb() {
        this.mainDB.close();
    }

    class WriteNewMessageTask extends AsyncTask<OnInstantMessageParam, OnInstantMessageParam, OnInstantMessageParam> {

        @Override
        protected OnInstantMessageParam doInBackground(OnInstantMessageParam... params) {
            int contactId;
            RDBMainDB db = App.getInstance().getDb();
            System.out.println(params[0].getFromUri() + " PPPpppPPPPPppppPPPpppppPPPPppppPPPPppppPPPPppp");
            String num = params[0].getFromUri()
                    .substring(5, params[0].getFromUri().indexOf('@'));

            contactId = db.getDAO()
                    .getBuddyIdByNo(num);
            App.getInstance().closeDb();

            REBuddy buddy = db.getDAO().getBuddyById(contactId);

            if(buddy == null){
                buddy = new REBuddy();
                buddy.BuddyName = "";
                buddy.BuddyNo = num;
                db.getDAO().insertBuddy(buddy);
                contactId = db.getDAO().getBuddyIdByNo(num);
            }else{
                buddyName = buddy.BuddyName;
            }

            if (unreadMessages.get(contactId) == null) unreadMessages.put(contactId, new LinkedList<>());
            unreadMessages.get(contactId).add(receivedMessage);

            receivedMessage.contactId = contactId;
            db.getDAO().insertMessage(receivedMessage);

            final int id = contactId;
            if(activeChats != null) {
                activeChats.removeIf(new Predicate<ActiveChatModel>() {
                    @Override
                    public boolean test(ActiveChatModel activeChatModel) {
                        return activeChatModel.getContact().BuddyId == id;
                    }
                });
                activeChats.add(0, new ActiveChatModel(contactId, db));
            }

            db.close();

            if (contactId == activeContactId) {
                activeChatList.add(receivedMessage);
            }
            return params[0];
        }

        @RequiresApi(api = Build.VERSION_CODES.P)
        @Override
        protected void onPostExecute(OnInstantMessageParam prm) {
            finnishNotification(prm);
            ChatRVAdapter.getInstance().notifyDataSetChanged();
        }
    }
}