package priv.dimitrije.tajnokom;

import android.annotation.SuppressLint;
import android.graphics.drawable.Drawable;
import android.util.DisplayMetrics;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowMetrics;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.recyclerview.widget.RecyclerView;

import java.util.LinkedList;
import java.util.List;

public class MessagesRVAdapter extends RecyclerView.Adapter<MessagesRVAdapter.ViewHolder> {
    List<REMessage> msgs;
    MessagesActivity parentActivity;

    LinkedList<ViewHolder> selectedHolders;

    private int selectedCount;

    public MessagesRVAdapter(List<REMessage> items, MessagesActivity messagesActivity){
        instance = this;
        msgs = items;
        parentActivity = messagesActivity;

        selectedHolders = new LinkedList<>();
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

    private boolean firstUnread = true;
    @SuppressLint("UseCompatLoadingForDrawables")
    @Override
    public void onBindViewHolder(@NonNull MessagesRVAdapter.ViewHolder holder, int position) {
        holder.tvMsg.setText(msgs.get(position).msgText);
        holder.tvMsg.setMaxWidth(75*holder.tvMsg.getContext().getResources().getDisplayMetrics().widthPixels/100);
        holder.tvTime.setText(msgs.get(position).time.substring(0,5));

        if((!msgs.get(position).read) && firstUnread){
            holder.tvUnread.setVisibility(View.VISIBLE);
            firstUnread = false;
        }


        if (!msgs.get(position).sent) {
                holder.tvMsg.setBackground(holder.tvMsg.getResources().getDrawable(R.drawable.message_bubble_receive, holder.tvMsg.getContext().getTheme()));
                ((ConstraintLayout.LayoutParams) holder.tvMsg.getLayoutParams()).horizontalBias = 0;
                ((ConstraintLayout.LayoutParams) holder.tvTime.getLayoutParams()).horizontalBias = 0.05f;
            }else{
                holder.tvMsg.setBackground(holder.tvMsg.getResources().getDrawable(R.drawable.message_bubble_send, holder.tvMsg.getContext().getTheme()));
                ((ConstraintLayout.LayoutParams) holder.tvMsg.getLayoutParams()).horizontalBias = 1;
                ((ConstraintLayout.LayoutParams) holder.tvTime.getLayoutParams()).horizontalBias = 0.95f;
            }


        holder.itemView.setOnClickListener( v -> {
            if(parentActivity.isEditing()){
                if (!holder.selected){
                    holder.selected = true;
                    Drawable d = v.getResources().getDrawable(R.drawable.msg_hold_overlay, v.getContext().getTheme());
                    v.setForeground(d);
                    selectedHolders.add(holder);
                    selectedCount++;
                    parentActivity.addSelectedMessage(msgs.get(holder.getBindingAdapterPosition()));
                    System.out.println("---------SELECTING MESSAGE: " + msgs.get(holder.getBindingAdapterPosition()).msgText + "---------\n");
                }else{
                    holder.selected = false;
                    v.setForeground(null);
                    selectedHolders.remove(holder);
                    parentActivity.removeSelectedMessage(msgs.get(holder.getBindingAdapterPosition()));
                    selectedCount--;
                }
            }
        });

            holder.itemView.setOnLongClickListener(v -> {
                if (!parentActivity.isEditing()){
                    System.out.println("VVVVVVVVVVVVVVVVVVVVVVVVVVVVVv " + v.getId() + " " + v);
                    holder.selected = true;
                    Drawable d = v.getResources().getDrawable(R.drawable.msg_hold_overlay, v.getContext().getTheme());
                    v.setForeground(d);
                    selectedCount++;
                    selectedHolders.add(holder);
                    parentActivity.addSelectedMessage(msgs.get(holder.getBindingAdapterPosition()));
                    System.out.println("---------SELECTING MESSAGE: " + msgs.get(holder.getBindingAdapterPosition()).msgText + "---------\n");
                    if(selectedCount == 1) parentActivity.toggleEditMode();
                }
                return true;
            });
    }

    public void resetOverlays(){
        for (ViewHolder vh : selectedHolders){
            vh.itemView.setForeground(null);
        }
        selectedHolders.clear();
        selectedCount = 0;
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
        TextView tvUnread;
        TextView tvTime;
        ConstraintLayout body;

        boolean selected;

        public ViewHolder(View itemView) {
            super(itemView);
            tvMsg = itemView.findViewById(R.id.tvMsg);
            body = (ConstraintLayout) itemView;
            tvTime = itemView.findViewById(R.id.tvTime);
            tvUnread = itemView.findViewById(R.id.tvUnread);

            selected = false;
        }
    }
}
