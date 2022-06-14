package priv.dimitrije.tajnokom;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.FragmentContainerView;


import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.PowerManager;
import android.provider.Settings;
import android.view.Menu;
import android.view.MenuItem;

import org.pjsip.pjsua2.PresenceStatus;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.function.Predicate;


public class MainActivity extends AppCompatActivity {
    private FragmentContainerView fragmentView;
    public Menu menu;
    public ContactsFragment contactsFragment;
    public ChatFragment chatFragment;

    @Override
    public void onBackPressed() {
        if(chatFragment.isEditing()){
            chatFragment.toggleEditing();
        }else {
            super.onBackPressed();
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = findViewById(R.id.mainToolbar);
        setSupportActionBar(toolbar);

        boolean isIgnoringBatteryOptimizations = getSystemService(PowerManager.class).isIgnoringBatteryOptimizations(getPackageName());
        if(!isIgnoringBatteryOptimizations){
            Intent intent = new Intent();
            intent.setAction(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS);
            intent.setData(Uri.parse("package:" + getPackageName()));
            startActivityForResult(intent, 10102);
        }

        App.getInstance().activeChats = new LinkedList<>();
        App.getInstance().contacts = new LinkedList<>();

        //fragmentView = findViewById(R.id.fragmentContainerView);
        getSupportFragmentManager().beginTransaction().add(R.id.fragmentContainerView, new MainFragment()).commit();

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        this.menu = menu;
        getMenuInflater().inflate(R.menu.mainmenu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        switch (item.getItemId()){
            case R.id.action_logout:
                logOut();
                return true;
            case R.id.action_add_buddy:
                goToAddBuddy();
                return true;
            case R.id.action_delete_contact:
                contactsFragment.deleteContacts();
                break;
            case R.id.action_cancel_contact_edit:
                contactsFragment.clearSelectedContacts();
                contactsFragment.toggleEditing();
                break;
            case R.id.action_delete_chat:
                chatFragment.deleteChats();
                break;
            default:
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    private void goToAddBuddy() {
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragmentContainerView, AddBuddyFragment.class, null)
                .addToBackStack("fragment_main")
                .commit();
    }

    private void logOut() {
        try {
            PresenceStatus presenceStatus = new PresenceStatus();
            presenceStatus.setActivity(0);
            try {
                App.getInstance().usrAccount.setOnlineStatus(presenceStatus);
            } catch (Exception e) {
                e.printStackTrace();
            }

            App.getInstance().endpoint.libRegisterThread("logout");
            App.getInstance().usrAccount.shutdown();
        new Thread(()->{
            try {
                App.getInstance().endpoint.libRegisterThread("logout");
            } catch (Exception e) {
                e.printStackTrace();
            }
            RDBMainDB db = App.getInstance().getDb();
            db.getDAO().deleteLogin(db.getDAO().getAllLogins().get(0));
            db.getDAO().deleteAllBuddies();
            db.close();
        }).start();
        try {
            App.getInstance().endpoint.libDestroy();
        } catch (Exception e) {
            e.printStackTrace();
        }
        App.getInstance().endpoint.delete();
        App.getInstance().destroyInstance();
        Intent stopAppServiceIntent = new Intent(this, App.class);
        stopService(stopAppServiceIntent);

        Intent intent = new Intent(MainActivity.this, LogInActivity.class);
        startActivity(intent);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }
}