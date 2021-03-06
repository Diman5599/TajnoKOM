package priv.dimitrije.tajnokom;

import android.app.NotificationManager;
import android.app.ProgressDialog;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.text.Editable;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.navigation.NavigationView;

import org.pjsip.pjsua2.BuddyInfo;
import org.pjsip.pjsua2.SendInstantMessageParam;

import java.time.LocalDateTime;
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
    DrawerLayout nvDrawer;

    //fioka
    TextView tvDrawerInitials;
    TextView tvDrawerContactName;
    TextView tvDrawerContactNumber;
    NavigationView navView;

    public static String currentBuddyName;
    public static String currentBuddyNo;
    private int currentBuddyId;

    protected static TextView tvChatContactStatus;

    private MyBuddyCfg bcfg;
    private SendInstantMessageParam prm;
    private MessagesRVAdapter messagesRVAdapter;

    private boolean editing;
    private ArrayList<REMessage> selectedMessages;

    private Menu menu;

    protected static String getStatus(int code){
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
        currentBuddyId = getIntent().getExtras().getInt("buddyId");

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

                MyBuddy buddy = new MyBuddy();
                bcfg = new MyBuddyCfg();
                bcfg.setUri("sip:"+currentBuddyNo+"@"+App.getInstance().domain);

                try {
                    buddy.create(App.getInstance().usrAccount, bcfg);
                    buddy.subscribePresence(true);
                    BuddyInfo buddyInfo = buddy.getInfo();
//                    int cnt = 0;
//                    while (buddyInfo.getPresStatus().getStatus() == 0 && cnt < 6){
//                        cnt++;
//                    }
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

        nvDrawer = findViewById(R.id.nvDrawer);
        nvDrawer.addDrawerListener(new DrawerLayout.SimpleDrawerListener() {
            @Override
            public void onDrawerOpened(View drawerView) {
                tvDrawerInitials = findViewById(R.id.tvDrawerInitials);
                tvDrawerInitials.setText(getInitials(currentBuddyName));
                tvDrawerContactName = findViewById(R.id.tvDrawerContactName);
                tvDrawerContactName.setText(currentBuddyName);
                tvDrawerContactNumber = findViewById(R.id.tvDrawerContactNumber);
                tvDrawerContactNumber.setText(currentBuddyNo);
            }
        });
        navView = findViewById(R.id.navView);
        navView.getMenu()
                .findItem(R.id.action_contact_details)
                .setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
                    @Override
                    public boolean onMenuItemClick(MenuItem item) {
                        Intent details = new Intent(navView.getContext(), ContactDetailsActivity.class);
                        details.putExtra("buddyName", currentBuddyName);
                        details.putExtra("buddyNo", currentBuddyNo);
                        details.putExtra("buddyId", currentBuddyId);
                        startActivity(details);
                        nvDrawer.closeDrawer(Gravity.RIGHT);
                        return true;
                    }
                });

        setSupportActionBar(toolbar);

        getSupportActionBar().setHomeAsUpIndicator(R.drawable.ic_baseline_arrow_back_ios_24);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowHomeEnabled(true);

        rvMessages = findViewById(R.id.rvMessages);
        etMessage = findViewById(R.id.etMessage);

        //etMessageLayout = findViewById(R.id.etMessageLayout);

        btnSend = findViewById(R.id.btnSend);
        btnSend.setOnClickListener((v -> {
            try {
                App.getInstance().endpoint.libRegisterThread(Thread.currentThread().getName());
            }catch (Exception e){
                e.printStackTrace();
            }

            String cleartextMessageContent = etMessage.getText().toString();
            String encryptedMessageContent = App.getInstance().getEncryptor().encrypt(cleartextMessageContent);

            prm = new SendInstantMessageParam();
            prm.setContent(encryptedMessageContent);

            etMessage.setText(null);

            try {
                App.getInstance().activeBuddy.sendInstantMessage(prm);
            } catch (Exception e) {
                e.printStackTrace();
            }
            REMessage reMessage = new REMessage();
            reMessage.msgText = cleartextMessageContent;
            reMessage.contactId = App.getInstance().activeContactId;
            reMessage.sent = true;
            reMessage.time = LocalDateTime.now().format(DateTimeFormatter.ofPattern("uuuu-MM-dd HH:mm:ss:SSS"));

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



        etMessage.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if(s.toString().equals("")){
                    btnSend.setEnabled(false);
                }else{
                    btnSend.setEnabled(true);
                }
            }
            @Override
            public void afterTextChanged(Editable s) {}
        });

       /* etMessage.setOnFocusChangeListener((v, hasFocus) -> {
            if(hasFocus){
                etMessageLayout.setHint("");
            }else{
                if(etMessage.getText().toString().equals(""))
                    etMessageLayout.setHint("Унесите поруку...");
            }
        });*/
    }

    List<REMessage> helperList;
    @Override
    protected void onStart() {
        super.onStart();

        }

    @Override
    protected void onResume() {
        super.onResume();

        TextView tvChatContactName = findViewById(R.id.tvChatContactNm);
        tvChatContactName.setText(currentBuddyName);
        TextView tvChatInitials = findViewById(R.id.tvChatInitials);
        tvChatInitials.setGravity(Gravity.CENTER);
        tvChatInitials.setText(getInitials(currentBuddyName));
        tvChatContactStatus = findViewById(R.id.tvChatContactStatus);
        tvChatContactStatus.setText(getStatus(buddyStatus));

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


        for(int cntr = App.getInstance().activeChatList.size()-1; cntr >= 0; cntr --){
            if(cntr < 0) break;
            if(App.getInstance().activeChatList.get(cntr).read){
                rvMessages.scrollToPosition(cntr);
                break;
            }
        }
        Thread markRead = new Thread(() -> {
            RDBMainDB db = App.getInstance().getDb();
            List<REMessage> unread = db.getDAO().getUnread(App.getInstance().activeContactId);
            for(REMessage m : unread){
                m.read = true;
            }
            db.getDAO().updateMessages(unread);
            db.close();
            List<REMessage> activeUnread = App.getInstance().unreadMessages.get(App.getInstance().activeContactId);
            if(activeUnread != null) activeUnread.clear();
        });
        markRead.start();


    }



    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        this.menu = menu;
        getMenuInflater().inflate(R.menu.open_drawer_action, menu);
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
            if(nvDrawer.isDrawerOpen(Gravity.RIGHT)) nvDrawer.closeDrawer(Gravity.RIGHT);
            else previousActivity();
        }

    }

    private void stopEditing(){
        editing = false;
        messagesRVAdapter.resetOverlays();
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
            case R.id.action_open_drawer:
                nvDrawer.openDrawer(Gravity.RIGHT);
                break;
            case android.R.id.home:
                previousActivity();
                break;
            default:
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    private void previousActivity(){
        Intent intent = new Intent(this, MainActivity.class);
        startActivity(intent);
        finish();
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