package priv.dimitrije.tajnokom;

import android.app.NotificationManager;
import android.app.ProgressDialog;
import android.os.AsyncTask;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.EditText;
import android.widget.TextView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import org.pjsip.pjsua2.BuddyInfo;
import org.pjsip.pjsua2.SendInstantMessageParam;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Iterator;
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

    private boolean editing;
    private ArrayList<REMessage> selectedMessages;

    private Menu menu;

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

    private int buddyStatus = 0;


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        editing = false;
        selectedMessages = new ArrayList<>();

        setContentView(R.layout.activity_messages);

        currentBuddyName = getIntent().getExtras().getString("buddyName");
        currentBuddyNo = getIntent().getExtras().getString("buddyNo");

        ProgressDialog progressDialog = ProgressDialog.show(MessagesActivity.this, "", "", true);
        progressDialog.show();
        AsyncTask connectToBuddyTask = new AsyncTask() {
            @Override
            protected Object doInBackground(Object[] objects) {
                try{
                    App.getInstance().endpoint.libRegisterThread(Thread.currentThread().getName());
                }catch (Exception e){
                    e.printStackTrace();
                }

                TajniBuddy buddy = new TajniBuddy();
                bcfg = new MyBuddyCfg();
                bcfg.setUri("sip:"+currentBuddyNo+"@"+App.getInstance().domain);

                try {
                    buddy.create(App.getInstance().usrAccount, bcfg);
                    buddy.subscribePresence(true);
                    BuddyInfo buddyInfo = buddy.getInfo();
                    int cnt = 0;
                    while (buddyInfo.getPresStatus().getStatus() == 0 && cnt < 6){
                        cnt++;
                    }
                    buddyStatus = buddyInfo.getPresStatus().getStatus();
                    buddyInfo.delete();
                } catch (Exception e) {
                    e.printStackTrace();
                }
                bcfg.delete();
                App.getInstance().activeBuddy = buddy;
                return null;
            }

            @Override
            protected void onPostExecute(Object o) {
                progressDialog.dismiss();
            }
        };
        connectToBuddyTask.execute();

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

        rvMessages = findViewById(R.id.rvMessages);

        etMessage = findViewById(R.id.etMessage);

        btnSend = findViewById(R.id.btnSend);
        btnSend.setOnClickListener((v -> {
            try {
                App.getInstance().endpoint.libRegisterThread(Thread.currentThread().getName());
            }catch (Exception e){
                e.printStackTrace();
            }

            prm = new SendInstantMessageParam();
            prm.setContent(etMessage.getText().toString());

            etMessage.setText(null);

            try {
                App.getInstance().activeBuddy.sendInstantMessage(prm);
            } catch (Exception e) {
                e.printStackTrace();
            }
            REMessage reMessage = new REMessage();
            reMessage.msgText = prm.getContent();
            reMessage.contactId = App.getInstance().activeContactId;
            reMessage.sent = true;
            reMessage.time = LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm"));

            Thread writeMsgToDb = new Thread(() -> {
                App.getInstance().getDb().getDAO().insertMessage(reMessage);
                App.getInstance().closeDb();
            });
            writeMsgToDb.start();

            App.getInstance().activeChatList.add(reMessage);
            messagesRVAdapter.notifyItemInserted(messagesRVAdapter.getItemCount()-1);
            rvMessages.scrollToPosition(App.getInstance().activeChatList.size()-1);

            prm.delete();
        }));
    }

    List<REMessage> helperList;
    @Override
    protected void onStart() {
        super.onStart();


        try {
            Object lock = new Object();

            synchronized (lock) {
                Thread setActiveContactIdThread = new Thread(() -> {
                    int activeId;
                    RDBMainDB db = App.getInstance().getDb();
                    activeId = db.getDAO().getBuddyIdByNo(currentBuddyNo);
                    App.getInstance().activeContactId = activeId;
                    App.getInstance().activeChatList = new LinkedList<>(db.getDAO().getMsgsFrom(activeId, 50));
                    db.close();
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

        messagesRVAdapter = new MessagesRVAdapter(App.getInstance().activeChatList, this);
        rvMessages.setAdapter(messagesRVAdapter);
        rvMessages.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false));
        rvMessages.setOnScrollChangeListener((v, scrollX, scrollY, oldScrollX, oldScrollY) -> {
            LinearLayoutManager linearLayoutManager = (LinearLayoutManager)((RecyclerView) v).getLayoutManager();
            int pos = linearLayoutManager.findFirstCompletelyVisibleItemPosition();
            if (pos <= 2){
                    if(App.getInstance().activeChatList.size() >= 50) {
                        AsyncTask getMoreMsgs = new AsyncTask() {
                            @Override
                            protected Object doInBackground(Object[] objects) {
                                    helperList = App.getInstance().getDb()
                                            .getDAO()
                                            .getMsgsFrom(
                                                    App.getInstance().activeContactId,
                                                    App.getInstance().activeChatList.size() + 50
                                            );
                                    App.getInstance().closeDb();
                                    return null;
                            }

                            @Override
                            protected void onPostExecute(Object o) {
                                synchronized (App.getInstance().activeChatList) {
                                    App.getInstance().activeChatList.clear();
                                    App.getInstance().activeChatList.addAll(helperList);
                                }
                                messagesRVAdapter. notifyItemRangeChanged(0, helperList.size());
                            }
                        };
                        getMoreMsgs.execute();
                    }
                }
        });

        NotificationManager notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        notificationManager.cancel(App.getInstance().activeContactId);

        /*for(int cntr = App.getInstance().activeChatList.size()-1; cntr >= 0; cntr--){
            if(cntr < 0) break;
            if(App.getInstance().activeChatList.get(cntr).read){
                rvMessages.scrollToPosition(cntr);
            }
        }*/

        for(int cntr = App.getInstance().activeChatList.size()-1; cntr >= 0; cntr --){
            if(cntr < 0) break;
            if(App.getInstance().activeChatList.get(cntr).read){
                rvMessages.scrollToPosition(cntr);
                break;
            }
        }
        Thread markRead = new Thread(() -> {
            RDBMainDB db = App.getInstance().getDb();
            List<REMessage> unread = db.getDAO().getUnread();
            for(REMessage m : unread){
                m.read = true;
            }
            db.getDAO().updateMessages(unread);
            db.close();
        });
        markRead.start();

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        this.menu = menu;
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
        App.getInstance().activeBuddy.delete();
    }

    @Override
    public void onBackPressed() {
        if(editing){
            stopEditing();
            selectedMessages.clear();
        }else{
            super.onBackPressed();
        }
    }

    private void stopEditing(){
        editing = false;
        messagesRVAdapter.resetOverlays();
        //TODO: resetuj toolbar
        menu.removeItem(R.id.delete_msgs);
        getMenuInflater().inflate(R.menu.chatmenu, menu);
    }

    public void toggleEditMode() {
        editing = true;
        menu.removeItem(R.id.action_contact_details);
        getMenuInflater().inflate(R.menu.edit_mode_menu, menu);
    }

    public void addSelectedMessage(REMessage msg){
        selectedMessages.add(msg);
        System.out.println("---------SELECTING MESSAGE: " + msg.msgText + " | " + selectedMessages.size() + "---------\n");

    }

    public void removeSelectedMessage(REMessage msg){
        selectedMessages.remove(msg);
    }

    public boolean isEditing(){
        return editing;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        switch (item.getItemId()){
            case R.id.delete_msgs:
                deleteSelectedMessages();
                break;
            case R.id.action_contact_details:
                //TODO: Prikazi detalje kontakta

                break;
            default:
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    private void deleteSelectedMessages() {
        App.getInstance().activeChatList.removeAll(selectedMessages);
        messagesRVAdapter.resetOverlays();
        messagesRVAdapter.notifyDataSetChanged();
        stopEditing();
        Thread deleteMsgsThread = new Thread(() -> {
            Iterator<REMessage> i = selectedMessages.iterator();
            RDBMainDB db = App.getInstance().getDb();
            while(i.hasNext()) {
                db.getDAO().deleteMessage(i.next());
            }
            db.close();
            selectedMessages.clear();
        });
        deleteMsgsThread.start();

    }
}