package priv.dimitrije.tajnokom;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentContainerView;


import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.PowerManager;
import android.provider.Settings;
import android.text.InputType;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.EditText;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import org.pjsip.pjsua2.CallOpParam;
import org.pjsip.pjsua2.PresenceStatus;

import java.util.LinkedList;


public class MainActivity extends AppCompatActivity {
    private FragmentContainerView fragmentView;
    public Menu menu;
    public ContactsFragment contactsFragment;
    public ChatFragment chatFragment;

    EditText dialogEditText;

    FloatingActionButton btnDial;

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

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.RECORD_AUDIO}, 10103);
        }

        App.getInstance().activeChats = new LinkedList<>();
        App.getInstance().contacts = new LinkedList<>();

        //fragmentView = findViewById(R.id.fragmentContainerView);
        getSupportFragmentManager().beginTransaction().add(R.id.fragmentContainerView, new MainFragment()).commit();

        AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(this);
        dialogBuilder.setTitle("Позовите број");
        dialogEditText = new EditText(this);
        dialogEditText.setInputType(InputType.TYPE_CLASS_TEXT);
        dialogBuilder.setView(dialogEditText);

        dialogBuilder.setPositiveButton("Позови", (dialog, which) -> {
            MyCall call = new MyCall(App.getInstance().usrAccount);
            CallOpParam callOpParam = new CallOpParam(true);
            try {
                String dstUri = "sip:" + dialogEditText.getText().toString() + "@" + App.getInstance().domain;
                System.out.println(dstUri + "DDDDDDDDDDDDDDDDDdddddddddddddddddDDDDDDDDDDDDDDDDDDDdddddddddddddd");
                call.makeCall(dstUri, callOpParam);
                dialogEditText = null;
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
        dialogBuilder.setNegativeButton("Откажи", (dialog, which) ->{
            dialogEditText = null;
            dialog.dismiss();
        });

        btnDial = findViewById(R.id.btnDial);
        btnDial.setOnClickListener(v -> {
            dialogBuilder.create().show();
        });

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
            App.getInstance().endpoint.libRegisterThread("logout");

            PresenceStatus presenceStatus = new PresenceStatus();
            presenceStatus.setActivity(0);
            try {
                App.getInstance().usrAccount.setOnlineStatus(presenceStatus);
            } catch (Exception e) {
                e.printStackTrace();
            }

            App.getInstance().usrAccount.shutdown();
        new Thread(()->{
            RDBMainDB db = App.getInstance().getDb();
            db.getDAO().deleteLogin(db.getDAO().getAllLogins().get(0));
            db.close();
        }).start();
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