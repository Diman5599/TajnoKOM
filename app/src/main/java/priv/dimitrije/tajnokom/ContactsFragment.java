package priv.dimitrije.tajnokom;

import android.icu.text.Collator;
import android.os.AsyncTask;
import android.os.Bundle;

import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.view.LayoutInflater;
import android.view.Menu;
import android.view.View;
import android.view.ViewGroup;

import com.codecomputerlove.fastscrollrecyclerviewdemo.FastScrollRecyclerViewItemDecoration;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;


/**
 * A simple {@link Fragment} subclass.
 * Use the {@link ContactsFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class ContactsFragment extends Fragment {

    protected static RecyclerView rvContacts;
    private boolean editing;

    private LinkedList<REBuddy> selectedContacts;

    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";

    // TODO: Rename and change types of parameters
    private String mParam1;
    private String mParam2;

    public ContactsFragment() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param param1 Parameter 1.
     * @param param2 Parameter 2.
     * @return A new instance of fragment ContactsFragment.
     */
    // TODO: Rename and change types and number of parameters
    public static ContactsFragment newInstance(String param1, String param2) {
        ContactsFragment fragment = new ContactsFragment();
        Bundle args = new Bundle();
        args.putString(ARG_PARAM1, param1);
        args.putString(ARG_PARAM2, param2);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            mParam1 = getArguments().getString(ARG_PARAM1);
            mParam2 = getArguments().getString(ARG_PARAM2);
        }
    }

    protected void deleteContacts() {
        List<REBuddy> tempList = new LinkedList<>(selectedContacts);
        toggleEditing();
        App.getInstance().contacts.removeAll(tempList);
        rvContacts.getAdapter().notifyDataSetChanged();
        Thread deleteContactsThread = new Thread(() -> {
            RDBMainDB db = App.getInstance().getDb();
            for(REBuddy b : tempList){
                b.BuddyName = "";
                db.getDAO().updateBuddy(b);
            }
            db.close();
        });
        deleteContactsThread.start();
    }

    protected void toggleEditing(){
        Menu mMenu = ((MainActivity) getActivity()).menu;
        if(!editing) {
            editing = true;
            mMenu.removeItem(R.id.action_logout);
            mMenu.removeItem(R.id.action_add_buddy);
            getActivity().getMenuInflater().inflate(R.menu.edit_contacts_menu, mMenu);
            for (int c = 0; c < rvContacts.getChildCount(); c++) {
                ContactsRVAdapter.ViewHolder vh = (ContactsRVAdapter.ViewHolder) rvContacts.findViewHolderForAdapterPosition(c);

                ConstraintLayout.LayoutParams lp = new ConstraintLayout.LayoutParams(ConstraintLayout.LayoutParams.WRAP_CONTENT, ConstraintLayout.LayoutParams.MATCH_CONSTRAINT);
                lp.horizontalBias = 0;

                vh.cbSelectedContact.setLayoutParams(lp);
                vh.cbSelectedContact.setVisibility(View.VISIBLE);
            }
        }else{
            mMenu.removeItem(R.id.action_delete_contact);
            mMenu.removeItem(R.id.action_cancel_contact_edit);
            getActivity().getMenuInflater().inflate(R.menu.mainmenu, mMenu);
            editing = false;
            for (int c = 0; c < rvContacts.getAdapter().getItemCount(); c++) {
                ContactsRVAdapter.ViewHolder vh = (ContactsRVAdapter.ViewHolder) rvContacts.findViewHolderForAdapterPosition(c);
                ConstraintLayout.LayoutParams lp = new ConstraintLayout.LayoutParams(1, ConstraintLayout.LayoutParams.MATCH_CONSTRAINT);
                lp.horizontalBias = 0;

                vh.cbSelectedContact.setLayoutParams(lp);
                vh.cbSelectedContact.setVisibility(View.INVISIBLE);
                vh.cbSelectedContact.setChecked(false);
            }
        }
    }

    ContactsFragment instance;
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        ((MainActivity) getActivity()).contactsFragment = this;

        selectedContacts = new LinkedList<>();

        View root = inflater.inflate(R.layout.fragment_contacts, container, false);

        rvContacts = root.findViewById(R.id.rvContacts);
        rvContacts.setWillNotDraw(true);

        instance = this;
        editing = false;

        return root;
    }

    @Override
    public void onStart() {
        super.onStart();

        LoadContactsTask loadContactsTask = new LoadContactsTask();
        loadContactsTask.execute();
    }

    @Override
    public void onPause() {
        super.onPause();

        Menu mMenu = ((MainActivity) getActivity()).menu;
        mMenu.clear();
        if(editing){
            toggleEditing();
        }
        getActivity().getMenuInflater().inflate(R.menu.mainmenu, mMenu);

    }

    public boolean isEditig() {
        return editing;
    }

    public void addContactToSelected(REBuddy reBuddy){
        selectedContacts.add(reBuddy);
    }

    public void removeContactFromSelected(REBuddy reBuddy){
        selectedContacts.remove(reBuddy);
    }

    public void clearSelectedContacts(){
        selectedContacts.clear();
    }

    public int getSelectedCount(){
        return selectedContacts.size();
    }

    private LinkedHashMap<String, Integer> calculateIndexesForName(List<REBuddy> items){
        LinkedHashMap<String, Integer> mapIndex = new LinkedHashMap<>();
        for (int i = 0; i<items.size(); i++){
            String name = items.get(i).BuddyName;
            String index = name.substring(0,1);
            index = index.toUpperCase();

            if (!mapIndex.containsKey(index)) {
                mapIndex.put(index, i);
            }
        }
        return mapIndex;
    }


    public class LoadContactsTask extends AsyncTask{

        @Override
        protected Object doInBackground(Object[] objects) {
            //ucitavanje postojecih kontakata iz lokalne baze podataka
            App.getInstance().contacts = App.getInstance().getDb().getDAO().getAllBuddiesS();
            App.getInstance().contacts.removeIf(reBuddy -> reBuddy.BuddyName.equals(""));
            App.getInstance().contacts.sort((Comparator<REBuddy>) (o1, o2) -> Collator.getInstance().compare(o1.BuddyName, o2.BuddyName));
            System.out.println(App.getInstance().contacts);
            App.getInstance().closeDb();
            return null;
        }

        @Override
        protected void onPostExecute(Object o) {
            LinkedHashMap<String, Integer> indexMapping = calculateIndexesForName(App.getInstance().contacts);

            rvContacts.setLayoutManager(new LinearLayoutManager(getActivity()));
            rvContacts.setAdapter(new ContactsRVAdapter(App.getInstance().contacts, indexMapping, instance));
            FastScrollRecyclerViewItemDecoration decoration = new FastScrollRecyclerViewItemDecoration(instance.getContext());
            rvContacts.addItemDecoration(decoration);
            rvContacts.setWillNotDraw(false);
        }
    }
}