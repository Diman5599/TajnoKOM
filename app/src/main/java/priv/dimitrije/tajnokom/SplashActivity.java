package priv.dimitrije.tajnokom;

import androidx.appcompat.app.AppCompatActivity;
import androidx.room.Room;

import android.content.Intent;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Bundle;
import android.widget.ProgressBar;

import java.util.List;

public class SplashActivity extends AppCompatActivity {
    private boolean loggedIn = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        //provera da li postoji zapamcen nalog u bazi i pokusaj prijave
        LoadTask loadTask = new LoadTask();
        loadTask.execute();

        ProgressBar progressBar = (ProgressBar) findViewById(R.id.pbSplashLoad);
        progressBar.getIndeterminateDrawable().setColorFilter(
                Color.RED, android.graphics.PorterDuff.Mode.SRC_IN);
    }

    @Override
    public void onBackPressed() {

    }

    @Override
    protected void onStop() {
        super.onStop();


    }

    @Override
    protected void onStart() {
        super.onStart();

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }
    public class LoadTask extends AsyncTask<Void, Void, Void>{

        @Override
        protected Void doInBackground(Void... voids) {

                if (!(App.getInstance() == null))
                    loggedIn = App.getInstance().isLogedin();

            return null;
        }


        @Override
        protected void onPostExecute(Void unused) {
            try {
                Intent intent = null;
                if(loggedIn) {
                    intent = new Intent(SplashActivity.this, MainActivity.class);
                }else{
                    intent = new Intent(SplashActivity.this, LogInActivity.class);
                }
                startActivity(intent);
                finish();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}