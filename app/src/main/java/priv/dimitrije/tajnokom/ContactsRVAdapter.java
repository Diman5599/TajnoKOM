package priv.dimitrije.tajnokom;

import android.content.Intent;
import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.RecyclerView;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ContactsRVAdapter extends RecyclerView.Adapter<ContactsRVAdapter.ViewHolder> {

    private List<REBuddy> localDataSet;
    private Map<View, Integer> buttonMap;
    private Fragment fragment;


    //dataSet - trenutna lista kontakata
    public ContactsRVAdapter(List<REBuddy> dataSet, Fragment callingFragment){
        localDataSet = dataSet;
        buttonMap = new HashMap<>();
        this.fragment = callingFragment;
    }


    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.contact_field, parent, false);

        return new ViewHolder(view);
    }

    //izvrsava se pri svakom prebacivanju na recyclerview
    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        REBuddy contact = localDataSet.get(position);
        holder.tvContactName.setText(contact.BuddyName);
        holder.tvInitials.setText(getInitials(contact.BuddyName));
        holder.tvContactNo.setText(contact.BuddyNo);
        holder.tvInitials.setGravity(Gravity.CENTER);
        holder.setIndex(position);
        holder.bindButton(buttonMap);
        holder.field.setOnClickListener((v -> {
            REBuddy b = App.contacts.get(buttonMap.get(v));
            /*if(!App.activeChats.stream().anyMatch(reBuddy -> b.BuddyName.equals(reBuddy.BuddyName))) {
                App.activeChats.add(0, b);
                App.chatRVAdapter.notifyItemInserted(App.activeChats.size() - 1);
            }*/
            openChat(b);
        }));
        holder.tvContactNo.setOnClickListener(v -> clicked(v));
        holder.tvInitials.setOnClickListener(v -> clicked(v));
        holder.tvContactName.setOnClickListener(v -> clicked(v));
    }

    private void clicked(View v){
        REBuddy b = App.contacts.get(buttonMap.get(v.getParent()));
            /*if(!App.activeChats.stream().anyMatch(reBuddy -> b.BuddyName.equals(reBuddy.BuddyName))) {
                App.activeChats.add(0, b);
                App.chatRVAdapter.notifyItemInserted(App.activeChats.size() - 1);
            }*/
        openChat(b);
    }

    private void openChat(REBuddy buddy){
        App.getInstance().pjTrash.clear();
        /*this.fragment.getActivity()
                .getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragmentContainerView, MessagesActivity.class, args)
                .addToBackStack("chat")
                .setReorderingAllowed(true)
                .commit();*/
        Intent msgActivityIntent = new Intent(this.fragment.getActivity(), MessagesActivity.class);

        msgActivityIntent.putExtra("buddyName", buddy.BuddyName);
        msgActivityIntent.putExtra("buddyNo", buddy.BuddyNo);

        this.fragment.getActivity().startActivityFromFragment(this.fragment, msgActivityIntent,101);

    }

    private String getInitials(String contact) {
        String[] names = contact.split(" ", 2);

        String result = "";
        for(String n : names){
            if(n.length() > 0)
                result += n.substring(0,1);
        }
        return result.toUpperCase();
    }

    @Override
    public int getItemCount() {
        return localDataSet.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder{
        //koji je u listi (veza sa kontaktom)
        private int index;

        TextView tvInitials;
        TextView tvContactName;
        TextView tvContactNo;
        ConstraintLayout field;

        public ViewHolder(View v){
            super(v);
            tvInitials = v.findViewById(R.id.tvInitials);
            tvContactName = v.findViewById(R.id.tvContactNm);
            tvContactNo = v.findViewById(R.id.tvContactNo);
            field = v.findViewById(R.id.contactFieldLayout);
        }

        public int getIndex(){return index;}

        public void setIndex(int i){index = i;}

        public void bindButton(Map<View, Integer> map){
            map.put(field, index);
        }
    }

}
