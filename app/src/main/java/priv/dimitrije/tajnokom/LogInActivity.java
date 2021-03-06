package priv.dimitrije.tajnokom;

import androidx.appcompat.app.AppCompatActivity;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputLayout;

import java.util.regex.Pattern;

public class LogInActivity extends AppCompatActivity {
    private boolean loggedIn = false;

    TextInputLayout tilHost;
    TextInputLayout tilUser;
    TextInputLayout tilPassword;

    Button btnPrijava;

    EditText host;
    EditText usr;
    EditText pass;

    String hostAdr;
    String extension;
    String passw;

    ProgressDialog progressDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_log_in);

        btnPrijava = (Button) findViewById(R.id.btnPrijava);
        btnPrijava.setOnClickListener(v -> requestRegistration(v));

        host =  findViewById(R.id.txtHost);
        usr =  findViewById(R.id.txtKorisnik);
        pass =  findViewById(R.id.tvDetailsContactNumber);

        tilHost = findViewById(R.id.tilHost);
        tilUser = findViewById(R.id.tilUser);
        tilPassword = findViewById(R.id.tillContactNumber);

        int viewsWidth = 85*getResources().getDisplayMetrics().widthPixels/100;
        tilPassword.getLayoutParams().width = viewsWidth;
        tilHost.getLayoutParams().width = viewsWidth;
        tilUser.getLayoutParams().width = viewsWidth;
        btnPrijava.getLayoutParams().width = viewsWidth;

        //test parametri
        host.setText("sip.linphone.org");
        usr.setText("dim55");
        pass.setText("Kolasinac0212");
    }

    @Override
    public void onBackPressed() {
        try {
            MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(LogInActivity.this);
            builder.setMessage("???? ???? ?????? ?????????????? ???? ???????????? ???? ?????????????????? ?????????????????????");
            builder.setPositiveButton("????", (d, w) -> {
                App.getInstance().usrAccount.shutdown();
                finishAffinity();
                android.os.Process.killProcess(android.os.Process.myPid());
                System.exit(0);
            });
            builder.setNegativeButton("????", (d, w) -> {
                d.cancel();
            });
            builder.show();
        }catch (Exception e){
            e.printStackTrace();
        }
    }


    //metoda koja se poziva na dugme Prijava
    private void requestRegistration(View v) {
        hostAdr = host.getText().toString();
        extension = usr.getText().toString();
        passw = pass.getText().toString();

        progressDialog = new ProgressDialog(this, R.style.AppCompatAlertDialogStyle);
        progressDialog.setCancelable(false);
        progressDialog.setMessage("????????????????????????...");
        progressDialog.setIndeterminate(true);
        progressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        progressDialog.setProgressNumberFormat(null);
        progressDialog.setProgressPercentFormat(null);
        progressDialog.show();

        if(Pattern.matches(".*\\p{InCyrillic}.*", extension) || Pattern.matches(".*\\p{InCyrillic}.*", extension) || Pattern.matches(".*\\p{InCyrillic}.*", extension)){
            progressDialog.dismiss();
            AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(LogInActivity.this);
            alertDialogBuilder.setMessage("?????????????????? ?????? ?? ??????????????, ?????? ?? ?????????? ?????????????? ???????? ???????????????? ???????? ?????????? ???????????????? ???????????????? ?? ??????????????.");
            alertDialogBuilder.setNeutralButton("?? ????????", (d,t) ->{
                d.dismiss();
            });
            AlertDialog alertDialog = alertDialogBuilder.show();
            return;
        }


        host.setEnabled(false);
        usr.setEnabled(false);
        pass.setEnabled(false);

        //instanciranje i pokretanje asinhrone niti za prijavu na server
        LogInThread linThread = new LogInThread();
        linThread.execute();
    }

    @Override
    protected void onStop() {
        super.onStop();
    }

    @Override
    protected void onStart() {
        super.onStart();
        //
        //App.getInstance(this);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    public class LogInThread extends AsyncTask<Void, Void, Void> {

        @Override
        protected Void doInBackground(Void... voids) {

            RELogInCreds reLogInCreds = new RELogInCreds();
            reLogInCreds.domainName = hostAdr;
            reLogInCreds.password = passw;
            reLogInCreds.username = extension;

            Bundle lock = new Bundle();
            App.logInLock = lock;
            synchronized (lock) {
                if (App.getInstance() == null) {
                    Intent appServiceIntent = new Intent(getApplicationContext(), App.class);
                    Bundle extras = new Bundle();
                    extras.putString("domain", hostAdr);
                    extras.putString("pass", passw);
                    extras.putString("user", extension);
                    appServiceIntent.putExtras(extras);
                    startForegroundService(appServiceIntent);
                }
                try {
                    lock.wait();
                }catch (Exception e) {
                    e.printStackTrace();
                }
            }


            try {
                App.getInstance().endpoint.libRegisterThread(Thread.currentThread().getName());
            }catch (Exception e){
                e.printStackTrace();
            }

            loggedIn = App.getInstance().isLogedin();
            return null;
        }


        @Override
        protected void onPostExecute(Void unused) {
            try {
                //registrovanje trenutne niti u okviru pjlib-a
                App.getInstance().endpoint.libRegisterThread(Thread.currentThread().getName());
            } catch (Exception e) {
                e.printStackTrace();
            }

            //vracanje svih komponenti interfejsa na omogucene
            progressDialog.dismiss();
            host.setEnabled(true);
            usr.setEnabled(true);
            pass.setEnabled(true);



            try {
                //provera da li je u logIn niti izvrsena prijava i preusmeravanje na sledecu aktivnosti ili obavestenje o neuspesnoj prijavi
                if (loggedIn) {
                    new Thread(() -> {
                        //pamcenje trenutnih podataka za prijavu u Room-u
                        RDBMainDB db = App.getInstance().getDb();
                        RDMainDbDAO dao = db.getDAO();
                        if (!dao.getAllLogins().isEmpty()) dao.deleteLogin(dao.getAllLogins().get(0));
                        RELogInCreds reLogInCreds = new RELogInCreds();
                        reLogInCreds.domainName = hostAdr;
                        reLogInCreds.password = passw;
                        reLogInCreds.username = extension;
                        dao.insertLogin(reLogInCreds);
                        db.close();
                    }).start();

                    //prijava uspela
                    Intent intent = new Intent(LogInActivity.this, MainActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                } else {


                    //neuspela prijava
                    String msg = null;
                    switch (App.FAILED_STATUS){
                        case SipConstants.SERVER_NOT_RESPONDING:
                            msg = "???????????? ???? ???? ?????????????? (" + App.FAILED_STATUS + ")";
                            break;
                        case SipConstants.WRONG_PASSWORD_OR_USER:
                            msg = "???????????????? ?????????? ?????? ???????????????????? ??????.\n?????????????????? ????????????.";
                            break;
                        case SipConstants.REQUEST_MERGED:
                            msg = "???????????? ???? ??????????????." +
                                    "???????????????? ??????????????.";
                            break;
                        default:
                            msg = "????????????, ?????????????????? ????????????.";
                    }

                    //ukloni servis
                    Intent stopAppServiceIntent = new Intent(getApplicationContext(), App.class);
                    App.getInstance().destroyInstance();
                    stopService(stopAppServiceIntent);

                    AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(LogInActivity.this, R.style.MaterialAlertDialog_Material3);
                    alertDialogBuilder.setMessage(msg);
                    alertDialogBuilder.setNeutralButton("?? ????????", (d,v) ->{
                        d.dismiss();
                    });
                    AlertDialog alertDialog = alertDialogBuilder.show();
                }
            }catch (NullPointerException nullPointerException){
                nullPointerException.printStackTrace();
                AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(LogInActivity.this);
                alertDialogBuilder.setMessage("???????????? ???? ???? ?????????????? ");
                alertDialogBuilder.setNeutralButton("?? ????????", (d,v) ->{
                    d.dismiss();
                });
                AlertDialog alertDialog = alertDialogBuilder.show();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}