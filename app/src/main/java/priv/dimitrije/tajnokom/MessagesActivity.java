package priv.dimitrije.tajnokom;

import android.app.NotificationManager;
import android.os.AsyncTask;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.view.Gravity;
import android.view.Menu;
import android.widget.EditText;
import android.widget.TextView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import org.pjsip.pjsua2.SendInstantMessageParam;

import java.util.LinkedList;
import java.util.List;


public class MessagesActivity extends AppCompatActivity {

    RecyclerView rvMessages;
    FloatingActionButton btnSend;
    EditText etMessage;

    String currentBuddyName;
    String currentBuddyNo;

    TextView tvChatContactStatus;

    private MyBuddyCfg bcfg;
    private SendInstantMessageParam prm;
    private MessagesRVAdapter messagesRVAdapter;

    private String getStatus(int code){
        String status = "Непознато";
        switch (code){
            case 0:
                status = "Непознато";
                break;
            case 1:
                status = "Доступан";
                break;
            case 2:
                status = "Није регистрован";
                break;
            default:
                break;
        }
        return status;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_messages);

        currentBuddyName = getIntent().getExtras().getString("buddyName");
        currentBuddyNo = getIntent().getExtras().getString("buddyNo");


        App.activeBuddy = new TajniBuddy();
        bcfg = new MyBuddyCfg();
        bcfg.setUri("sip:"+currentBuddyNo+"@"+App.domain);

        int buddyStatus = 0;

        try {
            App.activeBuddy.create(App.usrAccount, bcfg);
            App.activeBuddy.subscribePresence(true);
            while (App.activeBuddy.getInfo().getPresStatus().getStatus() == 0);
            buddyStatus = App.activeBuddy.getInfo().getPresStatus().getStatus();
        } catch (Exception e) {
            e.printStackTrace();
        }

        bcfg.delete();

        //postavljanje Toolbar-a
        Toolbar toolbar = findViewById(R.id.chatToolbar);
        getLayoutInflater().inflate(R.layout.chatbar, toolbar);

        TextView tvChatContactName = findViewById(R.id.tvChatContactNm);
        tvChatContactName.setText(currentBuddyName);
        TextView tvChatInitials = findViewById(R.id.tvChatInitials);
        tvChatInitials.setGravity(Gravity.CENTER);
        tvChatInitials.setText(getInitials(currentBuddyName));
        tvChatContactStatus = findViewById(R.id.tvChatContactStatus);
        tvChatContactStatus.setText(getStatus(buddyStatus));

        setSupportActionBar(toolbar);

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowHomeEnabled(true);

        try {
            Object lock = new Object();

            synchronized (lock) {
                Thread setActiveContactIdThread = new Thread(() -> {
                    int activeId;
                    activeId = App.getDb().getDAO().getBuddyIdByNo(currentBuddyNo);
                    App.activeContactId = activeId;
                    App.activeChatList = new LinkedList<>(App.getDb().getDAO().getMsgsFrom(activeId, 20));
                    synchronized (lock) {
                        lock.notifyAll();
                    }
                });
                setActiveContactIdThread.start();

                lock.wait();
            }

        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        rvMessages = findViewById(R.id.rvMessages);

        etMessage = findViewById(R.id.etMessage);

        btnSend = findViewById(R.id.btnSend);
        btnSend.setOnClickListener((v -> {

            try {
                App.endpoint.libRegisterThread(Thread.currentThread().getName());
            }catch (Exception e){
                e.printStackTrace();
            }

            prm = new SendInstantMessageParam();
            prm.setContent(etMessage.getText().toString());

            etMessage.setText(null);

            try {
                App.activeBuddy.sendInstantMessage(prm);
            } catch (Exception e) {
                e.printStackTrace();
            }
            REMessage reMessage = new REMessage();
            reMessage.msgText = prm.getContent();
            reMessage.contactId = App.activeContactId;
            reMessage.sent = true;

            Thread writeMsgToDb = new Thread(() -> {
                App.getDb().getDAO().insertMessage(reMessage);
            });
            writeMsgToDb.start();

            App.activeChatList.add(reMessage);
            messagesRVAdapter.notifyItemInserted(messagesRVAdapter.getItemCount()-1);
            rvMessages.scrollToPosition(App.activeChatList.size()-1);

            prm.delete();
        }));
    }

    List<REMessage> helperList;
    @Override
    protected void onStart() {
        super.onStart();
        messagesRVAdapter = new MessagesRVAdapter(App.activeChatList);
        rvMessages.setAdapter(messagesRVAdapter);
        rvMessages.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false));
        rvMessages.setOnScrollChangeListener((v, scrollX, scrollY, oldScrollX, oldScrollY) -> {
            LinearLayoutManager linearLayoutManager = (LinearLayoutManager)((RecyclerView) v).getLayoutManager();
            int pos = linearLayoutManager.findFirstCompletelyVisibleItemPosition();
            if (pos <= 2){
                    if(App.activeChatList.size() >= 20) {
                        AsyncTask getMoreMsgs = new AsyncTask() {
                            @Override
                            protected Object doInBackground(Object[] objects) {
                                    helperList = App.getDb()
                                            .getDAO()
                                            .getMsgsFrom(
                                                    App.activeContactId,
                                                    App.activeChatList.size() + 20
                                            );
                                    return null;
                            }

                            @Override
                            protected void onPostExecute(Object o) {
                                synchronized (App.activeChatList) {
                                    App.activeChatList.clear();
                                    App.activeChatList.addAll(helperList);
                                }
                                messagesRVAdapter.notifyDataSetChanged();
                            }
                        };
                        getMoreMsgs.execute();
                    }
                }
        });

        NotificationManager notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        notificationManager.cancel(App.activeContactId);

        rvMessages.scrollToPosition(messagesRVAdapter.getItemCount()-1);

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.chatmenu, menu);
        return true;
    }

    private String getInitials(String contact) {
        String[] names = contact.split(" ", 2);

        String result = "";
        for(String n : names){
            if(n.length() > 0)
                result += n.substring(0,1);
        }
        return result.toUpperCase();
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    protected void onStop() {
        super.onStop();
        MessagesRVAdapter.destroy();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        App.activeBuddy.delete();
    }
}