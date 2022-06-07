package priv.dimitrije.tajnokom;

import android.app.AlertDialog;
import android.database.sqlite.SQLiteConstraintException;
import android.os.AsyncTask;
import android.os.Bundle;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ProgressBar;

import org.pjsip.pjsua2.BuddyConfig;

import java.util.Comparator;


public class AddBuddyFragment extends Fragment{

    private Button addBuddyButton;
    private EditText etBuddyName;
    private EditText etBuddyNo;
    private FrameLayout pbarContainer;
    private ProgressBar pbar;

    public AddBuddyFragment() {
        // Required empty public constructor
    }

    public static AddBuddyFragment newInstance(String param1, String param2) {
        AddBuddyFragment fragment = new AddBuddyFragment();
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    private void addBuddy() {
        if (etBuddyName.getText().toString().equals("") || etBuddyNo.getText().toString().equals("")) {
            AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(getActivity());
            alertDialogBuilder.setMessage("Унесите име.");
            alertDialogBuilder.setNeutralButton("У реду", (d,t) ->{
                d.dismiss();
            });
            AlertDialog alertDialog = alertDialogBuilder.show();
            return;
        };

        pbar = new ProgressBar(getActivity());

        pbarContainer.addView(pbar);
        addBuddyButton.setEnabled(false);
        etBuddyName.setEnabled(false);
        etBuddyNo.setEnabled(false);

        AddBudyTask addBudyTask = new AddBudyTask();
        addBudyTask.execute();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        final View root = inflater.inflate(R.layout.fragment_add_buddy, container, false);

        addBuddyButton = root.findViewById(R.id.btnAddBuddy);
        etBuddyName = root.findViewById(R.id.etBuddyName);
        etBuddyNo = root.findViewById(R.id.etBuddyNo);
        pbarContainer = root.findViewById(R.id.pbarContainer);

        addBuddyButton.setOnClickListener((v)->addBuddy());

        return root;
    }

    private class AddBudyTask extends AsyncTask<Void, Void, Void>{
       // TajniBuddy buddy = null;

        @Override
        protected Void doInBackground(Void... voids) {
            /*BuddyConfig bddcfg = new BuddyConfig();
            bddcfg.setUri("sip:" + etBuddyName.getText() + "@" + App.accfg.getRegConfig().getRegistrarUri().replaceFirst("sip:", ""));
            System.out.println(bddcfg.getUri());
            bddcfg.setSubscribe(true);
            buddy = new TajniBuddy();
            try {
                buddy.create(App.usrAccount, bddcfg);
                Thread.sleep(100);
                //nit ceka da kontakt prihvati zahtev
                synchronized (buddy) {
                    buddy.wait(5000);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }*/

            RDBMainDB db = App.getInstance().getDb();
            int id = db.getDAO().getBuddyIdByNo(etBuddyNo.getText().toString());
            REBuddy newContact = db.getDAO().getBuddyById(id);
            if(newContact == null){
                newContact = new REBuddy();
                newContact.BuddyName =  etBuddyName.getText().toString();
                newContact.BuddyNo =  etBuddyNo.getText().toString();
                db.getDAO().insertBuddy(newContact);
                App.getInstance().contacts.add(newContact);
            }else{
                if(newContact.BuddyName.equals("")){
                    newContact.BuddyName = etBuddyName.getText().toString();
                    db.getDAO().updateBuddy(newContact);
                    App.getInstance().contacts.add(newContact);
                }else{
                    AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(getActivity());
                    alertDialogBuilder.setMessage("Већ постоји контакт са унетим бројем.");
                    alertDialogBuilder.setNeutralButton("У реду", (d,t) ->{
                        d.dismiss();
                    });
                    AlertDialog alertDialog = alertDialogBuilder.show();
                }
            }



            db.close();
            App.getInstance().contacts.sort(Comparator.comparing(o -> o.BuddyName));

            return null;
        }

        @Override
        protected void onPostExecute(Void unused) {
            /*etBuddyName.setEnabled(true);
            addBuddyButton.setEnabled(true);

            pbarContainer.removeView(pbar);

            try {
                //ako je prihvacen zahtev
                if(buddy.getInfo().getPresStatus().getStatus()==1) {
                    //MyApp.contactList.add(buddy);

                    /*SendInstantMessageParam simp = new SendInstantMessageParam();
                    simp.setContentType("text/plain");
                    simp.setContent("Zdravo!");
                    buddy.sendInstantMessage(simp);*/

                   /* new Thread(() -> {
                        synchronized (buddy){
                            REBuddy b = new REBuddy();
                            b.BuddyName = etBuddyName.getText().toString();
                            try {
                                if (!App.contacts.stream().anyMatch(reBuddy -> b.BuddyName.equals(reBuddy.BuddyName))) {
                                    App.getDb().getDAO().insertBuddy(b);
                                    App.contacts.add(b);
                                    App.contacts.sort(Comparator.comparing(o -> o.BuddyName));
                                }
                            }catch (SQLiteConstraintException e){
                                e.printStackTrace();
                            }
                        }
                    }).start();

                    getActivity().getSupportFragmentManager()
                            .beginTransaction()
                            .replace(R.id.fragmentContainerView, MainFragment.class, null)
                            .setReorderingAllowed(true)
                            .commit();


                }else{
                    //ako zahtev nije uspeo (nit se probudila zbog isteka timeout-a)
                    AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(getActivity());
                    alertDialogBuilder.setMessage("Грешка." +
                            "\nПроверите мрежну везу и уписано име, па покушајте поново.");
                    alertDialogBuilder.setNeutralButton("У реду", (d,t) ->{
                        d.dismiss();
                    });
                    AlertDialog alertDialog = alertDialogBuilder.show();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }*/

            getActivity().getSupportFragmentManager().popBackStack("fragment_main", FragmentManager.POP_BACK_STACK_INCLUSIVE);
            getActivity().getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.fragmentContainerView, MainFragment.class, null)
                    .commit();

            pbarContainer.removeView(pbar);

            super.onPostExecute(unused);
        }
    }
}