package priv.dimitrije.tajnokom;

import android.content.Intent;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.io.Serializable;
import java.util.LinkedList;
import java.util.List;

public class ChatRVAdapter extends RecyclerView.Adapter<ChatRVAdapter.ViewHolder> implements Serializable {

    private List<ActiveChatModel> localDataSet;
    private ChatFragment parentFragment;

    private LinkedList<ViewHolder> selectedViewHolders;

    private static ChatRVAdapter instance;

    public static ChatRVAdapter getInstance(){
        return instance;
    }

    public ChatRVAdapter(List<ActiveChatModel> chats, ChatFragment parentFragment){
        instance = this;

        localDataSet = chats;
        this.parentFragment = parentFragment;

        selectedViewHolders = new LinkedList<>();
    }


    @NonNull
    @Override
    public ChatRVAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.chat_field, parent, false);

        return new ChatRVAdapter.ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ChatRVAdapter.ViewHolder holder, int position) {
        ActiveChatModel chat = localDataSet.get(position);
        if(!chat.getContact().BuddyName.equals("")){
            holder.tvChatInitials.setText(getInitials(chat.getContact().BuddyName));
            holder.tvContactName.setText(chat.getContact().BuddyName);
        }else{
            holder.tvChatInitials.setText(getInitials(chat.getContact().BuddyNo));
            holder.tvContactName.setText(chat.getContact().BuddyNo);
        }
        holder.tvChatInitials.setGravity(Gravity.CENTER);
        holder.tvLastMessage.setText((!chat.getLastMessage().sent ? holder.tvContactName.getText().toString() : "Ја") + ": " + chat.getLastMessage().msgText.replace('\n', ' '));
        holder.tvUnreadCount.setGravity(Gravity.CENTER);
        if(chat.getUnreadCount() == 0) holder.tvUnreadCount.setVisibility(View.INVISIBLE);
        else holder.tvUnreadCount.setVisibility(View.VISIBLE);
        holder.tvUnreadCount.setText("" + chat.getUnreadCount());

        //otvori cet
        holder.itemView.setOnClickListener((v -> {
            if(!parentFragment.isEditing()) {
                openThisChat(v, chat.getContact());
            }else{
                if(holder.selected){
                    holder.selected = false;
                    parentFragment.removeSelectedChats(localDataSet.get(position));
                    selectedViewHolders.remove(holder);
                    holder.ivCheck.setVisibility(View.INVISIBLE);
                }else {
                    holder.selected = true;
                    parentFragment.addSelectedChats(localDataSet.get(position));
                    selectedViewHolders.add(holder);
                    holder.ivCheck.setVisibility(View.VISIBLE);
                    Animation scaleIvCheck = AnimationUtils.loadAnimation(holder.ivCheck.getContext(), R.anim.scale_check_iv);
                    holder.ivCheck.startAnimation(scaleIvCheck);
                }
            }
        }));

        //zapocni "edit" cetova
       holder.itemView.setOnLongClickListener(v -> {
            if(!parentFragment.isEditing()) {
                selectedViewHolders.add(holder);

                parentFragment.addSelectedChats(localDataSet.get(position));
                parentFragment.toggleEditing();
                holder.selected = true;
                holder.ivCheck.setVisibility(View.VISIBLE);
                Animation scaleIvCheck = AnimationUtils.loadAnimation(holder.ivCheck.getContext(), R.anim.scale_check_iv);
                holder.ivCheck.startAnimation(scaleIvCheck);
            }
            return true;
        });
    }

    private void openThisChat(View v, REBuddy reBuddy){
        Intent intent = new Intent(v.getContext(), MessagesActivity.class);

        intent.putExtra("buddyName", reBuddy.BuddyName);
        intent.putExtra("buddyNo", reBuddy.BuddyNo);
        intent.putExtra("buddyId", reBuddy.BuddyId);

        this.parentFragment.getContext().startActivity(intent);
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
        return App.getInstance().activeChats.size();
    }

    public void resetSelection() {
        for(ViewHolder vh : selectedViewHolders){
            vh.ivCheck.setVisibility(View.INVISIBLE);
        }
    }

    public static class ViewHolder extends RecyclerView.ViewHolder{
        TextView tvChatInitials;
        TextView tvContactName;
        TextView tvLastMessage;
        TextView tvUnreadCount;
      //  Button btnChatOverlay;
        ImageView ivCheck;

        boolean selected;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);

            selected = false;

            tvUnreadCount = itemView.findViewById(R.id.tvUnreadCount);
            tvChatInitials = itemView.findViewById(R.id.tvChatInitials);
            tvContactName = itemView.findViewById(R.id.tvContactNm);
            tvLastMessage = itemView.findViewById(R.id.tvLastMessage);
            ivCheck = itemView.findViewById(R.id.ivCheck);
        }
    }
}
