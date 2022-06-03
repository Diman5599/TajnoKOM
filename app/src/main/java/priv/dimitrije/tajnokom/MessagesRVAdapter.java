package priv.dimitrije.tajnokom;

import android.annotation.SuppressLint;
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

    public MessagesRVAdapter(List<REMessage> items){
        instance = this;
        msgs = items;
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

        public ViewHolder(View itemView) {
            super(itemView);
            tvMsg = itemView.findViewById(R.id.tvMsg);
        }
    }
}
