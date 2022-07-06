package priv.dimitrije.tajnokom;

import android.app.ProgressDialog;
import android.content.res.Resources;
import android.os.AsyncTask;
import android.os.Bundle;

import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.view.LayoutInflater;
import android.view.Menu;
import android.view.View;
import android.view.ViewGroup;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedList;
import java.util.List;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link ChatFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class ChatFragment extends Fragment {
    private RecyclerView rvChat;
    private boolean editing;

    private ChatRVAdapter chatRVAdapter;

    private List<ActiveChatModel> selectedChats;

    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";

    // TODO: Rename and change types of parameters
    private String mParam1;
    private String mParam2;

    public ChatFragment() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param param1 Parameter 1.
     * @param param2 Parameter 2.
     * @return A new instance of fragment ChatFragment.
     */
    // TODO: Rename and change types and number of parameters
    public static ChatFragment newInstance(String param1, String param2) {
        ChatFragment fragment = new ChatFragment();
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

        selectedChats = new LinkedList<>();
    }

    private ChatFragment chatFragment;

    @Override
    public void onStart() {
        super.onStart();

        //helpers
        editing = false;
        chatFragment = this;

        LoadActiveChatsTask task = new LoadActiveChatsTask();
        task.execute();
    }

    public boolean isEditing(){
        return editing;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View root = inflater.inflate(R.layout.fragment_chat, container, false);

        rvChat = root.findViewById(R.id.rvChat);

        ((MainActivity) getActivity()).chatFragment = this;

        return root;
    }

    public void addSelectedChats(ActiveChatModel activeChatModel){
        selectedChats.add(activeChatModel);
    }

    public void removeSelectedChats(ActiveChatModel activeChatModel){
        selectedChats.remove(activeChatModel);
    }
    public ProgressDialog progressDialog;
    public void deleteChats() {
        progressDialog = ProgressDialog.show(getContext(), "", "");
        AsyncTask deleteChatsTask = new AsyncTask() {
            @Override
            protected Object doInBackground(Object[] objects) {
                RDBMainDB db = App.getInstance().getDb();
                for(ActiveChatModel acm : selectedChats){
                    db.getDAO().deleteMessagesOfBuddy(acm.getContact().BuddyId);
                }
                db.close();
                App.getInstance().activeChats.removeAll(selectedChats);
                selectedChats.clear();
                return null;
            }

            @Override
            protected void onPostExecute(Object o) {
                progressDialog.dismiss();
                chatRVAdapter.notifyDataSetChanged();
            }
        };
        deleteChatsTask.execute();
        toggleEditing();
    }

    public void toggleEditing(){
        Menu mMenu = ((MainActivity) getActivity()).menu;
        mMenu.clear();
        if(editing){
            editing = false;
            getActivity().getMenuInflater().inflate(R.menu.mainmenu, mMenu);
            chatRVAdapter.resetSelection();
        }else{
            editing = true;
            getActivity().getMenuInflater().inflate(R.menu.edit_chats_menu, mMenu);
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        Menu mMenu = ((MainActivity) getActivity()).menu;
        try{
            mMenu.clear();
        }catch (NullPointerException e){
            e.printStackTrace();
        }
        if(editing){
            toggleEditing();
        }
        getActivity().getMenuInflater().inflate(R.menu.mainmenu, mMenu);
    }

    private class LoadActiveChatsTask extends AsyncTask{
        @Override
        protected Object doInBackground(Object[] objects) {
            RDBMainDB db = App.getInstance().getDb();
            List<Integer> buddyIds = db.getDAO().getAllBuddyIdsWithMessages();
            List<ActiveChatModel> chats = new LinkedList<>();
            for(int n = 0; n < buddyIds.size(); n++){
                chats.add(new ActiveChatModel(buddyIds.get(n), db));
            }
            db.close();
            chats.sort((o1, o2) ->
            {
                int diff;
                LocalDateTime t1 = LocalDateTime.from(DateTimeFormatter.ofPattern("uuuu-MM-dd HH:mm:ss:SSS").parse(o1.getLastMessage().time));
                LocalDateTime t2 = LocalDateTime.from(DateTimeFormatter.ofPattern("uuuu-MM-dd HH:mm:ss:SSS").parse(o2.getLastMessage().time));
                diff = t2.compareTo(t1);
                return diff;
            });
            for(ActiveChatModel a : chats){
                System.out.println(a + " | " + a.getLastMessage().time);
            }
            App.getInstance().activeChats = chats;
            return null;
        }

        @Override
        protected void onPostExecute(Object o) {
            chatRVAdapter = new ChatRVAdapter(App.getInstance().activeChats, chatFragment);
            rvChat.setAdapter(chatRVAdapter);
            rvChat.setLayoutManager(new LinearLayoutManager(getActivity(), LinearLayoutManager.VERTICAL, false));
        }

    }
}