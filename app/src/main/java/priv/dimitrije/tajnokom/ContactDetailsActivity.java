package priv.dimitrije.tajnokom;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import android.app.ProgressDialog;
import android.content.Intent;
import android.icu.text.Collator;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.TextView;

import com.google.android.material.appbar.CollapsingToolbarLayout;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.Comparator;
import java.util.regex.Pattern;

public class ContactDetailsActivity extends AppCompatActivity {
    CollapsingToolbarLayout ctDetailsToolbar;

    private String contactName;
    private String contactNo;
    private int contactId;

    private boolean editing;

    private static TextView tvInitials;
    private static TextView tvContactName;
    private static TextView tvContactNumber;

    private static Button btnSaveChanges;

    private static FloatingActionButton btnToggleEditContact;

    private static ProgressDialog progressDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_contact_details);

        editing = false;

        Bundle intentExtras = getIntent().getExtras();
        contactName = intentExtras.getString("buddyName");
        contactNo = intentExtras.getString("buddyNo");
        contactId = intentExtras.getInt("buddyId");

        tvInitials = findViewById(R.id.tvDetailsInitials);
        tvContactName = findViewById(R.id.tvDetailsContactName);
        tvContactNumber = findViewById(R.id.tvDetailsContactNumber);
        btnToggleEditContact = findViewById(R.id.btnToggleEditContact);
        btnSaveChanges = findViewById(R.id.btnSaveChanges);
        ctDetailsToolbar = findViewById(R.id.ctDetailsToolbar);

        tvInitials.setText(ContactsRVAdapter.getInitials(contactName));
        tvContactName.setText(contactName);
        tvContactNumber.setText(contactNo);
        ctDetailsToolbar.setTitle(contactName);

        btnToggleEditContact.setOnClickListener(v -> {
            if(editing){
                tvContactNumber.setText(contactNo);
                tvContactName.setText(contactName);

                tvInitials.setEnabled(false);
                tvContactName.setEnabled(false);
                tvContactNumber.setEnabled(false);
                btnSaveChanges.setEnabled(false);

                btnToggleEditContact.setImageDrawable(getResources().getDrawable(R.drawable.ic_baseline_edit_24));

                editing = false;
            }else{
                tvInitials.setEnabled(true);
                tvContactName.setEnabled(true);
                tvContactNumber.setEnabled(true);
                btnSaveChanges.setEnabled(true);

                btnToggleEditContact.setImageDrawable(getResources().getDrawable(R.drawable.baseline_edit_off_24));

                editing = true;
            }
        });

        btnSaveChanges.setOnClickListener( v -> {
            if(tvContactNumber.getText().toString().equals("")
                    || tvContactName.getText().toString().equals("")
                    || tvContactName.getText().toString().isEmpty()
                    || tvContactNumber.getText().toString().isEmpty()){
                AlertDialog.Builder alertBuilder = new AlertDialog.Builder(v.getContext());
                alertBuilder.setTitle("Грешка")
                        .setMessage("Унесите име и број контакта")
                        .setNeutralButton("У реду", (d,t) -> {
                            d.dismiss();
                        });
                alertBuilder.show();
            }else if(!Pattern.matches("[a-zA-z0-9]+", tvContactNumber.getText().toString())){
                AlertDialog.Builder alertBuilder = new AlertDialog.Builder(v.getContext());
                alertBuilder.setTitle("Грешка")
                        .setMessage("SIP број сме садржати само слова енглеске абецеде и бројеве")
                        .setNeutralButton("У реду", (d,t) -> {
                            d.dismiss();
                        });
                alertBuilder.show();
            }else{
                tvInitials.setEnabled(false);
                tvContactName.setEnabled(false);
                tvContactNumber.setEnabled(false);
                btnSaveChanges.setEnabled(false);

                progressDialog = ProgressDialog.show(this, "", "");

                contactNo = tvContactNumber.getText().toString();
                contactName = tvContactName.getText().toString();
                MessagesActivity.currentBuddyName = contactName;
                MessagesActivity.currentBuddyNo = contactNo;

                btnToggleEditContact.setImageDrawable(getResources().getDrawable(R.drawable.ic_baseline_edit_24));

                UpdateContactTask updateContactTask = new UpdateContactTask();
                updateContactTask.execute(contactName, contactNo, String.valueOf(contactId));
            }
        });
        setSupportActionBar(findViewById(R.id.detailsToolbar));

        getSupportActionBar().setHomeAsUpIndicator(R.drawable.ic_baseline_arrow_back_ios_24);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                previousActivity();
                break;
            default:
                break;
        }
        return true;
    }

    private void previousActivity(){
//        Intent intent = new Intent(this, MessagesActivity.class);
//        intent.putExtra("buddyName", contactName);
//        intent.putExtra("buddyId", contactId);
//        intent.putExtra("buddyNo", contactNo);
//        startActivity(intent);
        finish();
    }

    private static class UpdateContactTask extends AsyncTask<String, String, String>{

        public UpdateContactTask(){

        }

        @Override
        protected String doInBackground(String... strings) {
            RDBMainDB db = App.getInstance().getDb();

            REBuddy b = db.getDAO().getBuddyById(Integer.valueOf(strings[2]));
            b.BuddyName = strings[0];
            b.BuddyNo = strings[1];
            db.getDAO().updateBuddy(b);

            App.getInstance().contacts = db.getDAO().getAllBuddiesS();
            App.getInstance().contacts.removeIf(reBuddy -> reBuddy.BuddyName.equals(""));
            App.getInstance().contacts.sort((Comparator<REBuddy>) (o1, o2) -> Collator.getInstance().compare(o1.BuddyName, o2.BuddyName));

            db.close();

            return strings[0];
        }

        @Override
        protected void onPostExecute(String string) {
            tvInitials.setText(ContactsRVAdapter.getInitials(string));

            progressDialog.dismiss();
        }
    }
}