package priv.dimitrije.tajnokom;

import android.content.Intent;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;

import android.widget.Button;
import android.widget.CheckBox;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.recyclerview.widget.RecyclerView;

import com.codecomputerlove.fastscrollrecyclerviewdemo.FastScrollRecyclerViewInterface;

import java.nio.charset.CharsetDecoder;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class ContactsRVAdapter extends RecyclerView.Adapter<ContactsRVAdapter.ViewHolder> implements FastScrollRecyclerViewInterface {

    private List<REBuddy> localDataSet;
    private Map<View, Integer> buttonMap;
    private ContactsFragment fragment;
    private LinkedHashMap<String, Integer> mIndexMap;

    //dataSet - trenutna lista kontakata
    public ContactsRVAdapter(List<REBuddy> dataSet, LinkedHashMap<String, Integer> indexMapping, ContactsFragment callingFragment){
        localDataSet = dataSet;
        buttonMap = new HashMap<>();
        this.fragment = callingFragment;
        this.mIndexMap = indexMapping;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.contact_field, parent, false);

        return new ViewHolder(view, this);
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
        if(fragment.isEditig()){
            ConstraintLayout.LayoutParams lp = new ConstraintLayout.LayoutParams(ConstraintLayout.LayoutParams.WRAP_CONTENT, ConstraintLayout.LayoutParams.MATCH_CONSTRAINT);
            lp.horizontalBias = 0;
            holder.cbSelectedContact.setLayoutParams(lp);
            holder.cbSelectedContact.setVisibility(View.VISIBLE);
        }else{
            ConstraintLayout.LayoutParams lp = new ConstraintLayout.LayoutParams(1, 1);
            lp.horizontalBias = 0;
            holder.cbSelectedContact.setLayoutParams(lp);
            holder.cbSelectedContact.setVisibility(View.INVISIBLE);
        }
        if(fragment.selectedContacts.contains(localDataSet.get(position))){
            holder.cbSelectedContact.setChecked(true);
        }else{
            holder.cbSelectedContact.setChecked(false);
        }
    }

    private void clicked(View v){
        REBuddy b = App.getInstance().contacts.get(buttonMap.get(v));
        openChat(b);
    }

    private void openChat(REBuddy buddy){
        Intent msgsActivityIntent = new Intent(this.fragment.getActivity(), MessagesActivity.class);


        msgsActivityIntent.putExtra("buddyName", buddy.BuddyName);
        msgsActivityIntent.putExtra("buddyNo", buddy.BuddyNo);
        msgsActivityIntent.putExtra("buddyId", buddy.BuddyId);

        this.fragment.getActivity().startActivityFromFragment(this.fragment, msgsActivityIntent,101);
    }

    public static String getInitials(String contact) {
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

    @Override
    public HashMap<String, Integer> getMapIndex() {
        return mIndexMap;
    }

    public static class ViewHolder extends RecyclerView.ViewHolder{
        //koji je u listi (veza sa kontaktom)
        private int index;

        ContactsRVAdapter contactsRVAdapter;

        TextView tvInitials;
        TextView tvContactName;
        TextView tvContactNo;
        TextView overlay;
        CheckBox cbSelectedContact;

        public ViewHolder(View v, ContactsRVAdapter adapter){
            super(v);
            contactsRVAdapter = adapter;
            tvInitials = v.findViewById(R.id.tvInitials);
            tvContactName = v.findViewById(R.id.tvContactNm);
            tvContactNo = v.findViewById(R.id.tvContactNo);
            overlay = v.findViewById(R.id.tvOverlay);
            cbSelectedContact = v.findViewById(R.id.cbSelectContact);

            overlay.setOnLongClickListener((vw -> {
                if(!contactsRVAdapter.fragment.isEditig()) {
                    cbSelectedContact.setChecked(true);
                    contactsRVAdapter.fragment.toggleEditing();
                }
                return true;
            }));

            overlay.setOnClickListener(vw -> {
                Animation animation = AnimationUtils.loadAnimation(vw.getContext(), R.anim.contact_clicked_anim);
                overlay.setBackgroundColor(vw.getResources().getColor(R.color.bordeaux, vw.getContext().getTheme()));
                overlay.startAnimation(animation);
                overlay.postDelayed(()->{
                    if(!contactsRVAdapter.fragment.isEditig()) contactsRVAdapter.clicked(vw);
                    else {
                        cbSelectedContact.setChecked(!cbSelectedContact.isChecked());
                    }
                    overlay.setBackgroundColor(vw.getResources().getColor(R.color.transparency, vw.getContext().getTheme()));
                }, 200);
            });

            cbSelectedContact.setOnCheckedChangeListener((buttonView, isChecked) -> {
                if(isChecked){
                    contactsRVAdapter.fragment.addContactToSelected(contactsRVAdapter.localDataSet.get(this.getBindingAdapterPosition()));
                }else{
                    contactsRVAdapter.fragment.removeContactFromSelected(contactsRVAdapter.localDataSet.get(this.getBindingAdapterPosition()));
                }

            });
        }

        public int getIndex(){return index;}

        public void setIndex(int i){index = i;}

        public void bindButton(Map<View, Integer> map){
            map.put(overlay, index);
        }
    }

}
