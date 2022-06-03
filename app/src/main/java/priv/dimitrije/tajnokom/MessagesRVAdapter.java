package priv.dimitrije.tajnokom;

import android.annotation.SuppressLint;
import android.graphics.drawable.Drawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;

public class MessagesRVAdapter extends RecyclerView.Adapter<MessagesRVAdapter.ViewHolder> {
    List<REMessage> msgs;
    MessagesActivity parentActivity;

    private int selectedCount;

    public MessagesRVAdapter(List<REMessage> items, MessagesActivity messagesActivity){
        instance = this;
        msgs = items;
        parentActivity = messagesActivity;

        selectedCount = 0;
    }

    public static void destroy() {
        instance = null;
    }

    @NonNull
    @Override
    public MessagesRVAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.msg_field, parent, false);
        return new ViewHolder(view);
    }

    private boolean firstReceivedFlag = false;
    @SuppressLint("UseCompatLoadingForDrawables")
    @Override
    public void onBindViewHolder(@NonNull MessagesRVAdapter.ViewHolder holder, int position) {
            holder.tvMsg.setText(msgs.get(position).msgText);
            if (!msgs.get(position).sent) {
                holder.tvMsg.setBackground(holder.tvMsg.getResources().getDrawable(R.drawable.message_bubble_receive, holder.tvMsg.getContext().getTheme()));
                ((ConstraintLayout.LayoutParams) holder.tvMsg.getLayoutParams()).horizontalBias = 0;
            }else{
                holder.tvMsg.setBackground(holder.tvMsg.getResources().getDrawable(R.drawable.message_bubble_send, holder.tvMsg.getContext().getTheme()));
                ((ConstraintLayout.LayoutParams) holder.tvMsg.getLayoutParams()).horizontalBias = 1;
            }


        holder.itemView.setOnClickListener( v -> {
            if(parentActivity.isEditing()){
                if (!holder.selected){
                    holder.selected = true;
                    Drawable d = v.getResources().getDrawable(R.drawable.msg_hold_overlay, v.getContext().getTheme());
                    v.setForeground(d);
                    selectedCount++;
                    parentActivity.addSelectedMessage(msgs.get(holder.getBindingAdapterPosition()));
                }else{
                    holder.selected = false;
                    v.setForeground(null);
                    parentActivity.removeSelectedMessage(msgs.get(holder.getBindingAdapterPosition()));
                    selectedCount--;
                }
            }
        });

            holder.itemView.setOnLongClickListener(v -> {
                if (!parentActivity.isEditing()){
                    holder.selected = true;
                    Drawable d = v.getResources().getDrawable(R.drawable.msg_hold_overlay, v.getContext().getTheme());
                    v.setForeground(d);
                    selectedCount++;
                    parentActivity.addSelectedMessage(msgs.get(holder.getBindingAdapterPosition()));
                    if(selectedCount == 1) parentActivity.toggleEditMode();
                }
                return true;
            });
    }



    private static MessagesRVAdapter instance;

    public static MessagesRVAdapter getInstance(){
        return instance;
    }

    public static void notifyMe(){
        instance.notifyItemInserted(instance.getItemCount());
    }

    @Override
    public int getItemCount() {
        return msgs.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder{
        TextView tvMsg;
        ConstraintLayout body;

        boolean selected;

        public ViewHolder(View itemView) {
            super(itemView);
            tvMsg = itemView.findViewById(R.id.tvMsg);
            body = (ConstraintLayout) itemView;

            selected = false;
        }
    }
}
