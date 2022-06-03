package priv.dimitrije.tajnokom;

import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.io.Serializable;
import java.util.List;

public class ChatRVAdapter extends RecyclerView.Adapter<ChatRVAdapter.ViewHolder> implements Serializable {

    private List<REBuddy> localDataSet;

    public ChatRVAdapter(List<REBuddy> bds){
        localDataSet = bds;
    }


    @NonNull
    @Override
    public ChatRVAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.chat_field, parent, false);
        view.setOnLongClickListener(v ->{
            //TODO: open specific chat

            return true;
        });
        view.setOnLongClickListener(v -> {
            Toast toast = Toast.makeText(v.getContext(), "CLICK!", Toast.LENGTH_SHORT);
            toast.show();
            return true;
        });
        return new ChatRVAdapter.ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ChatRVAdapter.ViewHolder holder, int position) {
        String contact = localDataSet.get(0).BuddyName;
        holder.tvChatInitials.setText(getInitials(contact));
        holder.tvChatInitials.setGravity(Gravity.CENTER);
        holder.tvContactName.setText(contact);
    }

    private String getInitials(String contact) {
        return contact.substring(0,1);
    }

    @Override
    public int getItemCount() {
        return App.activeChats.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder{
        TextView tvChatInitials;
        TextView tvContactName;
        TextView tvLastMessage;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);

            tvChatInitials = itemView.findViewById(R.id.tvChatInitials);
            tvContactName = itemView.findViewById(R.id.tvContactNm);
            tvLastMessage = itemView.findViewById(R.id.tvLastMessage);
        }
    }
}
