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

import org.pjsip.pjsua2.AccountInfo;

import java.util.regex.Pattern;

public class LogInActivity extends AppCompatActivity {
    private boolean loggedIn = false;

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

        App.appContext = getBaseContext();

        Button btnPrijava = (Button) findViewById(R.id.btnPrijava);
        btnPrijava.setOnClickListener(v -> requestRegistration(v));

        host = (EditText) findViewById(R.id.txtHost);
        usr = (EditText) findViewById(R.id.txtKorisnik);
        pass = (EditText) findViewById(R.id.txtLozinka);

        //test parametri
        host.setText("192.168.8.101");
        usr.setText("Test");
        pass.setText("100");
    }

    @Override
    public void onBackPressed() {
        try {
            MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(LogInActivity.this);
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
        }catch (Exception e){
            e.printStackTrace();
        }
    }


    //metoda koja se poziva na dugme Prijava
    private void requestRegistration(View v) {
        hostAdr = host.getText().toString();
        extension = usr.getText().toString();
        passw = pass.getText().toString();

        progressDialog = ProgressDialog.show(LogInActivity.this, "", "Пријављивање...", true);

        if(Pattern.matches(".*\\p{InCyrillic}.*", extension) || Pattern.matches(".*\\p{InCyrillic}.*", extension) || Pattern.matches(".*\\p{InCyrillic}.*", extension)){
            progressDialog.dismiss();
            AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(LogInActivity.this);
            alertDialogBuilder.setMessage("Корисничо име и лозинка, као и назив сервера могу садржати само слова енглеске латинице и бројеве.");
            alertDialogBuilder.setNeutralButton("У реду", (d,t) ->{
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

            App.domain = hostAdr;

            if (App.getInstance() == null) {
                Intent appServiceIntent = new Intent(getApplicationContext(), App.class);
                startForegroundService(appServiceIntent);
            }
            while (App.getInstance() == null) ;

            loggedIn = App.getInstance().logIn(reLogInCreds);
            App.domain = reLogInCreds.domainName;

            return null;
        }


        @Override
        protected void onPostExecute(Void unused) {
            try {
                //registrovanje trenutne niti u okviru pjlib-a
                App.getInstance().endpoint.libRegisterThread("logint3");
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
                    }).start();

                    //prijava uspela
                    Intent intent = new Intent(LogInActivity.this, MainActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                } else {

                    //ukloni servis
                    Intent stopAppServiceIntent = new Intent(getApplicationContext(), App.class);
                    stopService(stopAppServiceIntent);

                    //neuspela prijava
                    String msg = null;
                    switch (App.accInfo.getRegStatus()){
                        case SipConstants.SERVER_NOT_RESPONDING:
                            msg = "Сервер се не одазива (" + App.accInfo.getRegStatus() + ")";
                            break;
                        case SipConstants.WRONG_PASSWORD_OR_USER:
                            msg = "Погрешна шифра или корисничко име.\nПокушајте поново.";
                            break;
                        case SipConstants.REQUEST_MERGED:
                            msg = "Грешка на серверу." +
                                    "Пробајте касније.";
                            break;
                        default:
                            msg = "Грешка, покушајте поново.";
                    }

                    AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(LogInActivity.this);
                    alertDialogBuilder.setMessage(msg);
                    alertDialogBuilder.setNeutralButton("У реду", (d,v) ->{
                        d.dismiss();
                    });
                    AlertDialog alertDialog = alertDialogBuilder.show();
                }
            }catch (NullPointerException nullPointerException){
                nullPointerException.printStackTrace();
                AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(LogInActivity.this);
                alertDialogBuilder.setMessage("Сервер се не одазива ");
                alertDialogBuilder.setNeutralButton("У реду", (d,v) ->{
                    d.dismiss();
                });
                AlertDialog alertDialog = alertDialogBuilder.show();
            }
        }
    }
}