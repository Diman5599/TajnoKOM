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

import java.util.ArrayList;
import java.util.Comparator;
import java.util.function.Predicate;


public class MainActivity extends AppCompatActivity {
    private FragmentContainerView fragmentView;

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

        //fragmentView = findViewById(R.id.fragmentContainerView);
        getSupportFragmentManager().beginTransaction().add(R.id.fragmentContainerView, new MainFragment()).commit();

        loadContacts();
    }

    private void loadContacts() {
        new Thread(
                ()->{
                    //zameniti ucitavanjem iz baze!!!!!!!!!!
                    App.activeChats = new ArrayList<>();

                    //ucitavanje postojecih kontakata iz lokalne baze podataka
                    App.contacts = App.getInstance().getDb().getDAO().getAllBuddiesS();
                    App.contacts.removeIf(reBuddy -> reBuddy.BuddyName.equals(""));
                    App.contacts.sort(Comparator.comparing(o -> o.BuddyName));
                }
        ).start();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
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
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void goToAddBuddy() {
        getSupportFragmentManager().beginTransaction().replace(R.id.fragmentContainerView, AddBuddyFragment.class, null).addToBackStack("test1")
                .setReorderingAllowed(true).commit();
    }

    private void logOut() {
        try {
            PresenceStatus presenceStatus = new PresenceStatus();
            presenceStatus.setActivity(0);
            try {
                App.usrAccount.setOnlineStatus(presenceStatus);
            } catch (Exception e) {
                e.printStackTrace();
            }

            App.getInstance().endpoint.libRegisterThread("logout");
        App.usrAccount.shutdown();
        new Thread(()->{
            try {
                App.getInstance().endpoint.libRegisterThread("logout");
            } catch (Exception e) {
                e.printStackTrace();
            }
            RDBMainDB db = App.getInstance().getDb();
            db.getDAO().deleteLogin(db.getDAO().getAllLogins().get(0));
            db.getDAO().deleteAllBuddies();
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


    /*@Override
    public void onBackPressed() {
        int empty = getSupportFragmentManager().getBackStackEntryCount();
        System.out.println(empty);
        if (empty==0) {
            MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(MainActivity.this);
            builder.setMessage("Да ли сте сигурни да желите да напустите апликацију?");
            builder.setPositiveButton("Да", (d, w) -> {
                App.usrAccount.shutdown();
                finishAffinity();
                android.os.Process.killProcess(android.os.Process.myPid());
                System.exit(0);
            });
            builder.setNegativeButton("Не", (d, w) -> {
                d.cancel();
            });
            builder.show();
        }else{
            super.onBackPressed();
        }
    }*/

    @Override
    protected void onPause() {
        super.onPause();
        /*PresenceStatus presenceStatus = new PresenceStatus();
        presenceStatus.setActivity(0);
        try {
            App.usrAccount.setOnlineStatus(presenceStatus);
        } catch (Exception e) {
            e.printStackTrace();
        }*/
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }
}