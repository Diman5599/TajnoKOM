package priv.dimitrije.tajnokom;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Build;
import android.os.IBinder;
import android.service.notification.StatusBarNotification;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.room.Room;

import org.pjsip.pjsua2.AccountConfig;
import org.pjsip.pjsua2.AccountInfo;
import org.pjsip.pjsua2.AuthCredInfo;
import org.pjsip.pjsua2.Endpoint;
import org.pjsip.pjsua2.EpConfig;
import org.pjsip.pjsua2.OnInstantMessageParam;
import org.pjsip.pjsua2.TransportConfig;
import org.pjsip.pjsua2.UaConfig;
import org.pjsip.pjsua2.Version;
import org.pjsip.pjsua2.pjsip_transport_type_e;

import java.util.LinkedList;
import java.util.List;

public class App extends Service {
    public static Context appContext;

    public Endpoint endpoint;
    public static MyAccount usrAccount;
    public static AccountConfig accfg;

    public static String domain;

    //Lista kontakata
    public static List<REBuddy> contacts;
    //Lista postijecih razgovora
    public static List<REBuddy> activeChats;

    public static EpConfig epConfig ;
    public static TransportConfig sipTpConfig;

    public RDBMainDB db = null;
    public static AccountInfo accInfo;
    public static AuthCredInfo aci;
    public static TajniBuddy activeBuddy;

    private static App instance = null;
    private static App helpInstance = null;

    public static ChatRVAdapter chatRVAdapter;

    //public List<Object> pjTrash;


    //sadrzaj ceta u kome je korisnik trenutno, kako bi se azurirao pri stizanju poruke
    public static LinkedList<REMessage> activeChatList;
    //koji je kontakt u pitanju
    public static int activeContactId;

    private int notificationId = 2;
    private boolean logedin;


    public App(){
        logedin = false;
    }

    boolean logIn(RELogInCreds reLogInCreds){
        logedin = (new LogInManager()).logIn(reLogInCreds);
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

        //start task for writing
        WriteNewMessageTask writeTask = new WriteNewMessageTask();
        writeTask.execute(msg);
    }

    private String buddyName;
    private void finnishNotification(OnInstantMessageParam msg){
        if(MessagesRVAdapter.getInstance() != null) MessagesRVAdapter.notifyMe();
        else {
            NotificationManager notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);

            String buddyNo = msg.getFromUri().substring(5, msg.getFromUri().indexOf('@'));
            if (buddyName == null || buddyName.equals("")) buddyName = buddyNo;
            Intent receivedMessageIntent = new Intent(this, MessagesActivity.class);
            receivedMessageIntent.putExtra("buddyName", buddyName);
            receivedMessageIntent.putExtra("buddyNo", buddyNo);

            PendingIntent pendingIntent = PendingIntent.getActivity(this, 102, receivedMessageIntent, 0);

            NotificationCompat.MessagingStyle.Message message =
                    new NotificationCompat.MessagingStyle.Message(msg.getMsgBody(), Long.valueOf(java.time.LocalTime.now().toSecondOfDay()) ,buddyName);

            NotificationCompat.MessagingStyle style = null;

            StatusBarNotification[] nots = notificationManager.getActiveNotifications();
            for(StatusBarNotification s : nots){
                if(s.getId() == receivedMessage.contactId) style =
                        NotificationCompat.MessagingStyle.extractMessagingStyleFromNotification(s.getNotification());
            }

            if (style == null) style = new NotificationCompat.MessagingStyle(buddyName);

            NotificationCompat.Builder builder = new NotificationCompat.Builder(this, "MsgChan")
                    .setSmallIcon(R.mipmap.tkomico)
                    .setContentIntent(pendingIntent)
                    .setStyle(style
                            .addMessage(message))
                    .setPriority(NotificationCompat.PRIORITY_DEFAULT);

            notificationManager.notify(receivedMessage.contactId, builder.build());
        }
        msg.delete();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        //pjTrash = new LinkedList<>();

