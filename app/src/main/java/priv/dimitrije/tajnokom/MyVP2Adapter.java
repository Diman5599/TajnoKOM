package priv.dimitrije.tajnokom;


import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.viewpager2.adapter.FragmentStateAdapter;

class MyVP2Adapter extends FragmentStateAdapter {
    MainFragment root;

    public MyVP2Adapter(MainFragment fragment){
        super(fragment);
        root = fragment;
    }

    @NonNull
    @Override
    public Fragment createFragment(int position) {
        Fragment fragment = null;
        switch (position) {
            case 0:
                fragment = new ChatFragment();
                break;
            case 1:
                fragment = new ContactsFragment();
                break;
            default:
                fragment = new Fragment();
                break;
        }
        fragment.setArguments(new Bundle());
        return fragment;
    }

    @Override
    public int getItemCount() {
        return 2;
    }
}