        initSipEndpoint(this);

        activeChatList = new LinkedList<>();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = "testchan";
            String description = "test channel";
            int importance = NotificationManager.IMPORTANCE_DEFAULT;
            NotificationChannel channel = new NotificationChannel("MsgChan", name, importance);
            channel.setDescription(description);
            // Register the channel with the system; you can't change the importance
            // or other notification behaviors after this
            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }

        if(db == null) {
            RDBMainDB db = Room.databaseBuilder(this, RDBMainDB.class, "tajnokomDb").fallbackToDestructiveMigration().build();
            this.db = db;
        }

        DbTask dbTask = new DbTask();
        dbTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);

        helpInstance = this;

        Intent notificationIntent = new Intent(this, MainActivity.class);

        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0,
                notificationIntent, 0);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, "MsgChan")
                .setSmallIcon(R.mipmap.tkomico)
                .setContentTitle("TajnoKOM")
                .setContentText("Пријављени сте на сервер: " + domain)
                .setContentIntent(pendingIntent)
                .setPriority(NotificationCompat.PRIORITY_HIGH);

        startForeground(1, builder.build());

        return Service.START_NOT_STICKY;
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    App(Context context){
        if(db == null) {
            appContext = context;
            RDBMainDB db = Room.databaseBuilder(appContext, RDBMainDB.class, "tajnokomDb").fallbackToDestructiveMigration().build();
            this.db = db;
        }
    }

    public RDBMainDB getDb(){
        return db;
    }

    public void destroyInstance(){
        instance = null;
    }

    public static App getInstance(){
        return instance;
    }

    public App initSipEndpoint(Context context){
            appContext = context;

           // if(instance == null) {
               // instance = new App();
                endpoint = new Endpoint();

                try {
                    endpoint.libCreate();
                } catch (Exception e) {
                    e.printStackTrace();
                }

                epConfig = new EpConfig();
                sipTpConfig = new TransportConfig();

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
                epConfig = null;


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

    public class DbTask extends AsyncTask<Void, Void, Void>{

        @Override
        protected Void doInBackground(Void... voids) {
            try {
                //registrovanje trenutne niti u okviru pjlib-a
                App.getInstance().endpoint.libRegisterThread("logint");
            } catch (Exception e) {
                e.printStackTrace();
            }
            System.out.println("??????????????????????????????????????????????????????????????????????????????");
            if(!logedin){
                /*RDBMainDB db = App.getDb();
                List<RELogInCreds> creds = db.getDAO().getAllLogins();
                if(!creds.isEmpty()){
                    logIn(creds.get(0));
                    App.domain = creds.get(0).domainName;
                }*/
                List<REBuddy> buddies = db.getDAO().getAllBuddies();
                if(buddies.isEmpty()){
                    REBuddy b = new REBuddy();
                    b.BuddyName = "";
                    b.BuddyNo = "/";
                    db.getDAO().insertBuddy(b);
                }
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void unused) {


            instance = helpInstance;
            System.out.println("??????????????????????????????????????????????????????????????????????????????");
            super.onPostExecute(unused);
        }
    }

    class WriteNewMessageTask extends AsyncTask<OnInstantMessageParam, OnInstantMessageParam, OnInstantMessageParam>{

        @Override
        protected OnInstantMessageParam doInBackground(OnInstantMessageParam[] params) {
            int contactId;
            contactId = App.getInstance().getDb().getDAO()
                    .getBuddyIdByNo(params[0].getFromUri()
                            .substring(5, params[0].getFromUri().indexOf('@')));

            buddyName = App.getInstance().getDb().getDAO().getBuddyById(contactId).BuddyName;

            receivedMessage.contactId = contactId;
            App.getInstance().getDb().getDAO().insertMessage(receivedMessage);

            if(contactId == App.activeContactId){
                activeChatList.add(receivedMessage);
            }
            return params[0];
        }

        @Override
        protected void onPostExecute(OnInstantMessageParam prm) {
            finnishNotification(prm);
        }
    }

}